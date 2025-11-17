package com.solar.ev

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.widget.RadioButton
import android.widget.ScrollView // Added for explicit findViewByID if needed
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.solar.ev.databinding.ActivityApplicationBinding
import com.solar.ev.model.application.ApplicationData
import com.solar.ev.model.application.ApplicationRequest
// import com.solar.ev.model.application.ApplicationListItem // Not directly used in populateFormFields signature
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment // Added import
import com.solar.ev.viewModel.application.ApplicationViewModel
import com.solar.ev.viewModel.application.ApplicationViewModelFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ApplicationActivity : BaseActivity() {

    private lateinit var binding: ActivityApplicationBinding
    private lateinit var sessionManager: SessionManager
    private var loadingDialog: LoadingDialogFragment? = null // Added for loading dialog

    private val applicationViewModel: ApplicationViewModel by viewModels {
        ApplicationViewModelFactory(RetrofitInstance.api)
    }

    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var currentApplicationId: String? = null
    val BASE_IMAGE_URL = "https://solar.evableindia.com/core/public/storage/"

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    binding.ivApplicantPhoto.setImageURI(uri)
                    Log.d("ApplicationActivity", "New image selected: $uri")
                }
            }
        }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_IS_EDIT_MODE = "EXTRA_IS_EDIT_MODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        handleIntentExtras()
        setupListeners()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val scrollView = binding.root.findViewById<ScrollView?>(R.id.scrollView)
            scrollView?.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarApplication)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun handleIntentExtras() {
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        currentApplicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)

        if (isEditMode && currentApplicationId != null) {
            setupEditModeUI()
            fetchApplicationDetailsForEditing()
        } else {
            setupCreateModeUI()
        }
    }

    private fun setupCreateModeUI() {
        supportActionBar?.title = "New Application"
        binding.buttonSubmitApplication.text = "Submit Application"
        binding.ivApplicantPhoto.setImageResource(R.drawable.ic_launcher_background)
    }

    private fun setupEditModeUI() {
        supportActionBar?.title = "Edit Application"
        binding.buttonSubmitApplication.text = "Update Application"
    }

    private fun fetchApplicationDetailsForEditing() {
        val token = sessionManager.getUserToken()
        if (token != null && currentApplicationId != null) {
            applicationViewModel.fetchApplicationDetails("Bearer $token", currentApplicationId!!)
        } else {
            Toast.makeText(this, "Error: Missing token or Application ID.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupListeners() {
        binding.buttonDob.setOnClickListener { showDatePickerDialog() }
        binding.buttonSelectPhoto.setOnClickListener { openImagePicker() }
        binding.buttonSubmitApplication.setOnClickListener { handleSubmit() }
    }

    // Helper methods for loading dialog
    private fun showLoadingDialog() {
        if (loadingDialog == null || loadingDialog?.dialog?.isShowing == false) {
            val defaultMessage = "Loading..." // Define a default message
            loadingDialog = LoadingDialogFragment.newInstance(defaultMessage)
            // Check if the activity is in a state to show dialogs
            if (!isFinishing && !isDestroyed) {
                 loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG)
            }
        }
    }

    private fun hideLoadingDialog() {
        // Check if the activity is in a state to dismiss dialogs
        if (!isFinishing && !isDestroyed) {
            loadingDialog?.dismissAllowingStateLoss()
        }
        loadingDialog = null
    }

    private fun observeViewModel() {
        applicationViewModel.applicationDetailResult.observe(this) { result ->
            when (result) {
                is ApplicationViewModel.ApplicationDetailResult.Loading -> {
                    showLoadingDialog()
                }
                is ApplicationViewModel.ApplicationDetailResult.Success -> {
                    hideLoadingDialog()
                    result.application?.let { populateFormFields(it) }
                        ?: Toast.makeText(this, "Failed to load details.", Toast.LENGTH_SHORT).show()
                }
                is ApplicationViewModel.ApplicationDetailResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Error fetching details: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        applicationViewModel.submitResult.observe(this) { result ->
            when (result) {
                is ApplicationViewModel.SubmitResult.Loading -> {
                    showLoadingDialog()
                }
                is ApplicationViewModel.SubmitResult.Success -> {
                    hideLoadingDialog()
                    val message = if (isEditMode) "Application updated successfully" else "Application submitted successfully"
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is ApplicationViewModel.SubmitResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateFormFields(appData: ApplicationData) { // Changed parameter type
        val app = appData.application // Access the nested ApplicationListItem
        binding.etName.setText(app?.name ?: "")
        binding.tvSelectedDob.text = app?.dob ?: ""
        binding.etEmail.setText(app?.email ?: "")
        binding.etContactNumber.setText(app?.contactNumber ?: "")
        binding.etAddress.setText(app?.address ?: "")
        binding.etState.setText(app?.state ?: "")
        binding.etDistrict.setText(app?.district ?: "")
        binding.etPincode.setText(app?.pincode ?: "")

        when (app?.gender?.lowercase(Locale.getDefault())) {
            "male" -> binding.rgGender.check(binding.rbMale.id)
            "female" -> binding.rgGender.check(binding.rbFemale.id)
            "other" -> binding.rgGender.check(binding.rbOther.id)
        }

        selectedImageUri = null
        app?.photo?.let { photoUrl ->
            if (photoUrl.isNotEmpty()) {
                Log.d("#image", photoUrl)
                Glide.with(this)
                    .load(BASE_IMAGE_URL + photoUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivApplicantPhoto)
            } else {
                binding.ivApplicantPhoto.setImageResource(R.drawable.ic_launcher_background)
                if (photoUrl.isNotEmpty()) {
                     Log.w("ApplicationActivity", "Photo data from server is not a valid URL or is empty: $photoUrl")
                }
            }
        } ?: binding.ivApplicantPhoto.setImageResource(R.drawable.ic_launcher_background)
    }

    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: IOException) {
            Log.e("ApplicationActivity", "Error converting URI to Base64", e)
            Toast.makeText(this, "Failed to read image data.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            binding.tvSelectedDob.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun validateForm(): Boolean {
        if (binding.etName.text.isNullOrEmpty()) {
            binding.etName.error = "Name is required"; return false
        }
        if (binding.tvSelectedDob.text.isNullOrEmpty() || binding.tvSelectedDob.text == "Select Date of Birth") {
            Toast.makeText(this, "Date of Birth is required", Toast.LENGTH_SHORT).show(); return false
        }
        if (binding.etEmail.text.isNullOrEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString()).matches()) {
            binding.etEmail.error = "Valid Email is required"; return false
        }
        if (binding.etContactNumber.text.isNullOrEmpty() || binding.etContactNumber.text.toString().length < 10) {
            binding.etContactNumber.error = "Valid 10-digit Contact Number is required"; return false
        }
        if (binding.rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Gender is required", Toast.LENGTH_SHORT).show(); return false
        }
        if (!isEditMode && selectedImageUri == null) {
            Toast.makeText(this, "Applicant photo is required.", Toast.LENGTH_SHORT).show(); return false
        }
        return true
    }

    private fun handleSubmit() {
        if (!validateForm()) {
            Toast.makeText(this, "Please correct the errors.", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getUserToken()
        if (token == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        var photoBase64String: String? = null
        selectedImageUri?.let {
            photoBase64String = getBase64FromUri(it)
            if (photoBase64String == null) {
                hideLoadingDialog() // Hide dialog if Base64 conversion fails
                return
            }
        }

        val applicationRequest = ApplicationRequest(
            name = binding.etName.text.toString(),
            gender = findViewById<RadioButton>(binding.rgGender.checkedRadioButtonId).text.toString().lowercase(Locale.getDefault()),
            dob = binding.tvSelectedDob.text.toString(),
            email = binding.etEmail.text.toString(),
            contactNumber = binding.etContactNumber.text.toString(),
            address = binding.etAddress.text.toString(),
            state = binding.etState.text.toString(),
            district = binding.etDistrict.text.toString(),
            pincode = binding.etPincode.text.toString(),
            photo = photoBase64String?.let { "data:image/png;base64,$it" } // Handle null case for photo string
        )

        // showLoadingDialog() will be called by the ViewModel observer when SubmitResult.Loading is posted
        if (isEditMode && currentApplicationId != null) {
            applicationViewModel.updateExistingApplication(currentApplicationId!!, "Bearer $token", applicationRequest)
        } else {
            if (photoBase64String == null && !isEditMode) {
                Toast.makeText(this, "Applicant photo is required and could not be processed.", Toast.LENGTH_SHORT).show()
                hideLoadingDialog() // Hide dialog if photo is required and missing/failed
                return
            }
            applicationViewModel.submitApplication("Bearer $token", applicationRequest)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish(); return true
        }
        return super.onOptionsItemSelected(item)
    }
}
