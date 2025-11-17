package com.solar.ev

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
// import androidx.appcompat.app.AppCompatActivity // Now extends BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.solar.ev.databinding.ActivityMyApplicationsBinding
import com.solar.ev.model.User // Assuming ApplicationListItem.user is of this type
import com.solar.ev.model.application.ApplicationListItem
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.adapter.ApplicationListAdapter
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.application.MyApplicationsViewModel
import com.solar.ev.viewModel.application.MyApplicationsViewModelFactory
import com.solar.ev.viewModel.application.MyApplicationsViewModel.MyApplicationsResult
import com.solar.ev.viewModel.application.MyApplicationsViewModel.DeleteApplicationResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyApplicationsActivity : BaseActivity() {
    private lateinit var binding: ActivityMyApplicationsBinding
    private lateinit var applicationListAdapter: ApplicationListAdapter
    private lateinit var sessionManager: SessionManager // Already in BaseActivity, but can be shadowed if needed locally
    private var loadingDialog: LoadingDialogFragment? = null
    private var userRole: String? = null

    private var originalApplicationList: List<ApplicationListItem> = emptyList()
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private val displayDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val modelDateParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Agent filter variables
    private var agentDisplayList: MutableList<String> = mutableListOf()
    private var agentIdList: MutableList<String?> = mutableListOf() // String? to allow null for "All Agents"
    private var selectedAgentId: String? = null
    private val ALL_AGENTS_ID: String? = null // Represents "All Agents"
    private val ALL_AGENTS_DISPLAY_NAME = "All Agents"

    private val viewModel: MyApplicationsViewModel by viewModels {
        MyApplicationsViewModelFactory(RetrofitInstance.api)
    }

    private val adminLikeRolesForTitleAndAgentFilter = listOf("admin", "supervisor", "back-office")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this) // Initialize local sessionManager if needed by other methods not in BaseActivity
        userRole = sessionManager.getUserRole()

        setupToolbar()
        setupRecyclerView()
        setupFilterControls()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updateTitleBasedOnRole()
        fetchApplications() 
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMyApplications)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        updateTitleBasedOnRole()
    }

    private fun updateTitleBasedOnRole() {
        if (adminLikeRolesForTitleAndAgentFilter.any { it.equals(userRole, ignoreCase = true) }) {
            supportActionBar?.title = "All Applications"
        } else {
            supportActionBar?.title = "My Applications"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_my_applications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh_show_all -> {
                clearFilters()
                fetchApplications()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        applicationListAdapter = ApplicationListAdapter(
            onEditClickListener = { applicationId ->
                Log.d("MyApplicationsActivity", "Edit clicked for ID: $applicationId")
                val intent = Intent(this, ApplicationActivity::class.java)
                intent.putExtra("EXTRA_APPLICATION_ID", applicationId)
                intent.putExtra("EXTRA_IS_EDIT_MODE", true)
                startActivity(intent)
            },
            onDeleteClickListener = { applicationId ->
                Log.d("MyApplicationsActivity", "Delete clicked for ID: $applicationId")
                showDeleteConfirmationDialog(applicationId)
            },
            onProcessSuryaGharClickListener = { applicationId, applicantName ->
                Log.d("MyApplicationsActivity", "Process for Surya Ghar clicked for ID: $applicationId, Applicant: $applicantName")
                val intent = Intent(this, SuryaGharProcessingActivity::class.java)
                intent.putExtra(SuryaGharProcessingActivity.EXTRA_APPLICATION_ID, applicationId)
                applicantName?.let {
                    intent.putExtra(SuryaGharProcessingActivity.EXTRA_APPLICANT_NAME, it)
                }
                startActivity(intent)
            },
            onUploadQuotationClickListener = { applicationId ->
                val intent = Intent(this, QuotationActivity::class.java)
                intent.putExtra(QuotationActivity.EXTRA_APPLICATION_ID, applicationId)
                intent.putExtra("open_quotation", true)
                startActivity(intent)
            },
            currentUserRole = userRole
        )

        binding.recyclerViewMyApplications.apply {
            layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
            adapter = applicationListAdapter
        }
    }

    private fun setupFilterControls() {
        binding.etStartDateFilter.setOnClickListener {
            showDatePickerDialog(isStartDate = true)
        }

        binding.etEndDateFilter.setOnClickListener {
            showDatePickerDialog(isStartDate = false)
        }

        binding.btnApplyFilters.setOnClickListener {
            applyFilters()
        }

        binding.btnClearFilters.setOnClickListener {
            clearFilters()
        }

        binding.etSearchFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // applyFilters() // Option: Filter as user types. Uncomment if desired.
            }
        })

        // Agent Spinner Setup
        binding.spinnerAgentFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAgentId = if (position < agentIdList.size) agentIdList[position] else ALL_AGENTS_ID
                 // applyFilters() // Option: Filter immediately on selection. Uncomment if desired.
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedAgentId = ALL_AGENTS_ID
            }
        }
    }

    private fun populateAgentSpinner() {
        agentDisplayList.clear()
        agentIdList.clear()

        // Add "All Agents" as the default first option
        agentDisplayList.add(ALL_AGENTS_DISPLAY_NAME)
        agentIdList.add(ALL_AGENTS_ID)

        val uniqueAgents = originalApplicationList
            .mapNotNull { it.user } // Get non-null users
            .distinctBy { it.id }   // Get unique users by ID
            .sortedBy { it.name }   // Sort by name for display

        uniqueAgents.forEach { user ->
            user.name?.let { name ->
                agentDisplayList.add(name)
                agentIdList.add(user.id)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, agentDisplayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAgentFilter.adapter = adapter

        // Set visibility based on role and number of agents
        if (adminLikeRolesForTitleAndAgentFilter.any { it.equals(userRole, ignoreCase = true) } && uniqueAgents.size > 1) {
            binding.tvAgentFilterLabel.visibility = View.VISIBLE
            binding.spinnerAgentFilter.visibility = View.VISIBLE
        } else {
            binding.tvAgentFilterLabel.visibility = View.GONE
            binding.spinnerAgentFilter.visibility = View.GONE
            selectedAgentId = ALL_AGENTS_ID // Ensure no agent filter is applied if hidden
        }
        binding.spinnerAgentFilter.setSelection(0, false) // Select "All Agents" by default
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val dateStringToParse = if (isStartDate) selectedStartDate else selectedEndDate
        if (dateStringToParse != null) {
            try {
                calendar.time = displayDateFormatter.parse(dateStringToParse)!!
            } catch (e: Exception) { /* Use current date if parsing fails */ }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val formattedDate = displayDateFormatter.format(selectedCalendar.time)
            if (isStartDate) {
                selectedStartDate = formattedDate
                binding.etStartDateFilter.setText(formattedDate)
            } else {
                selectedEndDate = formattedDate
                binding.etEndDateFilter.setText(formattedDate)
            }
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun applyFilters() {
        val searchTerm = binding.etSearchFilter.text.toString().trim().lowercase(Locale.ROOT)
        var filteredList = originalApplicationList

        // Agent Filter
        if (selectedAgentId != ALL_AGENTS_ID) {
            filteredList = filteredList.filter { applicationListItem ->
                applicationListItem.user?.id == selectedAgentId
            }
        }

        // Search Term Filter
        if (searchTerm.isNotEmpty()) {
            filteredList = filteredList.filter { applicationListItem ->
                applicationListItem.name?.lowercase(Locale.ROOT)?.contains(searchTerm) == true
            }
        }

        // Date Filter
        try {
            val startDateSelected = selectedStartDate?.let { displayDateFormatter.parse(it)?.time }
            val endDateSelected = selectedEndDate?.let {
                val cal = Calendar.getInstance()
                cal.time = displayDateFormatter.parse(it)!!
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                cal.timeInMillis
            }

            if (startDateSelected != null) {
                filteredList = filteredList.filter { applicationListItem ->
                    applicationListItem.createdAt?.let { dateStr ->
                        try {
                            modelDateParser.parse(dateStr)?.time?.let { it >= startDateSelected } ?: false
                        } catch (e: Exception) {
                            Log.w("MyApplicationsActivity", "Could not parse createdAt from model: $dateStr", e)
                            false
                        }
                    } ?: false
                }
            }
            if (endDateSelected != null) {
                filteredList = filteredList.filter { applicationListItem ->
                    applicationListItem.createdAt?.let { dateStr ->
                         try {
                            modelDateParser.parse(dateStr)?.time?.let { it <= endDateSelected } ?: false
                        } catch (e: Exception) {
                            Log.w("MyApplicationsActivity", "Could not parse createdAt from model: $dateStr", e)
                            false
                        }
                    } ?: false
                }
            }
        } catch (e: Exception) {
            Log.e("MyApplicationsActivity", "Error parsing dates for filtering", e)
            Toast.makeText(this, "Error in date filter. Check date formats.", Toast.LENGTH_SHORT).show()
        }

        applicationListAdapter.submitList(filteredList)
        updateEmptyStateView(filteredList.isEmpty(), isFiltering = true)
    }

    private fun clearFilters() {
        binding.etSearchFilter.setText("")
        binding.etStartDateFilter.setText("")
        binding.etEndDateFilter.setText("")
        selectedStartDate = null
        selectedEndDate = null
        selectedAgentId = ALL_AGENTS_ID
        if (binding.spinnerAgentFilter.adapter != null && binding.spinnerAgentFilter.adapter.count > 0) {
            binding.spinnerAgentFilter.setSelection(0) // Reset to "All Agents"
        }
        applicationListAdapter.submitList(originalApplicationList)
        updateEmptyStateView(originalApplicationList.isEmpty(), isFiltering = false)
    }

    private fun updateEmptyStateView(isEmpty: Boolean, isFiltering: Boolean) {
        if (isEmpty) {
            binding.textViewMyApplicationsStatus.text = if (isFiltering) "No applications match your criteria." else "No applications found."
            binding.textViewMyApplicationsStatus.visibility = View.VISIBLE
            binding.recyclerViewMyApplications.visibility = View.GONE
        } else {
            binding.textViewMyApplicationsStatus.visibility = View.GONE
            binding.recyclerViewMyApplications.visibility = View.VISIBLE
        }
    }

    private fun showDeleteConfirmationDialog(applicationId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this application? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val token = sessionManager.getUserToken()
                if (token != null && userRole.equals("admin", ignoreCase = true)) {
                    viewModel.deleteApplicationById("Bearer $token", applicationId)
                } else if (token == null) {
                    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You do not have permission to delete.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLoadingDialog(dialogMessage: String = "Loading...") {
        hideLoadingDialog()
        if (!isFinishing && !isDestroyed) {
            loadingDialog = LoadingDialogFragment.newInstance(dialogMessage)
            loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG)
        }
    }

    private fun hideLoadingDialog() {
        if (!isFinishing && !isDestroyed) {
            loadingDialog?.dismissAllowingStateLoss()
        }
        loadingDialog = null
    }

    private fun fetchApplications() {
        val token = sessionManager.getUserToken()
        val role = userRole // userRole is already a class member

        if (token != null && role != null) {
            if (viewModel.deleteResult.value !is DeleteApplicationResult.Loading) {
                 showLoadingDialog("Fetching applications...")
            }
            binding.textViewMyApplicationsStatus.visibility = View.GONE
            binding.recyclerViewMyApplications.visibility = View.GONE
            viewModel.fetchApplications("Bearer $token", role)
        } else {
            hideLoadingDialog()
            Toast.makeText(this, "User not logged in or role not defined.", Toast.LENGTH_SHORT).show()
            // Assuming performLogout() handles navigation and session clearing
            performLogout() 
            originalApplicationList = emptyList()
            applicationListAdapter.submitList(emptyList())
            updateEmptyStateView(true, isFiltering = false)
            binding.textViewMyApplicationsStatus.text = "Authentication required or role undefined. Please log in."
        }
    }

    private fun observeViewModel() {
        viewModel.applicationsResult.observe(this) { result ->
            hideLoadingDialog() // Hide loading dialog once a result is received
            when (result) {
                is MyApplicationsResult.Loading -> {
                    // Already handled by showLoadingDialog in fetchApplications if not deleting
                    if (viewModel.deleteResult.value !is DeleteApplicationResult.Loading) {
                        showLoadingDialog("Fetching applications...")
                    }
                    binding.textViewMyApplicationsStatus.visibility = View.GONE
                    binding.recyclerViewMyApplications.visibility = View.GONE
                }
                is MyApplicationsResult.Success -> {
                    originalApplicationList = result.applications
                    populateAgentSpinner() // Populate/update agent spinner
                    
                    // Apply filters if any were set, otherwise show full list
                    if (binding.etSearchFilter.text.toString().isNotEmpty() || 
                        selectedStartDate != null || selectedEndDate != null || 
                        selectedAgentId != ALL_AGENTS_ID) { // Check if an agent is selected
                        applyFilters()
                    } else {
                        applicationListAdapter.submitList(originalApplicationList)
                        updateEmptyStateView(originalApplicationList.isEmpty(), isFiltering = false)
                    }
                }
                is MyApplicationsResult.Error -> {
                    originalApplicationList = emptyList()
                    populateAgentSpinner() // Still populate spinner (will show "All Agents")
                    applicationListAdapter.submitList(emptyList())
                    updateEmptyStateView(true, isFiltering = false)
                    binding.textViewMyApplicationsStatus.text = result.errorMessage
                    Log.e("MyApplicationsActivity", "Error fetching applications: ${result.errorMessage}")
                }
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            // No need to call hideLoadingDialog() here if showLoadingDialog("Deleting...") handles it.
            // But if it doesn't, ensure it's hidden on Success/Error.
            when (result) {
                is DeleteApplicationResult.Loading -> {
                    showLoadingDialog("Deleting application...")
                }
                is DeleteApplicationResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    fetchApplications() // Re-fetch to update the list
                }
                is DeleteApplicationResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Delete failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                    Log.e("MyApplicationsActivity", "Error deleting application ${result.applicationId}: ${result.errorMessage}")
                }
            }
        }
    }
}
