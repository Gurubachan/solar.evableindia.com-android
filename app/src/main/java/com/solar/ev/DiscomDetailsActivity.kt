package com.solar.ev

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
import com.solar.ev.databinding.ActivityDiscomDetailsBinding
import com.solar.ev.model.common.DocumentVerificationRequest
import com.solar.ev.model.discom.DiscomDetailsRequest
import com.solar.ev.model.discom.DiscomInfo
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.discom.DeleteDiscomDetailResult
import com.solar.ev.viewModel.discom.DiscomDetailsResult
import com.solar.ev.viewModel.discom.DiscomDetailsViewModel
import com.solar.ev.viewModel.discom.DiscomDetailsViewModelFactory
import com.solar.ev.viewModel.discom.VerifyDiscomDetailResult
import java.io.IOException
import java.util.Locale

class DiscomDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityDiscomDetailsBinding
    private lateinit var sessionManager: SessionManager
    private var applicationId: String? = null
    private var currentDiscomId: String? = null
    private var electricBillUri: Uri? = null
    private var existingBillFileName: String? = null
    private var existingBillRelativePath: String? = null
    private var isEditMode = false
    private var userRole: String? = null

    private var loadingDialog: LoadingDialogFragment? = null

    private val discomViewModel: DiscomDetailsViewModel by viewModels {
        DiscomDetailsViewModelFactory(RetrofitInstance.api)
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_DISCOM_ID = "EXTRA_DISCOM_ID"
        private const val TAG = "DiscomDetailsActivity"
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
        private val VERIFICATION_STATUSES = arrayOf("pending", "verified", "rejected", "objection")
        private const val STATUS_VERIFIED = "verified"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_OBJECTION = "objection"
    }

    private val electricBillPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let {
                uri ->
                val mimeType = contentResolver.getType(uri)
                if (mimeType == "application/pdf") {
                    electricBillUri = uri 
                    binding.tvSelectedBillFileName.text = getFileName(uri) ?: "Selected PDF"
                    binding.tvSelectedBillFileName.isClickable = true
                    binding.tvSelectedBillFileName.visibility = View.VISIBLE
                    showPdfPreviewPlaceholder(true)
                    existingBillRelativePath = null // Clear existing path if new file is selected
                    existingBillFileName = null
                } else {
                    Toast.makeText(this, "Please select a PDF file.", Toast.LENGTH_LONG).show()
                    electricBillUri = null 
                    if (isEditMode && existingBillRelativePath != null) {
                        binding.tvSelectedBillFileName.text = "Current bill: $existingBillFileName"
                        binding.tvSelectedBillFileName.isClickable = true
                        showPdfPreviewPlaceholder(true)
                    } else {
                        binding.tvSelectedBillFileName.text = "No file selected"
                        binding.tvSelectedBillFileName.isClickable = false
                        showPdfPreviewPlaceholder(false)
                    }
                }
            }
        } else {
             if (electricBillUri == null && isEditMode && existingBillRelativePath != null){
                binding.tvSelectedBillFileName.text = "Current bill: $existingBillFileName"
                binding.tvSelectedBillFileName.isClickable = true
                showPdfPreviewPlaceholder(true)
             } else if (electricBillUri == null) {
                binding.tvSelectedBillFileName.text = "No file selected"
                binding.tvSelectedBillFileName.isClickable = false
                showPdfPreviewPlaceholder(false)
             }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscomDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getUserRole()
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        currentDiscomId = intent.getStringExtra(EXTRA_DISCOM_ID)

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing. Cannot proceed.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()
        setupVerificationSection()
        observeViewModel()

        if (currentDiscomId != null) {
            val token = sessionManager.getUserToken()
            if (!token.isNullOrEmpty()) {
                discomViewModel.fetchDiscomDetail(token, currentDiscomId!!)
            } else {
                Toast.makeText(this, "Session expired. Cannot fetch details.", Toast.LENGTH_LONG).show()
                updateUiForCreateMode()
            }
        } else {
            updateUiForCreateMode()
        }
    }

    private fun setDiscomFormFieldsEnabled(enabled: Boolean) {
        binding.etDiscomName.isEnabled = enabled
        binding.etConsumerNumber.isEnabled = enabled
        binding.etCurrentLoad.isEnabled = enabled
        binding.buttonUploadElectricBill.isEnabled = enabled
        binding.buttonUploadElectricBill.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarDiscomDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun updateUiForCreateMode() {
        isEditMode = false
        currentDiscomId = null
        supportActionBar?.title = "Add Discom Details"
        
        setDiscomFormFieldsEnabled(true)
        binding.llVerifiedStatusDiscom.visibility = View.GONE
        binding.verificationSectionDiscom.visibility = View.GONE

        binding.buttonSubmitDiscomDetails.text = "Submit Discom Details"
        binding.buttonSubmitDiscomDetails.visibility = View.VISIBLE
        binding.buttonSubmitDiscomDetails.isEnabled = true
        binding.buttonSubmitDiscomDetails.alpha = 1.0f

        binding.tvSelectedBillFileName.text = "No file selected"
        binding.tvSelectedBillFileName.isClickable = false
        showPdfPreviewPlaceholder(false)
        binding.etDiscomName.setText("")
        binding.etConsumerNumber.setText("")
        binding.etCurrentLoad.setText("")
        electricBillUri = null
        existingBillFileName = null
        existingBillRelativePath = null
        binding.btnDeleteDiscomDetails.visibility = View.GONE
    }

    private fun populateForm(discomInfo: DiscomInfo) {
        isEditMode = true
        currentDiscomId = discomInfo.id
        supportActionBar?.title = "Edit Discom Details"

        binding.etDiscomName.setText(discomInfo.discomName ?: "")
        binding.etConsumerNumber.setText(discomInfo.consumerNumber ?: "")
        binding.etCurrentLoad.setText(discomInfo.currentLoad ?: "")

        if (!discomInfo.latestElectricBill.isNullOrEmpty()) {
            existingBillRelativePath = discomInfo.latestElectricBill
            existingBillFileName = existingBillRelativePath?.substringAfterLast('/')
            binding.tvSelectedBillFileName.text = "Current bill: $existingBillFileName"
            binding.tvSelectedBillFileName.visibility = View.VISIBLE
            binding.tvSelectedBillFileName.isClickable = true
            showPdfPreviewPlaceholder(true)
        } else {
            binding.tvSelectedBillFileName.text = "No existing bill. Select one."
            binding.tvSelectedBillFileName.visibility = View.VISIBLE
            binding.tvSelectedBillFileName.isClickable = false 
            showPdfPreviewPlaceholder(false)
            existingBillRelativePath = null
            existingBillFileName = null
        }
        electricBillUri = null 

        if (userRole.equals("admin", ignoreCase = true) && currentDiscomId != null) {
            binding.btnDeleteDiscomDetails.visibility = View.VISIBLE
        } else {
            binding.btnDeleteDiscomDetails.visibility = View.GONE
        }

        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)
        val currentVerificationStatus = discomInfo.verificationStatus?.lowercase(Locale.ROOT) ?: STATUS_PENDING
        val verificationRemark = discomInfo.verificationRemark

        // Status Display (Always show if not pending or empty)
        if (currentVerificationStatus.isNotEmpty() && currentVerificationStatus != STATUS_PENDING) {
            var statusMessage = "Status: ${currentVerificationStatus.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
            if (!verificationRemark.isNullOrEmpty()) {
                statusMessage += " - ${verificationRemark}"
            }
            binding.tvVerifiedStatusMessageDiscom.text = statusMessage
            binding.llVerifiedStatusDiscom.visibility = View.VISIBLE

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
                else -> R.color.status_default_color
            }
            binding.tvVerifiedStatusMessageDiscom.setTextColor(ContextCompat.getColor(this, colorResId))

        } else {
            binding.llVerifiedStatusDiscom.visibility = View.GONE
        }

        // Form Field Editability
        val canEditFields = currentVerificationStatus != STATUS_VERIFIED
        setDiscomFormFieldsEnabled(canEditFields)

        // Verification Section Visibility
        val showVerificationSection = isEditMode && isPrivilegedUser
        binding.verificationSectionDiscom.visibility = if (showVerificationSection) View.VISIBLE else View.GONE
        if (showVerificationSection) {
            binding.actVerificationStatusDiscom.setText(currentVerificationStatus, false)
            binding.etVerificationRemarkDiscom.setText(verificationRemark ?: "")
        }

        // Main Submit/Update Button Logic
        if (showVerificationSection) {
            binding.buttonSubmitDiscomDetails.visibility = View.GONE
        } else {
            binding.buttonSubmitDiscomDetails.visibility = View.VISIBLE
            if (currentVerificationStatus == STATUS_VERIFIED) {
                binding.buttonSubmitDiscomDetails.isEnabled = false
                binding.buttonSubmitDiscomDetails.alpha = 0.5f
                binding.buttonSubmitDiscomDetails.text = "Details Verified"
            } else {
                binding.buttonSubmitDiscomDetails.isEnabled = true
                binding.buttonSubmitDiscomDetails.alpha = 1.0f
                binding.buttonSubmitDiscomDetails.text = if (isEditMode) "Update Discom Details" else "Submit Discom Details"
            }
        }
    }

    private fun setupListeners() {
        binding.buttonUploadElectricBill.setOnClickListener { openPdfFilePicker() }
        binding.buttonSubmitDiscomDetails.setOnClickListener { handleSubmitDiscomDetails() }
        binding.tvSelectedBillFileName.setOnClickListener { viewPdfBill() }
        binding.ivElectricBillPreview.setOnClickListener { viewPdfBill() }
        binding.btnDeleteDiscomDetails.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun setupVerificationSection() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, VERIFICATION_STATUSES)
        binding.actVerificationStatusDiscom.setAdapter(statusAdapter)
        binding.buttonSubmitVerificationDiscom.setOnClickListener { handleSubmitDiscomVerification() }
    }

    private fun handleSubmitDiscomVerification() {
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

        if (currentDiscomId == null) {
            Toast.makeText(this, "Discom ID is missing. Cannot verify.", Toast.LENGTH_SHORT).show()
            return
        }

        val verificationStatus = binding.actVerificationStatusDiscom.text.toString()
        val verificationRemark = binding.etVerificationRemarkDiscom.text.toString().trim()

        if (verificationStatus.isEmpty() || !VERIFICATION_STATUSES.contains(verificationStatus.lowercase(Locale.ROOT))) {
            Toast.makeText(this, "Please select a valid verification status.", Toast.LENGTH_SHORT).show()
            binding.tilVerificationStatusDiscom.error = "Required"
            return
        }
        binding.tilVerificationStatusDiscom.error = null

        val request = DocumentVerificationRequest(
            verificationStatus = verificationStatus,
            verificationRemark = verificationRemark.takeIf { it.isNotEmpty() }
        )
        discomViewModel.verifyDiscomDocument(token, currentDiscomId!!, request)
    }

    private fun showDeleteConfirmationDialog() {
        if (currentDiscomId == null) {
            Toast.makeText(this, "Cannot delete. Discom details ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete these discom details? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val token = sessionManager.getUserToken()
                if (token != null && userRole.equals("admin", ignoreCase = true) && currentDiscomId != null) {
                    discomViewModel.deleteDiscomDetailById(token, currentDiscomId!!)
                } else if (token == null) {
                    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                } else if (!userRole.equals("admin", ignoreCase = true)){
                    Toast.makeText(this, "You do not have permission to delete discom details.", Toast.LENGTH_SHORT).show()
                } else {
                     Toast.makeText(this, "Cannot delete. Discom details ID is missing.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun viewPdfBill() {
        val pdfUriToView: Uri? = electricBillUri ?: existingBillRelativePath?.let { Uri.parse(BASE_STORAGE_URL + it) }
        val isRemotePdf = electricBillUri == null && existingBillRelativePath != null

        if (pdfUriToView != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUriToView, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                if (!isRemotePdf) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
             val chooser = Intent.createChooser(intent, "Open PDF with")
            try {
                startActivity(chooser)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No PDF viewer application found.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "No bill selected or available to view.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPdfPreviewPlaceholder(show: Boolean) {
        binding.ivElectricBillPreview.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun observeViewModel(){
        discomViewModel.createDiscomResult.observe(this) { result ->
            when(result){
                is DiscomDetailsResult.Loading -> showLoadingDialog("Submitting Discom Details...")
                is DiscomDetailsResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "Discom details saved!", Toast.LENGTH_LONG).show()
                    finish()
                }
                is DiscomDetailsResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        discomViewModel.updateDiscomResult.observe(this) { result ->
            when(result){
                is DiscomDetailsResult.Loading -> showLoadingDialog("Updating Discom Details...")
                is DiscomDetailsResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.response.message ?: "Discom details updated!", Toast.LENGTH_LONG).show()
                    finish()
                }
                is DiscomDetailsResult.Error -> {
                    hideLoadingDialog()
                    handleSubmissionError(result.errorMessage, result.errors)
                }
            }
        }

        discomViewModel.isFetchingDiscomDetail.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingDialog("Fetching Discom Details...")
            } else {
                 if (!isAnyOtherLoadingOperationActive()) hideLoadingDialog()
            }
        }

        discomViewModel.fetchedDiscomDetail.observe(this) { discomInfo ->
            if (discomInfo != null) {
                populateForm(discomInfo)
            } else {
                 if (currentDiscomId != null &&
                     discomViewModel.fetchDiscomDetailError.value == null && 
                     discomViewModel.isFetchingDiscomDetail.value == false &&
                     discomViewModel.deleteDiscomDetailResult.value !is DeleteDiscomDetailResult.Success &&
                     discomViewModel.verifyDiscomDetailResult.value !is VerifyDiscomDetailResult.Success) {
                    Toast.makeText(this, "Discom details not found. You can create new details.", Toast.LENGTH_LONG).show()
                    updateUiForCreateMode() 
                 }
            }
        }

        discomViewModel.fetchDiscomDetailError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Fetch Failed: $it", Toast.LENGTH_LONG).show()
                if(currentDiscomId != null) {
                     updateUiForCreateMode() 
                }
            }
        }

        discomViewModel.deleteDiscomDetailResult.observe(this) { result ->
            when(result) {
                is DeleteDiscomDetailResult.Loading -> showLoadingDialog("Deleting Discom Details...")
                is DeleteDiscomDetailResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is DeleteDiscomDetailResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Delete Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        discomViewModel.verifyDiscomDetailResult.observe(this) { result ->
            when(result) {
                is VerifyDiscomDetailResult.Loading -> showLoadingDialog("Submitting Discom Verification...")
                is VerifyDiscomDetailResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    currentDiscomId?.let { discomId ->
                        sessionManager.getUserToken()?.takeIf { it.isNotEmpty() }?.let { token ->
                            discomViewModel.fetchDiscomDetail(token, discomId)
                        }
                    } ?: finish()
                }
                is VerifyDiscomDetailResult.Error -> {
                    hideLoadingDialog()
                    Toast.makeText(this, "Discom Verification Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isAnyOtherLoadingOperationActive(): Boolean {
        return discomViewModel.createDiscomResult.value is DiscomDetailsResult.Loading ||
               discomViewModel.updateDiscomResult.value is DiscomDetailsResult.Loading ||
               discomViewModel.isFetchingDiscomDetail.value == true ||
               discomViewModel.deleteDiscomDetailResult.value is DeleteDiscomDetailResult.Loading ||
               discomViewModel.verifyDiscomDetailResult.value is VerifyDiscomDetailResult.Loading
    }

    private fun handleSubmissionError(errorMessage: String?, errors: Map<String, List<String>>?){
        val generalMessage = errorMessage ?: "An unknown error occurred."
        var specificErrorsText = ""

        errors?.forEach { (field, messages) ->
            val message = messages.joinToString(", ")
            when (field.lowercase(Locale.ROOT)) {
                "applicationid", "application_id" -> specificErrorsText += "App ID: $message\n"
                "discomname", "discom_name" -> { binding.tilDiscomName.error = message; specificErrorsText += "Discom Name: $message\n" }
                "consumernumber", "consumer_number" -> { binding.tilConsumerNumber.error = message; specificErrorsText += "Consumer No: $message\n" }
                "currentload", "current_load" -> { binding.tilCurrentLoad.error = message; specificErrorsText += "Current Load: $message\n" }
                "latestelectricbill", "latest_electric_bill" -> specificErrorsText += "Electric Bill: $message\n"
                else -> specificErrorsText += "${field.replaceFirstChar { it.titlecase(Locale.ROOT) }}: $message\n"
            }
        }
        val toastMessage = if (specificErrorsText.isNotEmpty()) "Submission Failed:\n${generalMessage}\n${specificErrorsText.trim()}" else "Submission Failed: $generalMessage"
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
        // Clear only general field errors, specific errors are set above
        if (errors == null || errors.isEmpty()) { // If no specific errors, clear all as a fallback
            clearAllFieldErrors()
        }
    }

    private fun clearAllFieldErrors(){
        binding.tilDiscomName.error = null
        binding.tilConsumerNumber.error = null
        binding.tilCurrentLoad.error = null
    }

    private fun openPdfFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            electricBillPickerLauncher.launch(Intent.createChooser(intent, "Select a PDF file"))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use {
            cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        return fileName ?: uri.lastPathSegment
    }

    private fun uriToDataUriString(uri: Uri): String? {
        return try {
            val mimeType = contentResolver.getType(uri)
            if (mimeType != "application/pdf") {
                Toast.makeText(this, "Invalid file type. Only PDF is allowed.", Toast.LENGTH_SHORT).show()
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

    private fun handleSubmitDiscomDetails() {
        val currentVerificationStatus = discomViewModel.fetchedDiscomDetail.value?.verificationStatus?.lowercase(Locale.ROOT)
        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)

        if (!binding.etDiscomName.isEnabled && !isPrivilegedUser) {
            Toast.makeText(this, "Details are ${currentVerificationStatus ?: "finalized"} and cannot be edited.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isPrivilegedUser && binding.verificationSectionDiscom.visibility == View.VISIBLE){
             Toast.makeText(this, "Please use the verification section to submit changes.", Toast.LENGTH_SHORT).show()
            return
        }

        clearAllFieldErrors()
        val discomName = binding.etDiscomName.text.toString().trim()
        val consumerNumber = binding.etConsumerNumber.text.toString().trim()
        val currentLoadStr = binding.etCurrentLoad.text.toString().trim()

        if (discomName.isEmpty()) {
            binding.tilDiscomName.error = "Discom name is required"; return
        }
        if (consumerNumber.isEmpty()) {
            binding.tilConsumerNumber.error = "Consumer number is required"; return
        }
        if (currentLoadStr.isEmpty()) {
            binding.tilCurrentLoad.error = "Current load is required"; return
        }
        val currentLoad = currentLoadStr.toDoubleOrNull()
        if (currentLoad == null || currentLoad <= 0) {
            binding.tilCurrentLoad.error = "Please enter a valid positive current load (e.g. 5.5)"; return
        }

        if (!isEditMode && electricBillUri == null) {
            Toast.makeText(this, "Please upload the latest electric bill (PDF).", Toast.LENGTH_SHORT).show()
            return
        }

        val electricBillBase64 = electricBillUri?.let { uriToDataUriString(it) }
        if (electricBillUri != null && electricBillBase64 == null) { 
            Toast.makeText(this, "Failed to process the selected electric bill PDF.", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show(); return
        }

        val currentAppId = applicationId ?: run {
            Toast.makeText(this, "Application ID missing.", Toast.LENGTH_LONG).show(); return
        }

        val discomDetailsRequest = DiscomDetailsRequest(
            applicationId = currentAppId,
            discomName = discomName,
            consumerNumber = consumerNumber,
            currentLoad = currentLoad,
            latestElectricBill = electricBillBase64
        )

        if (isEditMode && currentDiscomId != null) {
            discomViewModel.updateDiscomDetail(token, currentDiscomId!!, discomDetailsRequest)
        } else {
            discomViewModel.submitDiscomDetails(token, discomDetailsRequest)
        }
    }

    private fun showLoadingDialog(message: String = "Loading...") {
        if (loadingDialog?.dialog?.isShowing == true && loadingDialog?.arguments?.getString(LoadingDialogFragment.ARG_MESSAGE) == message) {
            return 
        }
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
