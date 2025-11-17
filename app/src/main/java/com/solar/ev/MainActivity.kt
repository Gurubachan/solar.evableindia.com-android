package com.solar.ev

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.solar.ev.databinding.ActivityMainBinding
import com.solar.ev.model.DashboardStatsData
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.report.AgentReportActivity // Added import
import com.solar.ev.viewModel.MainViewModel
import com.solar.ev.viewModel.MainViewModelFactory

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(RetrofitInstance.api)
    }
    private val TAG = "MainActivity"

    companion object {
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        setupButtonListeners()
        observeViewModel()
        fetchDashboardStats() // Initial fetch

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userRole = sessionManager.getUserRole()
        val isAdminSupervisorOrBackOffice = userRole.equals("admin", ignoreCase = true) ||
                userRole.equals("supervisor", ignoreCase = true) ||
                userRole.equals("back-office", ignoreCase = true)

        if (isAdminSupervisorOrBackOffice) {
            binding.btnUserManagement.visibility = View.VISIBLE
            binding.btnAgentReport.visibility = View.VISIBLE // Set visibility
        } else {
            binding.btnUserManagement.visibility = View.GONE
            binding.btnAgentReport.visibility = View.GONE // Set visibility
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.title = "Solar EV Dashboard"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, _ ->
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupButtonListeners() {
        binding.btnMyApplications.setOnClickListener {
            startActivity(Intent(this, MyApplicationsActivity::class.java))
        }

        binding.btnCreateNewApplication.setOnClickListener {
            val intent = Intent(this, ApplicationActivity::class.java)
            intent.putExtra("EXTRA_IS_EDIT_MODE", false)
            startActivity(intent)
        }

        binding.btnUserManagement.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        binding.btnAgentReport.setOnClickListener { // Added listener
            startActivity(Intent(this, AgentReportActivity::class.java))
        }
        
        binding.btnRefreshStatsInline.setOnClickListener {
            fetchDashboardStats()
        }
    }

    private fun fetchDashboardStats() {
        val token = sessionManager.getUserToken()
        val userRole = sessionManager.getUserRole() 
        if (token != null) {
            mainViewModel.loadApplicationSummaryCounts("Bearer $token", userRole)
        } else {
            Toast.makeText(this, "User not logged in. Please log in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun observeViewModel() {
        mainViewModel.summaryResult.observe(this) { result ->
            when (result) {
                is MainViewModel.SummaryResult.Loading -> {
                    binding.pbStatsLoading.visibility = View.VISIBLE
                    binding.gridStatsData.visibility = View.GONE
                    binding.tvStatsError.visibility = View.GONE
                }
                is MainViewModel.SummaryResult.Success -> {
                    binding.pbStatsLoading.visibility = View.GONE
                    binding.gridStatsData.visibility = View.VISIBLE
                    binding.tvStatsError.visibility = View.GONE
                    updateStatsUI(result.stats)
                }
                is MainViewModel.SummaryResult.Error -> {
                    binding.pbStatsLoading.visibility = View.GONE
                    binding.gridStatsData.visibility = View.GONE
                    binding.tvStatsError.visibility = View.VISIBLE
                    binding.tvStatsError.text = result.errorMessage
                    Log.e(TAG, "Error loading dashboard stats: ${result.errorMessage}")
                }
            }
        }
    }

    private fun updateStatsUI(stats: DashboardStatsData) {
        binding.tvTotalApplications.text = stats.totalApplications.toString()
        binding.tvKycVerified.text = "${stats.kyc.verified} / ${stats.kyc.total}"
        binding.tvBankVerified.text = "${stats.bank.verified} / ${stats.bank.total}"
        binding.tvDiscomVerified.text = "${stats.discom.verified} / ${stats.discom.total}"
        binding.tvInstallationVerified.text = "${stats.installation.verified} / ${stats.installation.total}"
        binding.tvFullyCompleted.text = stats.fullyCompletedApplications.toString()
    }

    private fun loadUserProfile(){
        binding.textViewWelcome.text = "Welcome ${sessionManager.getUserName() ?: "User"}!"
        val profilePhotoPath = sessionManager.getProfilePhotoUrl() // This should now be a relative path
        Log.d(TAG, "User Profile Photo Path from Session: '$profilePhotoPath'")

        if (!profilePhotoPath.isNullOrBlank()) {
            val fullPhotoUrl = BASE_STORAGE_URL + profilePhotoPath
            Log.d(TAG, "Loading Profile Photo from Full URL: '$fullPhotoUrl'")
            Glide.with(this)
                .load(fullPhotoUrl)
                .placeholder(R.drawable.ic_baseline_person_24) 
                .error(R.drawable.ic_baseline_person_24) 
                .circleCrop() 
                .into(binding.ivUserProfileHome)
        } else {
            Log.d(TAG, "Profile Photo Path is null or blank, loading default icon.")
            Glide.with(this)
                .load(R.drawable.ic_baseline_person_24)
                .circleCrop()
                .into(binding.ivUserProfileHome)
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile() // Load/refresh user profile info (name and image)
    }
}
