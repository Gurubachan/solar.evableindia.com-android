package com.solar.ev

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.solar.ev.databinding.ActivityInstallationDetailsBinding
import com.solar.ev.model.common.DocumentVerificationRequest
import com.solar.ev.model.installation.InstallationDetailsRequest
import com.solar.ev.model.installation.InstallationInfo
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.installation.DeleteInstallationDetailResult
import com.solar.ev.viewModel.installation.VerifyInstallationDetailResult
import com.solar.ev.viewModel.installation.InstallationDetailsResult
import com.solar.ev.viewModel.installation.InstallationDetailsViewModel
import com.solar.ev.viewModel.installation.InstallationDetailsViewModelFactory

import java.io.IOException
import java.util.Locale

class InstallationDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityInstallationDetailsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var applicationId: String? = null
    private var currentInstallationId: String? = null
    private var isEditMode = false
    private var userRole: String? = null

    private var installationPhotoUri: Uri? = null
    private var existingInstallationPhotoRelativePath: String? = null

    private var loadingDialog: LoadingDialogFragment? = null

    private val viewModel: InstallationDetailsViewModel by viewModels {
        InstallationDetailsViewModelFactory(RetrofitInstance.api)
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_INSTALLATION_ID = "EXTRA_INSTALLATION_ID"
        private const val TAG = "InstallationDetailsAct"
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
        private val SYSTEM_TYPES = arrayOf("on-grid", "off-grid", "hybrid")
        private val VERIFICATION_STATUSES = arrayOf("pending", "verified", "rejected", "objection") 
        private const val STATUS_VERIFIED = "verified"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_OBJECTION = "objection"
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if (isGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Cannot fetch coordinates.", Toast.LENGTH_LONG).show()
        }
    }

    private val installationPhotoPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let {
                uri ->
                installationPhotoUri = uri
                existingInstallationPhotoRelativePath = null
                binding.tvSelectedPhotoFileName.text = getFileName(uri)
                Glide.with(this).load(uri).placeholder(R.drawable.image_upload_placeholder).into(binding.ivInstallationPhotoPreview)
                binding.ivInstallationPhotoPreview.visibility = View.VISIBLE
                binding.tvSelectedPhotoFileName.visibility = View.VISIBLE
                binding.ivInstallationPhotoPreview.isClickable = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getUserRole()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        currentInstallationId = intent.getStringExtra(EXTRA_INSTALLATION_ID)

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing. Cannot proceed.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupSystemTypeDropdown()
        setupListeners()
        setupVerificationSection() 
        observeViewModel()

        if (currentInstallationId != null) {
            val token = sessionManager.getUserToken()
            if (!token.isNullOrEmpty()) {
                viewModel.fetchInstallationDetail(token, currentInstallationId!!)
            } else {
                Toast.makeText(this, "Session expired. Cannot fetch details.", Toast.LENGTH_LONG).show()
                updateUiForCreateMode()
            }
        } else {
            updateUiForCreateMode()
        }
    }

    private fun setInstallationFormFieldsEnabled(enabled: Boolean) {
        binding.etCapacityKw.isEnabled = enabled
        binding.actSystemType.isEnabled = enabled 
        binding.tilSystemType.isEnabled = enabled 
        binding.etLatitude.isEnabled = enabled
        binding.etLongitude.isEnabled = enabled
        binding.buttonGetCurrentLocation.isEnabled = enabled
        binding.buttonUploadInstallationPhoto.isEnabled = enabled

        binding.buttonGetCurrentLocation.alpha = if (enabled) 1.0f else 0.5f
        binding.buttonUploadInstallationPhoto.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarInstallationDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupSystemTypeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SYSTEM_TYPES)
        binding.actSystemType.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.buttonUploadInstallationPhoto.setOnClickListener { openImagePicker() }
        binding.buttonSubmitInstallationDetails.setOnClickListener { handleSubmit() }
        binding.ivInstallationPhotoPreview.setOnClickListener { viewInstallationPhoto() }
        binding.buttonGetCurrentLocation.setOnClickListener { handleGetLocationClick() }
        binding.btnDeleteInstallationDetails.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun setupVerificationSection() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, VERIFICATION_STATUSES)
        binding.actVerificationStatusInstallation.setAdapter(statusAdapter)
        binding.buttonSubmitVerificationInstallation.setOnClickListener { handleSubmitInstallationVerification() }
    }

    private fun handleSubmitInstallationVerification() {
        val token = sessionManager.getUserToken()
        val role = sessionManager.getUserRole()

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }
        if (!(role.equals("supervisor", ignoreCase = true) || role.equals("back-office", ignoreCase = true))) {
            Toast.makeText(this, "You do not have permission to verify documents.", Toast.LENGTH_LONG).show()
            return
        }
        if (currentInstallationId == null) {
            Toast.makeText(this, "Installation ID is missing. Cannot verify.", Toast.LENGTH_SHORT).show()
            return
        }

        val verificationStatus = binding.actVerificationStatusInstallation.text.toString()
        val verificationRemark = binding.etVerificationRemarkInstallation.text.toString().trim()

        if (verificationStatus.isEmpty() || !VERIFICATION_STATUSES.contains(verificationStatus.lowercase(Locale.ROOT))) {
            Toast.makeText(this, "Please select a valid verification status.", Toast.LENGTH_SHORT).show()
            binding.tilVerificationStatusInstallation.error = "Required"
            return
        }
        binding.tilVerificationStatusInstallation.error = null

        val request = DocumentVerificationRequest(
            verificationStatus = verificationStatus,
            verificationRemark = verificationRemark.takeIf { it.isNotEmpty() }
        )
        viewModel.verifyInstallationDocument(token, currentInstallationId!!, request)
    }

    private fun showDeleteConfirmationDialog() {
        if (currentInstallationId == null) {
            Toast.makeText(this, "Cannot delete. Installation details ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete these installation details? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val token = sessionManager.getUserToken()
                if (token != null && userRole.equals("admin", ignoreCase = true) && currentInstallationId != null) {
                    viewModel.deleteInstallationDetailById(token, currentInstallationId!!)
                } else if (token == null) {
                    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                } else if (!userRole.equals("admin", ignoreCase = true)){
                    Toast.makeText(this, "You do not have permission to delete installation details.", Toast.LENGTH_SHORT).show()
                } else {
                     Toast.makeText(this, "Cannot delete. Installation details ID is missing.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleGetLocationClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission") 
    private fun fetchCurrentLocation() {
        if (!isLocationEnabled()) {
            showLocationDisabledDialog()
            return
        }

        showLoadingDialog("Fetching location...")
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                hideLoadingDialog()
                if (location != null) {
                    binding.etLatitude.setText(String.format(Locale.US, "%.4f", location.latitude))
                    binding.etLongitude.setText(String.format(Locale.US, "%.4f", location.longitude))
                    Toast.makeText(this, "Location fetched!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to get current location. Please try again or enter manually.", Toast.LENGTH_LONG).show()
                    fetchLastKnownLocation()
                }
            }
            .addOnFailureListener { e ->
                hideLoadingDialog()
                Log.e(TAG, "Failed to get current location: ", e)
                Toast.makeText(this, "Error fetching location: ${e.message}. Try last known or enter manually.", Toast.LENGTH_LONG).show()
                fetchLastKnownLocation()
            }
    }
    @SuppressLint("MissingPermission") 
    private fun fetchLastKnownLocation(){
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if(location != null){
                binding.etLatitude.setText(String.format(Locale.US, "%.4f", location.latitude))
                binding.etLongitude.setText(String.format(Locale.US, "%.4f", location.longitude))
                Toast.makeText(this, "Last known location fetched!", Toast.LENGTH_SHORT).show()
            } else {
                 Toast.makeText(this, "No last known location available. Please enable GPS and try again or enter manually.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
             Toast.makeText(this, "Failed to get last known location. Please enter manually.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Services Disabled")
            .setMessage("Please enable location services (GPS) to fetch your current location.")
            .setPositiveButton("Enable Location") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun observeViewModel() {
        viewModel.createResult.observe(this) { result ->
            when (result) {
                is InstallationDetailsResult.Loading -> showLoadingDialog("Submitting Details...")
                is InstallationDetailsResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "Installation details saved!", Toast.LENGTH_LONG).show()
                    finish()
                }
                is InstallationDetailsResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        viewModel.updateResult.observe(this) { result ->
            when (result) {
                is InstallationDetailsResult.Loading -> showLoadingDialog("Updating Details...")
                is InstallationDetailsResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "Installation details updated!", Toast.LENGTH_LONG).show()
                    finish()
                }
                is InstallationDetailsResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        viewModel.isFetching.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingDialog("Fetching Details...")
            } else {
                if (!isAnyOtherLoadingOperationActive()) {
                     hideLoadingDialog()
                }
            }
        }
        
        viewModel.verifyInstallationResult.observe(this) { result ->
            when (result) {
                is VerifyInstallationDetailResult.Loading -> {
                    showLoadingDialog("Verifying installation...")
                }
                is VerifyInstallationDetailResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    val token = sessionManager.getUserToken()
                    if (currentInstallationId != null && !token.isNullOrEmpty()) {
                        viewModel.fetchInstallationDetail(token, currentInstallationId!!)
                    }
                }
                is VerifyInstallationDetailResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Verification failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.fetchedDetail.observe(this) { detail ->
            if (detail != null) {
                populateForm(detail)
            } else {
                val fetchAttempted = currentInstallationId != null
                val fetchConcluded = viewModel.isFetching.value == false
                val noFetchError = viewModel.fetchError.value == null
                val notJustDeleted = viewModel.deleteInstallationDetailResult.value !is DeleteInstallationDetailResult.Success
                val notJustVerified = viewModel.verifyInstallationResult.value !is VerifyInstallationDetailResult.Success 

                if (fetchAttempted && fetchConcluded && noFetchError && notJustDeleted && notJustVerified) {
                    Toast.makeText(this, "Could not load existing details. Fill form to create new.", Toast.LENGTH_LONG).show()
                    updateUiForCreateMode()
                }
            }
        }

        viewModel.fetchError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Fetch Failed: $it", Toast.LENGTH_LONG).show()
                if (currentInstallationId != null) { 
                    updateUiForCreateMode()
                }
            }
        }

        viewModel.deleteInstallationDetailResult.observe(this) { result ->
            when (result) {
                is DeleteInstallationDetailResult.Loading -> showLoadingDialog("Deleting Details...")
                is DeleteInstallationDetailResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is DeleteInstallationDetailResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Delete Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isAnyOtherLoadingOperationActive(): Boolean {
        return viewModel.createResult.value is InstallationDetailsResult.Loading ||
               viewModel.updateResult.value is InstallationDetailsResult.Loading ||
               viewModel.isFetching.value == true ||
               viewModel.deleteInstallationDetailResult.value is DeleteInstallationDetailResult.Loading ||
               viewModel.verifyInstallationResult.value is VerifyInstallationDetailResult.Loading 
    }

    private fun updateUiForCreateMode() {
        isEditMode = false
        currentInstallationId = null 
        supportActionBar?.title = "Add Installation Details"
        
        setInstallationFormFieldsEnabled(true)
        binding.llVerifiedStatusInstallation.visibility = View.GONE 
        binding.verificationSectionInstallation.visibility = View.GONE 
        
        binding.buttonSubmitInstallationDetails.text = "Submit Installation Details"
        binding.buttonSubmitInstallationDetails.visibility = View.VISIBLE 
        binding.buttonSubmitInstallationDetails.isEnabled = true      
        binding.buttonSubmitInstallationDetails.alpha = 1.0f        

        clearForm()
        installationPhotoUri = null
        existingInstallationPhotoRelativePath = null
        binding.ivInstallationPhotoPreview.visibility = View.GONE
        binding.tvSelectedPhotoFileName.visibility = View.GONE
        binding.ivInstallationPhotoPreview.isClickable = false
        binding.btnDeleteInstallationDetails.visibility = View.GONE
    }

    private fun populateForm(detail: InstallationInfo) {
        isEditMode = true
        currentInstallationId = detail.id
        supportActionBar?.title = "Edit Installation Details"

        binding.etCapacityKw.setText(detail.capacityKw ?: "")
        binding.actSystemType.setText(detail.systemType ?: "", false)
        binding.etLatitude.setText(detail.latitude ?: "")
        binding.etLongitude.setText(detail.longitude ?: "")

        detail.installationLocation?.takeIf { it.isNotEmpty() }?.let {
            path ->
            existingInstallationPhotoRelativePath = path
            installationPhotoUri = null
            binding.tvSelectedPhotoFileName.text = path.substringAfterLast('/')
            Glide.with(this).load(BASE_STORAGE_URL + path)
                .placeholder(R.drawable.image_upload_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(binding.ivInstallationPhotoPreview)
            binding.ivInstallationPhotoPreview.visibility = View.VISIBLE
            binding.tvSelectedPhotoFileName.visibility = View.VISIBLE
            binding.ivInstallationPhotoPreview.isClickable = true
        } ?: run {
            binding.ivInstallationPhotoPreview.visibility = View.GONE
            binding.tvSelectedPhotoFileName.visibility = View.GONE
            binding.ivInstallationPhotoPreview.isClickable = false
            existingInstallationPhotoRelativePath = null
        }

        if (userRole.equals("admin", ignoreCase = true) && currentInstallationId != null) {
            binding.btnDeleteInstallationDetails.visibility = View.VISIBLE
        } else {
            binding.btnDeleteInstallationDetails.visibility = View.GONE
        }

        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)
        val currentVerificationStatus = detail.verificationStatus?.lowercase(Locale.ROOT) ?: STATUS_PENDING
        val verificationRemark = detail.verificationRemark

        // Always manage status display
        if (currentVerificationStatus.isNotEmpty() && currentVerificationStatus != STATUS_PENDING) {
            var statusMessage = "Status: ${currentVerificationStatus.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
            if (!verificationRemark.isNullOrEmpty()) {
                statusMessage += " - ${verificationRemark}"
            }
            binding.tvVerifiedStatusMessageInstallation.text = statusMessage
            binding.llVerifiedStatusInstallation.visibility = View.VISIBLE

            // Set text color based on status
            // TODO: Define these colors in your app/src/main/res/values/colors.xml
            // E.g., <color name="status_verified_color">#4CAF50</color> (Green)
            //       <color name="status_rejected_color">#F44336</color> (Red)
            //       <color name="status_objection_color">#FF9800</color> (Orange)
            //       <color name="status_default_color">#000000</color> (Black or your default text color)
            val colorResId = when (currentVerificationStatus) {
                STATUS_VERIFIED -> R.color.status_verified_color
                STATUS_REJECTED -> R.color.status_rejected_color
                STATUS_OBJECTION -> R.color.status_objection_color
                else -> R.color.status_default_color // Fallback for any other status, including pending if it were made visible
            }
            binding.tvVerifiedStatusMessageInstallation.setTextColor(ContextCompat.getColor(this, colorResId))

        } else {
            binding.llVerifiedStatusInstallation.visibility = View.GONE
        }

        // Form field editability
        val canEditFields = currentVerificationStatus != STATUS_VERIFIED
        setInstallationFormFieldsEnabled(canEditFields)

        // Verification Section visibility (only for privileged users in edit mode)
        val showVerificationSection = isEditMode && isPrivilegedUser
        binding.verificationSectionInstallation.visibility = if (showVerificationSection) View.VISIBLE else View.GONE
        if (showVerificationSection) {
            binding.actVerificationStatusInstallation.setText(currentVerificationStatus, false)
            binding.etVerificationRemarkInstallation.setText(verificationRemark ?: "")
        }

        // Main Submit/Update Button Logic
        if (showVerificationSection) { // If verification section is shown, main submit button is hidden
            binding.buttonSubmitInstallationDetails.visibility = View.GONE
        } else {
            binding.buttonSubmitInstallationDetails.visibility = View.VISIBLE
            if (currentVerificationStatus == STATUS_VERIFIED) {
                binding.buttonSubmitInstallationDetails.isEnabled = false
                binding.buttonSubmitInstallationDetails.alpha = 0.5f
                binding.buttonSubmitInstallationDetails.text = "Details Verified"
            } else {
                binding.buttonSubmitInstallationDetails.isEnabled = true
                binding.buttonSubmitInstallationDetails.alpha = 1.0f
                binding.buttonSubmitInstallationDetails.text = if (isEditMode) "Update Installation Details" else "Submit Installation Details"
            }
        }
    }

    private fun clearForm() {
        binding.etCapacityKw.setText("")
        binding.actSystemType.setText("", false)
        binding.etLatitude.setText("")
        binding.etLongitude.setText("")
        binding.tilCapacityKw.error = null
        binding.tilSystemType.error = null
        binding.tilLatitude.error = null
        binding.tilLongitude.error = null
    }

    private fun handleSubmit() {
        val currentVerificationStatus = viewModel.fetchedDetail.value?.verificationStatus?.lowercase(Locale.ROOT)
        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)

        // Prevent submission if form fields are generally not editable due to status, unless privileged user (who would use verification section)
        if (!binding.etCapacityKw.isEnabled && !isPrivilegedUser) {
             Toast.makeText(this, "Details are ${currentVerificationStatus ?: "finalized"} and cannot be edited.", Toast.LENGTH_SHORT).show()
            return
        }
        // Prevent main submit if privileged user and verification section is active (they should use that section's submit)
        if (isPrivilegedUser && binding.verificationSectionInstallation.visibility == View.VISIBLE){
            Toast.makeText(this, "Please use the verification section to submit changes.", Toast.LENGTH_SHORT).show()
            return
        }

        clearAllFieldErrors()

        val capacityStr = binding.etCapacityKw.text.toString().trim()
        val systemType = binding.actSystemType.text.toString().trim()
        val latitudeStr = binding.etLatitude.text.toString().trim()
        val longitudeStr = binding.etLongitude.text.toString().trim()

        var isValid = true
        if (capacityStr.isEmpty()) {
            binding.tilCapacityKw.error = "Capacity is required"; isValid = false
        }
        val capacityKw = capacityStr.toDoubleOrNull()
        if (capacityKw == null && capacityStr.isNotEmpty()) {
            binding.tilCapacityKw.error = "Invalid capacity value"; isValid = false
        } else if (capacityKw != null && capacityKw <= 0) {
            binding.tilCapacityKw.error = "Capacity must be positive"; isValid = false
        }

        if (systemType.isEmpty()) {
            binding.tilSystemType.error = "System type is required"; isValid = false
        } else if (!SYSTEM_TYPES.map { it.lowercase(Locale.ROOT) }.contains(systemType.lowercase(Locale.ROOT))) {
             binding.tilSystemType.error = "Invalid system type selected"; isValid = false
        }

        if (latitudeStr.isEmpty()) {
            binding.tilLatitude.error = "Latitude is required"; isValid = false
        }
        val latitude = latitudeStr.toDoubleOrNull()
        if (latitude == null && latitudeStr.isNotEmpty()) {
            binding.tilLatitude.error = "Invalid latitude value"; isValid = false
        } else if (latitude != null && (latitude < -90 || latitude > 90)) {
            binding.tilLatitude.error = "Latitude must be between -90 and 90"; isValid = false
        }

        if (longitudeStr.isEmpty()) {
            binding.tilLongitude.error = "Longitude is required"; isValid = false
        }
        val longitude = longitudeStr.toDoubleOrNull()
        if (longitude == null && longitudeStr.isNotEmpty()) {
            binding.tilLongitude.error = "Invalid longitude value"; isValid = false
        } else if (longitude != null && (longitude < -180 || longitude > 180)) {
            binding.tilLongitude.error = "Longitude must be between -180 and 180"; isValid = false
        }

        if (!isEditMode && installationPhotoUri == null) {
            Toast.makeText(this, "Please upload installation location photo.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val installationPhotoBase64 = installationPhotoUri?.let { uriToDataUriString(it) }
        if (installationPhotoUri != null && installationPhotoBase64 == null) {
            Toast.makeText(this, "Failed to process installation photo.", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val currentAppId = applicationId ?: run {
            Toast.makeText(this, "Application ID missing.", Toast.LENGTH_LONG).show(); return
        }

        val request = InstallationDetailsRequest(
            applicationId = currentAppId,
            capacityKw = capacityKw!!, 
            systemType = systemType,
            latitude = latitude!!,     
            longitude = longitude!!,  
            installationLocation = installationPhotoBase64
        )


        if (isEditMode && currentInstallationId != null) {
            viewModel.updateInstallationDetail(token, currentInstallationId!!, request)
        } else {
            viewModel.submitInstallationDetails(token, request)
        }
    }

    private fun handleSubmissionError(errorMessage: String?, errors: Map<String, List<String>>?) {
        val generalMessage = errorMessage ?: "An unknown error occurred."
        var specificErrors = ""
        errors?.forEach { (field, messages) ->
            val message = messages.joinToString(", ")
            when (field) {
                "application_id" -> specificErrors += "App ID: $message\n"
                "capacity_kw" -> { binding.tilCapacityKw.error = message; specificErrors += "Capacity: $message\n" }
                "system_type" -> { binding.tilSystemType.error = message; specificErrors += "System Type: $message\n" }
                "latitude" -> { binding.tilLatitude.error = message; specificErrors += "Latitude: $message\n" }
                "longitude" -> { binding.tilLongitude.error = message; specificErrors += "Longitude: $message\n" }
                "installation_location" -> specificErrors += "Photo: $message\n"
                else -> specificErrors += "${field.replaceFirstChar { it.titlecase(Locale.ROOT) }}: $message\n"
            }
        }
        val toastMessage = if (specificErrors.isNotEmpty()) "Submission Failed:\n${generalMessage}\n${specificErrors.trim()}" else "Submission Failed: $generalMessage"
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
    }

    private fun clearAllFieldErrors() {
        binding.tilCapacityKw.error = null
        binding.tilSystemType.error = null
        binding.tilLatitude.error = null
        binding.tilLongitude.error = null
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        try {
            installationPhotoPickerLauncher.launch(Intent.createChooser(intent, "Select Image"))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use {
            cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) return cursor.getString(displayNameIndex)
            }
        }
        return uri.lastPathSegment
    }

    private fun uriToDataUriString(uri: Uri): String? {
        return try {
            val mimeType = contentResolver.getType(uri)
            if (mimeType?.startsWith("image/") != true) {
                Toast.makeText(this, "Invalid file type. Only images are allowed.", Toast.LENGTH_SHORT).show()
                return null
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mimeType;base64,$base64String"
            } ?: run {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error converting URI to Base64 data URI string", e)
            Toast.makeText(this, "Failed to read file data.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun viewInstallationPhoto() {
        var uriToView: Uri? = null
        var isRemote = false

        if (installationPhotoUri != null) {
            uriToView = installationPhotoUri
        } else if (isEditMode && existingInstallationPhotoRelativePath != null) {
            uriToView = Uri.parse(BASE_STORAGE_URL + existingInstallationPhotoRelativePath)
            isRemote = true
        }

        if (uriToView != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToView, "image/*")
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                if (!isRemote) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
             val chooser = Intent.createChooser(intent, "Open with")
            try {
                startActivity(chooser)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No application found to view images.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "No photo available to view.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingDialog(message: String = "Loading...") {
        if (loadingDialog?.dialog?.isShowing == true && loadingDialog?.arguments?.getString(LoadingDialogFragment.ARG_MESSAGE) == message) return
        hideLoadingDialog()
        
        loadingDialog = LoadingDialogFragment.newInstance(message)
        if (!isFinishing && !isDestroyed) {
            try {
                loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error showing dialog", e)
            }
        }
    }

    private fun hideLoadingDialog() {
        if (loadingDialog?.dialog?.isShowing == true) {
            if (!isFinishing && !isDestroyed) {
                try {
                    loadingDialog?.dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error dismissing dialog", e)
                }
            }
        }
        loadingDialog = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }
}
