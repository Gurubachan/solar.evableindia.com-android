package com.solar.ev

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.core.content.ContextCompat // Required for color access
import com.bumptech.glide.Glide
import com.solar.ev.databinding.ActivityKycBinding
import com.solar.ev.model.common.DocumentVerificationRequest
import com.solar.ev.model.kyc.KYCDetails
import com.solar.ev.model.kyc.KYCSubmitRequest
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.kyc.KYCViewModelFactory
import com.solar.ev.viewModel.kyc.DeleteKYCDocumentResult
import com.solar.ev.viewModel.kyc.KYCSubmitResult
import com.solar.ev.viewModel.kyc.KYCUpdateResult
import com.solar.ev.viewModel.kyc.KYCViewModel
import com.solar.ev.viewModel.kyc.VerifyKYCDocumentResult
import java.io.IOException
import java.util.Locale

class KYCActivity : BaseActivity() {

    private lateinit var binding: ActivityKycBinding
    private lateinit var sessionManager: SessionManager
    private var applicationId: String? = null
    private var currentKycId: String? = null
    private var isEditMode = false
    private var userRole: String? = null

    private var aadhaarFrontUri: Uri? = null
    private var aadhaarBackUri: Uri? = null
    private var panUri: Uri? = null

    private var existingAadhaarFrontPath: String? = null
    private var existingAadhaarBackPath: String? = null
    private var existingPanPath: String? = null

    private var loadingDialog: LoadingDialogFragment? = null

    private val kycViewModel: KYCViewModel by viewModels {
        KYCViewModelFactory(RetrofitInstance.api)
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_KYC_ID = "EXTRA_KYC_ID"
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
        private const val TAG = "KYCActivity"
        private val VERIFICATION_STATUSES = arrayOf("pending", "verified", "rejected", "objection")
        private const val STATUS_VERIFIED = "verified"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_OBJECTION = "objection"
    }

    private val aadhaarFrontPickerLauncher = createFilePickerLauncher { uri ->
        aadhaarFrontUri = uri
        existingAadhaarFrontPath = null
        binding.ivAadhaarFrontPreview.setImageURI(uri)
        binding.ivAadhaarFrontPreview.visibility = View.VISIBLE
        binding.ivAadhaarFrontPreview.isClickable = true
    }

    private val aadhaarBackPickerLauncher = createFilePickerLauncher { uri ->
        aadhaarBackUri = uri
        existingAadhaarBackPath = null
        binding.ivAadhaarBackPreview.setImageURI(uri)
        binding.ivAadhaarBackPreview.visibility = View.VISIBLE
        binding.ivAadhaarBackPreview.isClickable = true
    }

    private val panPickerLauncher = createFilePickerLauncher { uri ->
        panUri = uri
        existingPanPath = null
        binding.ivPanPreview.setImageURI(uri)
        binding.ivPanPreview.visibility = View.VISIBLE
        binding.ivPanPreview.isClickable = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getUserRole()
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        currentKycId = intent.getStringExtra(EXTRA_KYC_ID)

        if (applicationId == null && currentKycId == null) {
            Toast.makeText(this, "Application ID or KYC ID is missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()
        setupVerificationSection()
        observeViewModel()

        if (currentKycId?.isNotEmpty() == true) {
            val token = sessionManager.getUserToken()
            if (!token.isNullOrEmpty()) {
                kycViewModel.fetchKYCDocument(token, currentKycId!!)
            } else {
                Toast.makeText(this, "Session expired. Cannot fetch KYC details.", Toast.LENGTH_LONG).show()
                updateUiForCreateMode()
            }
        } else {
            updateUiForCreateMode()
        }
    }

    private fun setKycFormFieldsEnabled(enabled: Boolean) {
        binding.etAadhaarNumber.isEnabled = enabled
        binding.etPanNumber.isEnabled = enabled
        binding.buttonUploadAadhaarFront.isEnabled = enabled
        binding.buttonUploadAadhaarBack.isEnabled = enabled
        binding.buttonUploadPan.isEnabled = enabled

        binding.buttonUploadAadhaarFront.alpha = if (enabled) 1.0f else 0.5f
        binding.buttonUploadAadhaarBack.alpha = if (enabled) 1.0f else 0.5f
        binding.buttonUploadPan.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarKyc)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun updateUiForCreateMode() {
        isEditMode = false
        currentKycId = null
        supportActionBar?.title = "Add KYC Details"

        setKycFormFieldsEnabled(true)
        binding.llVerifiedStatusKyc.visibility = View.GONE
        binding.verificationSectionKyc.visibility = View.GONE

        binding.buttonSubmitKyc.text = "Submit KYC"
        binding.buttonSubmitKyc.visibility = View.VISIBLE
        binding.buttonSubmitKyc.isEnabled = true
        binding.buttonSubmitKyc.alpha = 1.0f

        binding.etAadhaarNumber.setText("")
        binding.etPanNumber.setText("")

        binding.ivAadhaarFrontPreview.visibility = View.GONE
        binding.ivAadhaarFrontPreview.isClickable = false
        binding.ivAadhaarBackPreview.visibility = View.GONE
        binding.ivAadhaarBackPreview.isClickable = false
        binding.ivPanPreview.visibility = View.GONE
        binding.ivPanPreview.isClickable = false

        aadhaarFrontUri = null
        aadhaarBackUri = null
        panUri = null
        existingAadhaarFrontPath = null
        existingAadhaarBackPath = null
        existingPanPath = null

        binding.btnDeleteKycDetails.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.buttonUploadAadhaarFront.setOnClickListener { openFilePicker(aadhaarFrontPickerLauncher) }
        binding.buttonUploadAadhaarBack.setOnClickListener { openFilePicker(aadhaarBackPickerLauncher) }
        binding.buttonUploadPan.setOnClickListener { openFilePicker(panPickerLauncher) }
        binding.buttonSubmitKyc.setOnClickListener { handleSubmitKYC() }

        binding.ivAadhaarFrontPreview.setOnClickListener { viewDocument(aadhaarFrontUri, existingAadhaarFrontPath) }
        binding.ivAadhaarBackPreview.setOnClickListener { viewDocument(aadhaarBackUri, existingAadhaarBackPath) }
        binding.ivPanPreview.setOnClickListener { viewDocument(panUri, existingPanPath) }
        binding.btnDeleteKycDetails.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun setupVerificationSection() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, VERIFICATION_STATUSES)
        binding.actVerificationStatusKyc.setAdapter(statusAdapter)
        binding.buttonSubmitVerificationKyc.setOnClickListener { handleSubmitKycVerification() }
    }

    private fun handleSubmitKycVerification() {
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

        if (currentKycId == null) {
            Toast.makeText(this, "KYC ID is missing. Cannot verify.", Toast.LENGTH_SHORT).show()
            return
        }

        val verificationStatus = binding.actVerificationStatusKyc.text.toString()
        val verificationRemark = binding.etVerificationRemarkKyc.text.toString().trim()

        if (verificationStatus.isEmpty() || !VERIFICATION_STATUSES.contains(verificationStatus.lowercase(Locale.ROOT))) {
            Toast.makeText(this, "Please select a valid verification status.", Toast.LENGTH_SHORT).show()
            binding.tilVerificationStatusKyc.error = "Required"
            return
        }
        binding.tilVerificationStatusKyc.error = null

        val request = DocumentVerificationRequest(
            verificationStatus = verificationStatus,
            verificationRemark = verificationRemark.takeIf { it.isNotEmpty() } ?: ""
        )
        kycViewModel.verifyKYCDocument(token, currentKycId!!, request)
    }


    private fun showDeleteConfirmationDialog() {
        if (currentKycId == null) {
            Toast.makeText(this, "Cannot delete. KYC ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete these KYC details? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val token = sessionManager.getUserToken()
                if (token != null && userRole.equals("admin", ignoreCase = true) && currentKycId != null) {
                    kycViewModel.deleteKYCDocumentById(token, currentKycId!!)
                } else if (token == null) {
                    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                } else if (!userRole.equals("admin", ignoreCase = true)){
                    Toast.makeText(this, "You do not have permission to delete KYC details.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun createFilePickerLauncher(onFileSelected: (Uri) -> Unit): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let(onFileSelected)
            }
        }
    }

    private fun viewDocument(localUri: Uri?, remotePath: String?) {
        val uriToView: Uri? = localUri ?: remotePath?.let { Uri.parse(BASE_STORAGE_URL + it) }
        val isRemote = localUri == null && remotePath != null

        if (uriToView != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            val mimeType = contentResolver.getType(uriToView) ?: "*/*" 
            intent.setDataAndType(uriToView, mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (!isRemote) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = if (mimeType == "application/pdf") Intent.createChooser(intent, "Open with") else intent
            try {
                startActivity(chooserIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No application found to view this file type.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "No document available to view.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun openFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" 
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
        }
        try {
            launcher.launch(Intent.createChooser(intent, "Select Document"))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use {
            cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return cursor.getString(displayNameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun uriToDataUriString(uri: Uri): String? {
        return try {
            val mimeType = contentResolver.getType(uri)
            if (mimeType !in listOf("image/jpeg", "image/png", "application/pdf")) {
                 Toast.makeText(this, "Unsupported file type: $mimeType. Please select JPG, PNG, or PDF.", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "Failed to read file data for ${getFileName(uri)}.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun populateKYCForm(details: KYCDetails) {
        isEditMode = true
        currentKycId = details.id 
        supportActionBar?.title = "Edit KYC Details"

        binding.etAadhaarNumber.setText(details.aadhaarNumber ?: "")
        binding.etPanNumber.setText(details.panNumber ?: "")

        details.aadhaarPhotoFront?.takeIf { it.isNotEmpty() }?.let {
            path ->
            existingAadhaarFrontPath = path
            aadhaarFrontUri = null 
            Glide.with(this).load(BASE_STORAGE_URL + path).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_broken_image).into(binding.ivAadhaarFrontPreview)
            binding.ivAadhaarFrontPreview.visibility = View.VISIBLE
            binding.ivAadhaarFrontPreview.isClickable = true
        } ?: run {
            binding.ivAadhaarFrontPreview.visibility = View.GONE
            binding.ivAadhaarFrontPreview.isClickable = false
            existingAadhaarFrontPath = null
        }

        details.aadhaarPhotoBack?.takeIf { it.isNotEmpty() }?.let {
            path ->
            existingAadhaarBackPath = path
            aadhaarBackUri = null 
            Glide.with(this).load(BASE_STORAGE_URL + path).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_broken_image).into(binding.ivAadhaarBackPreview)
            binding.ivAadhaarBackPreview.visibility = View.VISIBLE
            binding.ivAadhaarBackPreview.isClickable = true
        } ?: run {
            binding.ivAadhaarBackPreview.visibility = View.GONE
            binding.ivAadhaarBackPreview.isClickable = false
            existingAadhaarBackPath = null
        }

        details.panPhoto?.takeIf { it.isNotEmpty() }?.let {
            path ->
            existingPanPath = path
            panUri = null 
            Glide.with(this).load(BASE_STORAGE_URL + path).placeholder(R.drawable.ic_image_placeholder).error(R.drawable.ic_broken_image).into(binding.ivPanPreview)
            binding.ivPanPreview.visibility = View.VISIBLE
            binding.ivPanPreview.isClickable = true
        } ?: run {
            binding.ivPanPreview.visibility = View.GONE
            binding.ivPanPreview.isClickable = false
            existingPanPath = null
        }

        if (userRole.equals("admin", ignoreCase = true) && currentKycId != null) {
            binding.btnDeleteKycDetails.visibility = View.VISIBLE
        } else {
            binding.btnDeleteKycDetails.visibility = View.GONE
        }

        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)
        val currentVerificationStatus = details.verificationStatus?.lowercase(Locale.ROOT) ?: STATUS_PENDING
        val verificationRemark = details.verificationRemark

        // Status Display (Always show if not pending or empty)
        if (currentVerificationStatus.isNotEmpty() && currentVerificationStatus != STATUS_PENDING) {
            var statusMessage = "Status: ${currentVerificationStatus.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
            if (!verificationRemark.isNullOrEmpty()) {
                statusMessage += " - ${verificationRemark}"
            }
            binding.tvVerifiedStatusMessageKyc.text = statusMessage
            binding.llVerifiedStatusKyc.visibility = View.VISIBLE

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
                else -> R.color.status_default_color // Fallback for any other status
            }
            binding.tvVerifiedStatusMessageKyc.setTextColor(ContextCompat.getColor(this, colorResId))

        } else {
            binding.llVerifiedStatusKyc.visibility = View.GONE
        }

        // Form Field Editability
        val canEditFields = currentVerificationStatus != STATUS_VERIFIED
        setKycFormFieldsEnabled(canEditFields)

        // Verification Section Visibility
        val showVerificationSection = isEditMode && isPrivilegedUser
        binding.verificationSectionKyc.visibility = if (showVerificationSection) View.VISIBLE else View.GONE
        if (showVerificationSection) {
            binding.actVerificationStatusKyc.setText(currentVerificationStatus, false)
            binding.etVerificationRemarkKyc.setText(verificationRemark ?: "")
        }

        // Main Submit/Update Button Logic
        if (showVerificationSection) {
            binding.buttonSubmitKyc.visibility = View.GONE
        } else {
            binding.buttonSubmitKyc.visibility = View.VISIBLE
            if (currentVerificationStatus == STATUS_VERIFIED) {
                binding.buttonSubmitKyc.isEnabled = false
                binding.buttonSubmitKyc.alpha = 0.5f
                binding.buttonSubmitKyc.text = "Details Verified"
            } else {
                binding.buttonSubmitKyc.isEnabled = true
                binding.buttonSubmitKyc.alpha = 1.0f
                binding.buttonSubmitKyc.text = if (isEditMode) "Update KYC" else "Submit KYC"
            }
        }
    }

    private fun handleSubmitKYC() {
        val currentVerificationStatus = kycViewModel.kycDetails.value?.verificationStatus?.lowercase(Locale.ROOT)
        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)

        if (!binding.etAadhaarNumber.isEnabled && !isPrivilegedUser) {
             Toast.makeText(this, "Details are ${currentVerificationStatus ?: "finalized"} and cannot be edited.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isPrivilegedUser && binding.verificationSectionKyc.visibility == View.VISIBLE){
             Toast.makeText(this, "Please use the verification section to submit changes.", Toast.LENGTH_SHORT).show()
            return
        }

        val aadhaarNumber = binding.etAadhaarNumber.text.toString().trim()
        val panNumber = binding.etPanNumber.text.toString().trim()

        val currentAppId = applicationId
        if (currentAppId == null && !isEditMode) { 
            Toast.makeText(this, "Application ID is missing.", Toast.LENGTH_LONG).show()
            return
        }

        if (aadhaarNumber.isEmpty()) {
            binding.tilAadhaarNumber.error = "Aadhaar number is required"; return
        }
        if (aadhaarNumber.length != 12 || !aadhaarNumber.matches(Regex("^\\d{12}$"))) { 
            binding.tilAadhaarNumber.error = "Aadhaar must be 12 digits"; return
        }
        binding.tilAadhaarNumber.error = null

        if (panNumber.isEmpty()) {
            binding.tilPanNumber.error = "PAN number is required"; return
        }
        if (!panNumber.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$"))) { 
            binding.tilPanNumber.error = "Invalid PAN format"; return
        }
        binding.tilPanNumber.error = null

        if (!isEditMode && aadhaarFrontUri == null) {
            Toast.makeText(this, "Please upload Aadhaar front document", Toast.LENGTH_SHORT).show(); return
        }
        if (!isEditMode && aadhaarBackUri == null) {
            Toast.makeText(this, "Please upload Aadhaar back document", Toast.LENGTH_SHORT).show(); return
        }
        if (!isEditMode && panUri == null) {
            Toast.makeText(this, "Please upload PAN document", Toast.LENGTH_SHORT).show(); return
        }

        val aadhaarPhotoFrontBase64 = aadhaarFrontUri?.let { uriToDataUriString(it) }
        val aadhaarPhotoBackBase64 = aadhaarBackUri?.let { uriToDataUriString(it) }
        val panPhotoBase64 = panUri?.let { uriToDataUriString(it) }

        if (aadhaarFrontUri != null && aadhaarPhotoFrontBase64 == null) {
             Toast.makeText(this, "Failed to process new Aadhaar front document.", Toast.LENGTH_SHORT).show(); return
        }
        if (aadhaarBackUri != null && aadhaarPhotoBackBase64 == null) {
             Toast.makeText(this, "Failed to process new Aadhaar back document.", Toast.LENGTH_SHORT).show(); return
        }
        if (panUri != null && panPhotoBase64 == null) {
             Toast.makeText(this, "Failed to process new PAN document.", Toast.LENGTH_SHORT).show(); return
        }

        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show(); return
        }

        val kycRequest = KYCSubmitRequest(
            applicationId = currentAppId,
            aadhaarNumber = aadhaarNumber,
            aadhaarPhotoFront = aadhaarPhotoFrontBase64,
            aadhaarPhotoBack = aadhaarPhotoBackBase64,
            panNumber = panNumber,
            panPhoto = panPhotoBase64
        )

        if (isEditMode && currentKycId != null) {
            kycViewModel.updateKYCDocuments(token, currentKycId!!, kycRequest)
        } else if (currentAppId != null) { 
            kycViewModel.submitKYCDocuments(token, kycRequest)
        } else {
            Toast.makeText(this, "Application ID is missing for new KYC.", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        kycViewModel.kycSubmitResult.observe(this) { result ->
            when (result) {
                is KYCSubmitResult.Loading -> showLoadingDialog("Submitting KYC...")
                is KYCSubmitResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "KYC created successfully!", Toast.LENGTH_LONG).show()
                    finish() 
                }
                is KYCSubmitResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        kycViewModel.kycUpdateResult.observe(this) { result ->
            when (result) {
                is KYCUpdateResult.Loading -> showLoadingDialog("Updating KYC...")
                is KYCUpdateResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "KYC updated successfully!", Toast.LENGTH_LONG).show()
                    finish() 
                }
                is KYCUpdateResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        kycViewModel.isFetchingKyc.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingDialog("Fetching KYC Details...")
            } else {
                if (!isAnyOtherLoadingOperationActive()) {
                    hideLoadingDialog()
                }
            }
        }

        kycViewModel.fetchKycError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Failed to fetch KYC: $it", Toast.LENGTH_LONG).show()
                 updateUiForCreateMode() 
            }
        }

        kycViewModel.kycDetails.observe(this) { details ->
            if (details != null) {
                populateKYCForm(details)
            } else {
                val fetchAttempted = currentKycId?.isNotEmpty() == true
                val fetchConcluded = kycViewModel.isFetchingKyc.value == false
                val noFetchError = kycViewModel.fetchKycError.value == null
                val notJustDeleted = kycViewModel.deleteKycDocumentResult.value !is DeleteKYCDocumentResult.Success
                val notJustVerified = kycViewModel.verifyKycDocumentResult.value !is VerifyKYCDocumentResult.Success

                if (fetchAttempted && fetchConcluded && noFetchError && notJustDeleted && notJustVerified) {
                    Toast.makeText(this, "No existing KYC data found. Please fill the form.", Toast.LENGTH_SHORT).show()
                    updateUiForCreateMode()
                }
            }
        }

        kycViewModel.deleteKycDocumentResult.observe(this) { result ->
            when(result) {
                is DeleteKYCDocumentResult.Loading -> {
                    showLoadingDialog("Deleting KYC Details...")
                }
                is DeleteKYCDocumentResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is DeleteKYCDocumentResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Delete Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        kycViewModel.verifyKycDocumentResult.observe(this) { result ->
            when(result) {
                is VerifyKYCDocumentResult.Loading -> {
                    showLoadingDialog("Submitting KYC Verification...")
                }
                is VerifyKYCDocumentResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    currentKycId?.let { kycId ->
                        sessionManager.getUserToken()?.takeIf { it.isNotEmpty() }?.let { token ->
                            kycViewModel.fetchKYCDocument(token, kycId)
                        }
                    } ?: finish() 
                }
                is VerifyKYCDocumentResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "KYC Verification Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleSubmissionError(errorMessage: String?, errors: Map<String, List<String>>?) {
        val generalMessage = errorMessage ?: "An unknown error occurred."
        var specificErrorsText = ""

        errors?.forEach { (field, messages) ->
            val message = messages.joinToString(", ")
            when (field.lowercase(Locale.ROOT)) {
                "applicationid", "application_id" -> specificErrorsText += "App ID: $message\n"
                "aandhaarnumber", "aadhaar_number" -> { binding.tilAadhaarNumber.error = message; specificErrorsText += "Aadhaar No: $message\n" }
                "aandhaarphotofront", "aadhaar_photo_front" -> specificErrorsText += "Aadhaar Front: $message\n"
                "aandhaarphotoback", "aadhaar_photo_back" -> specificErrorsText += "Aadhaar Back: $message\n"
                "pannumber", "pan_number" -> { binding.tilPanNumber.error = message; specificErrorsText += "PAN No: $message\n" }
                "panphoto", "pan_photo" -> specificErrorsText += "PAN Photo: $message\n"
                else -> specificErrorsText += "${field.replaceFirstChar { it.titlecase(Locale.ROOT) }}: $message\n"
            }
        }
        val toastMessage = if (specificErrorsText.isNotEmpty()) "Submission Failed:\n${generalMessage}\n${specificErrorsText.trim()}" else "Submission Failed: $generalMessage"
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        if (errors == null || errors.isEmpty()) {
            clearAllFieldErrors()
        }
    }

    private fun clearAllFieldErrors() {
        binding.tilAadhaarNumber.error = null
        binding.tilPanNumber.error = null
    }


    private fun isAnyOtherLoadingOperationActive(): Boolean {
        return kycViewModel.kycSubmitResult.value is KYCSubmitResult.Loading ||
               kycViewModel.kycUpdateResult.value is KYCUpdateResult.Loading ||
               kycViewModel.isFetchingKyc.value == true ||
               kycViewModel.deleteKycDocumentResult.value is DeleteKYCDocumentResult.Loading ||
               kycViewModel.verifyKycDocumentResult.value is VerifyKYCDocumentResult.Loading
    }

    private fun showLoadingDialog(dialogMessage: String = "Loading...") {
        if (loadingDialog?.dialog?.isShowing == true && loadingDialog?.arguments?.getString(LoadingDialogFragment.ARG_MESSAGE) == dialogMessage) {
             return
        }
        hideLoadingDialog() 

        loadingDialog = LoadingDialogFragment.newInstance(dialogMessage)
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
