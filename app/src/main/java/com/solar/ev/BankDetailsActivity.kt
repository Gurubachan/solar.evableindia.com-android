package com.solar.ev

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.solar.ev.databinding.ActivityBankDetailsBinding
import com.solar.ev.model.bank.BankDetailsRequest
import com.solar.ev.model.bank.BankInfo
import com.solar.ev.model.common.DocumentVerificationRequest
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.bank.BankDetailsResult
import com.solar.ev.viewModel.bank.BankDetailsUpdateResult
import com.solar.ev.viewModel.bank.BankDetailsViewModel
import com.solar.ev.viewModel.bank.BankDetailsViewModelFactory
import com.solar.ev.viewModel.bank.DeleteBankDetailResult
import com.solar.ev.viewModel.bank.VerifyBankDetailResult
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class BankDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityBankDetailsBinding
    private lateinit var sessionManager: SessionManager
    private var applicationId: String? = null
    private var applicantName: String? = null
    private var currentBankId: String? = null
    private var isEditMode = false
    private var userRole: String? = null

    private var accountDocumentUri: Uri? = null
    private var selectedDocumentMimeType: String? = null
    private var existingDocumentPath: String? = null // Renamed from existingAccountPhotoRelativePath
    private var loadingDialog: LoadingDialogFragment? = null
    private var ifscLookupJob: Job? = null
    private val ifscDebounceTime = 1000L

    private val bankDetailsViewModel: BankDetailsViewModel by viewModels {
        BankDetailsViewModelFactory(RetrofitInstance.api, RetrofitInstance.razorpayApi)
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_APPLICANT_NAME = "EXTRA_APPLICANT_NAME"
        const val EXTRA_BANK_ID = "EXTRA_BANK_ID"
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
        private const val IFSC_CODE_LENGTH = 11
        private const val TAG = "BankDetailsActivity"
        private val VERIFICATION_STATUSES = arrayOf("pending", "verified", "rejected", "objection")
        private const val STATUS_VERIFIED = "verified"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_REJECTED = "rejected"
        private const val STATUS_OBJECTION = "objection"
    }

    private val documentPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let {
                uri ->
                accountDocumentUri = uri
                existingDocumentPath = null // Clear existing path if a new file is picked
                selectedDocumentMimeType = contentResolver.getType(uri)
                Log.d(TAG, "Selected file URI: $uri, MIME type: $selectedDocumentMimeType")

                if (selectedDocumentMimeType?.startsWith("image/") == true) {
                    Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.image_upload_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .into(binding.ivAccountDocumentPreview)
                } else if (selectedDocumentMimeType == "application/pdf") {
                    binding.ivAccountDocumentPreview.setImageResource(R.drawable.ic_pdf_placeholder) 
                } else {
                    // Handle other types or show a generic icon/error
                    binding.ivAccountDocumentPreview.setImageResource(R.drawable.ic_broken_image) // Or a generic file icon
                    Toast.makeText(this, "Unsupported file type: $selectedDocumentMimeType", Toast.LENGTH_LONG).show()
                }
                binding.ivAccountDocumentPreview.visibility = View.VISIBLE
                binding.ivAccountDocumentPreview.isClickable = true
                Toast.makeText(this, "Document selected: ${getFileName(uri)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getUserRole()
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        applicantName = intent.getStringExtra(EXTRA_APPLICANT_NAME)
        currentBankId = intent.getStringExtra(EXTRA_BANK_ID)

        binding.buttonUploadAccountDocument.text = "Upload Cheque/Passbook/Statement"

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing. Cannot proceed.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()
        setupVerificationSection()
        observeViewModel()
        setupIfscLookupListener()

        if (currentBankId?.isNotEmpty() == true) {
            val token = sessionManager.getUserToken()
            if (!token.isNullOrEmpty()) {
                bankDetailsViewModel.fetchBankDetail(token, currentBankId!!)
            } else {
                Toast.makeText(this, "Session expired. Cannot fetch bank details.", Toast.LENGTH_LONG).show()
                updateUiForCreateMode()
            }
        } else {
            updateUiForCreateMode()
        }
    }

    private fun setBankFormFieldsEnabled(enabled: Boolean) {
        binding.etAccountHolderName.isEnabled = enabled
        binding.etIfscCode.isEnabled = enabled
        binding.etAccountNumber.isEnabled = enabled
        binding.buttonUploadAccountDocument.isEnabled = enabled
        binding.buttonUploadAccountDocument.alpha = if (enabled) 1.0f else 0.5f

        binding.etBankName.isEnabled = enabled
        binding.etBranchName.isEnabled = enabled

        if (enabled) {
            if (binding.etIfscCode.text.isNullOrEmpty() || binding.etBankName.text.isNullOrEmpty()){
                binding.etBankName.isFocusable = true
                binding.etBankName.isFocusableInTouchMode = true
                binding.etBranchName.isFocusable = true
                binding.etBranchName.isFocusableInTouchMode = true
            } else { 
                 binding.etBankName.isFocusable = false
                 binding.etBankName.isFocusableInTouchMode = false
                 binding.etBranchName.isFocusable = false
                 binding.etBranchName.isFocusableInTouchMode = false
            }
        } else {
            binding.etBankName.isFocusable = false
            binding.etBankName.isFocusableInTouchMode = false
            binding.etBranchName.isFocusable = false
            binding.etBranchName.isFocusableInTouchMode = false
        }
    }

    private fun updateUiForCreateMode() {
        isEditMode = false
        supportActionBar?.title = "Add Bank Details"

        setBankFormFieldsEnabled(true)
        binding.llVerifiedStatusBank.visibility = View.GONE
        binding.verificationSectionBank.visibility = View.GONE
        
        binding.buttonSubmitBankDetails.text = "Submit Bank Details"
        binding.buttonSubmitBankDetails.visibility = View.VISIBLE
        binding.buttonSubmitBankDetails.isEnabled = true
        binding.buttonSubmitBankDetails.alpha = 1.0f

        applicantName?.let {
            binding.etAccountHolderName.setText(it)
        }
        binding.etIfscCode.setText("")
        binding.etBankName.setText("")
        binding.etBranchName.setText("")
        binding.etAccountNumber.setText("")
        binding.ivAccountDocumentPreview.visibility = View.GONE
        binding.ivAccountDocumentPreview.setImageDrawable(null)
        binding.ivAccountDocumentPreview.isClickable = false
        accountDocumentUri = null
        existingDocumentPath = null
        selectedDocumentMimeType = null

        binding.btnDeleteBankDetails.visibility = View.GONE
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarBankDetails)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getMimeTypeFromPath(filePath: String?): String? {
        if (filePath.isNullOrEmpty()) return null
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension?.lowercase(Locale.ROOT))
    }

    private fun populateForm(bankInfo: BankInfo) {
        isEditMode = true
        currentBankId = bankInfo.id
        supportActionBar?.title = "Edit Bank Details"

        binding.etAccountHolderName.setText(bankInfo.accountHolderName ?: applicantName ?: "")
        binding.etIfscCode.setText(bankInfo.ifscCode ?: "")
        binding.etBankName.setText(bankInfo.bankName ?: "")
        binding.etBranchName.setText(bankInfo.branchName ?: "")
        binding.etAccountNumber.setText(bankInfo.accountNumber ?: "")

        bankInfo.accountPhoto?.takeIf { it.isNotEmpty() }?.let {
            path ->
            existingDocumentPath = path
            accountDocumentUri = null // Clear picked URI if we are populating with existing path
            selectedDocumentMimeType = getMimeTypeFromPath(path)
            Log.d(TAG, "Existing document path: $path, MIME type: $selectedDocumentMimeType")

            if (selectedDocumentMimeType?.startsWith("image/") == true) {
                Glide.with(this)
                    .load(BASE_STORAGE_URL + path)
                    .placeholder(R.drawable.image_upload_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(binding.ivAccountDocumentPreview)
            } else if (selectedDocumentMimeType == "application/pdf") {
                binding.ivAccountDocumentPreview.setImageResource(R.drawable.ic_pdf_placeholder)
            } else {
                binding.ivAccountDocumentPreview.setImageResource(R.drawable.ic_broken_image) // Or a generic file icon
                 if(selectedDocumentMimeType != null) Toast.makeText(this, "Unsupported existing file type: $selectedDocumentMimeType", Toast.LENGTH_SHORT).show()
            }
            binding.ivAccountDocumentPreview.visibility = View.VISIBLE
            binding.ivAccountDocumentPreview.isClickable = true
        } ?: run {
            binding.ivAccountDocumentPreview.visibility = View.GONE
            binding.ivAccountDocumentPreview.setImageDrawable(null)
            binding.ivAccountDocumentPreview.isClickable = false
            existingDocumentPath = null
            selectedDocumentMimeType = null
        }

        if (userRole.equals("admin", ignoreCase = true) && currentBankId != null) {
            binding.btnDeleteBankDetails.visibility = View.VISIBLE
        } else {
            binding.btnDeleteBankDetails.visibility = View.GONE
        }

        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)
        val currentVerificationStatus = bankInfo.verificationStatus?.lowercase(Locale.ROOT) ?: STATUS_PENDING
        val verificationRemark = bankInfo.verificationRemark

        if (currentVerificationStatus.isNotEmpty() && currentVerificationStatus != STATUS_PENDING) {
            var statusMessage = "Status: ${currentVerificationStatus.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
            if (!verificationRemark.isNullOrEmpty()) {
                statusMessage += " - ${verificationRemark}"
            }
            binding.tvVerifiedStatusMessageBank.text = statusMessage
            binding.llVerifiedStatusBank.visibility = View.VISIBLE
            val colorResId = when (currentVerificationStatus) {
                STATUS_VERIFIED -> R.color.status_verified_color
                STATUS_REJECTED -> R.color.status_rejected_color
                STATUS_OBJECTION -> R.color.status_objection_color
                else -> R.color.status_default_color
            }
            binding.tvVerifiedStatusMessageBank.setTextColor(ContextCompat.getColor(this, colorResId))
        } else {
            binding.llVerifiedStatusBank.visibility = View.GONE
        }

        val canEditFields = currentVerificationStatus != STATUS_VERIFIED
        setBankFormFieldsEnabled(canEditFields)

        val showVerificationSection = isEditMode && isPrivilegedUser
        binding.verificationSectionBank.visibility = if (showVerificationSection) View.VISIBLE else View.GONE
        if (showVerificationSection) {
            binding.actVerificationStatusBank.setText(currentVerificationStatus, false)
            binding.etVerificationRemarkBank.setText(verificationRemark ?: "")
        }

        if (showVerificationSection) {
            binding.buttonSubmitBankDetails.visibility = View.GONE
        } else {
            binding.buttonSubmitBankDetails.visibility = View.VISIBLE
            if (currentVerificationStatus == STATUS_VERIFIED) {
                binding.buttonSubmitBankDetails.isEnabled = false
                binding.buttonSubmitBankDetails.alpha = 0.5f
                binding.buttonSubmitBankDetails.text = "Details Verified"
            } else {
                binding.buttonSubmitBankDetails.isEnabled = true
                binding.buttonSubmitBankDetails.alpha = 1.0f
                binding.buttonSubmitBankDetails.text = if (isEditMode) "Update Bank Details" else "Submit Bank Details"
            }
        }
    }

    private fun setupListeners() {
        binding.buttonUploadAccountDocument.setOnClickListener { openFilePicker(documentPickerLauncher) } // Renamed
        binding.buttonSubmitBankDetails.setOnClickListener { handleSubmitBankDetails() }
        binding.ivAccountDocumentPreview.setOnClickListener { viewDocument() } // Renamed
        binding.btnDeleteBankDetails.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun setupVerificationSection() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, VERIFICATION_STATUSES)
        binding.actVerificationStatusBank.setAdapter(statusAdapter)
        binding.buttonSubmitVerificationBank.setOnClickListener { handleSubmitVerificationBank() }
    }

    private fun handleSubmitVerificationBank() {
        val token = sessionManager.getUserToken()
        val role = sessionManager.getUserRole()
        if (token.isNullOrEmpty()) { Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show(); return }
        if (!(role.equals("supervisor", ignoreCase = true) || role.equals("back-office", ignoreCase = true))) { Toast.makeText(this, "You do not have permission to verify documents.", Toast.LENGTH_LONG).show(); return }
        if (currentBankId == null) { Toast.makeText(this, "Bank ID is missing. Cannot verify.", Toast.LENGTH_SHORT).show(); return }
        val verificationStatus = binding.actVerificationStatusBank.text.toString()
        val verificationRemark = binding.etVerificationRemarkBank.text.toString().trim()
        if (verificationStatus.isEmpty() || !VERIFICATION_STATUSES.contains(verificationStatus.lowercase(Locale.ROOT))) { Toast.makeText(this, "Please select a valid verification status.", Toast.LENGTH_SHORT).show(); binding.tilVerificationStatusBank.error = "Required"; return }
        binding.tilVerificationStatusBank.error = null
        val request = DocumentVerificationRequest(verificationStatus = verificationStatus, verificationRemark = verificationRemark.takeIf { it.isNotEmpty() })
        bankDetailsViewModel.verifyBankDetailDocument(token, currentBankId!!, request)
    }

    private fun showDeleteConfirmationDialog() {
        if (currentBankId == null) { Toast.makeText(this, "Cannot delete. Bank details ID is missing.", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete these bank details? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val token = sessionManager.getUserToken()
                if (token != null && userRole.equals("admin", ignoreCase = true) && currentBankId != null) {
                    bankDetailsViewModel.deleteBankDetailById(token, currentBankId!!)
                } else if (token == null) {
                    Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
                } else if (!userRole.equals("admin", ignoreCase = true)){
                    Toast.makeText(this, "You do not have permission to delete bank details.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Cannot delete. Bank details ID is missing.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun viewDocument() { // Renamed from viewAccountPhoto
        var documentUriToView: Uri? = null
        var documentMimeType: String? = null
        var isRemoteFile = false

        if (accountDocumentUri != null) { // A new file has been picked
            documentUriToView = accountDocumentUri
            documentMimeType = selectedDocumentMimeType
        } else if (isEditMode && existingDocumentPath != null) { // Viewing an existing remote file
            documentUriToView = Uri.parse(BASE_STORAGE_URL + existingDocumentPath)
            documentMimeType = getMimeTypeFromPath(existingDocumentPath)
            isRemoteFile = true
        }

        Log.d(TAG, "Viewing document. URI: $documentUriToView, MIME: $documentMimeType")

        if (documentUriToView != null && documentMimeType != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(documentUriToView, documentMimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (!isRemoteFile) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Open with")
            try {
                startActivity(chooser)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No application found to view this file type ($documentMimeType).", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "No document available to view.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupIfscLookupListener() {
        binding.etIfscCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!binding.etIfscCode.isEnabled) return 
                ifscLookupJob?.cancel()
                val ifsc = s.toString().trim().uppercase()
                if (ifsc.length == IFSC_CODE_LENGTH && isValidIfsc(ifsc)) {
                    binding.tilIfscCode.error = null
                    ifscLookupJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(ifscDebounceTime)
                        bankDetailsViewModel.fetchIfscDetails(ifsc)
                    }
                } else if (ifsc.isNotEmpty() && ifsc.length != IFSC_CODE_LENGTH) {
                    binding.tilBankName.editText?.setText("")
                    binding.tilBranchName.editText?.setText("")
                    if (binding.etBankName.isEnabled) { 
                        binding.etBankName.isFocusable = true
                        binding.etBankName.isFocusableInTouchMode = true
                        binding.etBranchName.isFocusable = true
                        binding.etBranchName.isFocusableInTouchMode = true
                    }
                } else if (ifsc.isNotEmpty() && !isValidIfsc(ifsc)) {
                    binding.tilIfscCode.error = "Invalid IFSC format"
                } else if (ifsc.isEmpty()){
                    binding.tilIfscCode.error = null
                    binding.tilBankName.editText?.setText("")
                    binding.tilBranchName.editText?.setText("")
                    if (binding.etBankName.isEnabled) { 
                        binding.etBankName.isFocusable = true
                        binding.etBankName.isFocusableInTouchMode = true
                        binding.etBranchName.isFocusable = true
                        binding.etBranchName.isFocusableInTouchMode = true
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        bankDetailsViewModel.bankDetailsResult.observe(this) { result ->
            when (result) {
                is BankDetailsResult.Loading -> showLoadingDialog("Submitting details...")
                is BankDetailsResult.Success -> { hideLoadingDialog(); Toast.makeText(this, result.response.message ?: "Bank details saved!", Toast.LENGTH_LONG).show(); finish() }
                is BankDetailsResult.Error -> { hideLoadingDialog(); handleSubmissionError(result.errorMessage, result.errors) }
            }
        }
        bankDetailsViewModel.bankUpdateResult.observe(this) { result ->
            when (result) {
                is BankDetailsUpdateResult.Loading -> showLoadingDialog("Updating details...")
                is BankDetailsUpdateResult.Success -> { hideLoadingDialog(); Toast.makeText(this, result.response.message ?: "Bank details updated!", Toast.LENGTH_LONG).show(); finish()  }
                is BankDetailsUpdateResult.Error -> { hideLoadingDialog(); handleSubmissionError(result.errorMessage, result.errors) }
            }
        }
        bankDetailsViewModel.isFetchingBankDetail.observe(this) { isLoading ->
            if (isLoading) { showLoadingDialog("Fetching bank details...") }
            else { if (!isAnyOtherLoadingOperationActive()) hideLoadingDialog() }
        }
        bankDetailsViewModel.bankDetail.observe(this) { details ->
            if (details != null) {
                populateForm(details)
            } else {
                val fetchAttempted = currentBankId?.isNotEmpty() == true
                val fetchConcluded = bankDetailsViewModel.isFetchingBankDetail.value == false
                val noFetchError = bankDetailsViewModel.fetchBankDetailError.value == null
                val notJustDeleted = bankDetailsViewModel.deleteBankDetailResult.value !is DeleteBankDetailResult.Success
                val notJustVerified = bankDetailsViewModel.verifyBankDetailResult.value !is VerifyBankDetailResult.Success
                if (fetchAttempted && fetchConcluded && noFetchError && notJustDeleted && notJustVerified) {
                    Toast.makeText(this, "No bank details found. Please fill form.", Toast.LENGTH_SHORT).show()
                    updateUiForCreateMode()
                }
            }
        }
        bankDetailsViewModel.fetchBankDetailError.observe(this) { error ->
            error?.let { Toast.makeText(this, "Fetch Failed: $it", Toast.LENGTH_LONG).show(); updateUiForCreateMode() }
        }
        bankDetailsViewModel.isFetchingIfsc.observe(this) { isLoading ->
            if (isLoading && binding.etIfscCode.isEnabled) { showLoadingDialog("Verifying IFSC...") }
            else { if (!isAnyOtherLoadingOperationActive()) hideLoadingDialog() }
        }
        bankDetailsViewModel.ifscDetails.observe(this) { ifscResponse ->
             if (!binding.etIfscCode.isEnabled) return@observe 
            if (ifscResponse != null) {
                binding.etBankName.setText(ifscResponse.bank ?: "")
                binding.etBranchName.setText(ifscResponse.branch ?: "")
                binding.tilIfscCode.error = null
                binding.etBankName.isFocusable = false; binding.etBankName.isFocusableInTouchMode = false
                binding.etBranchName.isFocusable = false; binding.etBranchName.isFocusableInTouchMode = false
            } else {
                 val ifscError = bankDetailsViewModel.ifscFetchError.value
                 if(ifscError == null || ifscError.isEmpty()){
                    if (binding.etBankName.isEnabled) { 
                        binding.etBankName.isFocusable = true; binding.etBankName.isFocusableInTouchMode = true
                        binding.etBranchName.isFocusable = true; binding.etBranchName.isFocusableInTouchMode = true
                    }
                 }
            }
        }
        bankDetailsViewModel.ifscFetchError.observe(this) { error ->
            if (!binding.etIfscCode.isEnabled) return@observe 
            error?.let {
                if (it.isNotEmpty()) {
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                    binding.tilIfscCode.error = it
                    binding.etBankName.setText(""); binding.etBranchName.setText("")
                    if (binding.etBankName.isEnabled) { 
                        binding.etBankName.isFocusable = true; binding.etBankName.isFocusableInTouchMode = true
                        binding.etBranchName.isFocusable = true; binding.etBranchName.isFocusableInTouchMode = true
                    }
                }
            }
        }
        bankDetailsViewModel.deleteBankDetailResult.observe(this) { result ->
            when (result) {
                is DeleteBankDetailResult.Loading -> showLoadingDialog("Deleting bank details...")
                is DeleteBankDetailResult.Success -> { hideLoadingDialog(); Toast.makeText(this, result.message, Toast.LENGTH_LONG).show(); finish() }
                is DeleteBankDetailResult.Error -> { hideLoadingDialog(); Toast.makeText(this, "Delete Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show() }
            }
        }
        bankDetailsViewModel.verifyBankDetailResult.observe(this) { result ->
            when (result) {
                is VerifyBankDetailResult.Loading -> showLoadingDialog("Submitting verification...")
                is VerifyBankDetailResult.Success -> {
                    hideLoadingDialog()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                    currentBankId?.let { bankId -> sessionManager.getUserToken()?.takeIf { it.isNotEmpty() }?.let { token -> bankDetailsViewModel.fetchBankDetail(token, bankId) } } ?: finish()
                }
                is VerifyBankDetailResult.Error -> { hideLoadingDialog(); Toast.makeText(this, "Verification Failed: ${result.errorMessage}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun isAnyOtherLoadingOperationActive(): Boolean {
        val isSubmitting = bankDetailsViewModel.bankDetailsResult.value is BankDetailsResult.Loading
        val isUpdating = bankDetailsViewModel.bankUpdateResult.value is BankDetailsUpdateResult.Loading
        val isFetchingAppBankDetails = bankDetailsViewModel.isFetchingBankDetail.value == true
        val isFetchingIfsc = bankDetailsViewModel.isFetchingIfsc.value == true && binding.etIfscCode.isEnabled
        val isDeleting = bankDetailsViewModel.deleteBankDetailResult.value is DeleteBankDetailResult.Loading
        val isVerifying = bankDetailsViewModel.verifyBankDetailResult.value is VerifyBankDetailResult.Loading
        return isSubmitting || isUpdating || isFetchingAppBankDetails || isFetchingIfsc || isDeleting || isVerifying
    }

    private fun openFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { 
            type = "*/*" // Changed to */* to properly support EXTRA_MIME_TYPES
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
        }
        try {
            launcher.launch(Intent.createChooser(intent, "Select Document (Image or PDF)"))
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

    // uriToDataUriString already correctly handles PDF if contentResolver.getType(uri) returns "application/pdf"
    // as it checks: if (mimeType !in listOf("image/jpeg", "image/png", "application/pdf"))
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
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error converting URI to Base64 data URI string", e)
            Toast.makeText(this, "Failed to read file data for ${getFileName(uri)}.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun handleSubmitBankDetails() {
        val currentVerificationStatus = bankDetailsViewModel.bankDetail.value?.verificationStatus?.lowercase(Locale.ROOT)
        val isPrivilegedUser = userRole.equals("supervisor", ignoreCase = true) || userRole.equals("back-office", ignoreCase = true)

        if (!binding.etAccountHolderName.isEnabled && !isPrivilegedUser) {
            Toast.makeText(this, "Details are ${currentVerificationStatus ?: "finalized"} and cannot be edited.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isPrivilegedUser && binding.verificationSectionBank.visibility == View.VISIBLE){
            Toast.makeText(this, "Please use the verification section to submit changes.", Toast.LENGTH_SHORT).show()
            return
        }

        val accountHolderName = binding.etAccountHolderName.text.toString().trim()
        val bankName = binding.etBankName.text.toString().trim()
        val ifscCode = binding.etIfscCode.text.toString().trim().uppercase()
        val branchName = binding.etBranchName.text.toString().trim()
        val accountNumber = binding.etAccountNumber.text.toString().trim()

        if (accountHolderName.isEmpty()) { binding.tilAccountHolderName.error = "Required"; return }
        binding.tilAccountHolderName.error = null
        if (ifscCode.isEmpty()) { binding.tilIfscCode.error = "Required"; return }
        else if (!isValidIfsc(ifscCode)) { binding.tilIfscCode.error = "Invalid IFSC format"; return }
        binding.tilIfscCode.error = null

        if (bankName.isEmpty()) { binding.tilBankName.error = "Bank Name is Required (auto-filled via IFSC if valid)"; return }
        binding.tilBankName.error = null
        if (branchName.isEmpty()) { binding.tilBranchName.error = "Branch Name is Required (auto-filled via IFSC if valid)"; return }
        binding.tilBranchName.error = null

        if (accountNumber.isEmpty()) { binding.tilAccountNumber.error = "Required"; return }
        if (!accountNumber.matches(Regex("^\\d{9,18}$"))){
            binding.tilAccountNumber.error = "Account number must be 9-18 digits."
            return
        }
        binding.tilAccountNumber.error = null

        if (!isEditMode && accountDocumentUri == null) {
            Toast.makeText(this, "Please upload account document (Cheque/Passbook/Statement).", Toast.LENGTH_SHORT).show()
            return
        }

        val accountDocumentBase64 = accountDocumentUri?.let { uriToDataUriString(it) }
        if (accountDocumentUri != null && accountDocumentBase64 == null) {
            // uriToDataUriString would have shown a toast for unsupported type or read error
            return
        }

        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) { Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show(); return }

        val currentAppId = applicationId ?: run { Toast.makeText(this, "Application ID missing.", Toast.LENGTH_LONG).show(); return }

        val bankDetailsRequest = BankDetailsRequest(
            applicationId = currentAppId,
            accountHolderName = accountHolderName,
            bankName = bankName,
            ifscCode = ifscCode,
            branchName = branchName,
            accountNumber = accountNumber,
            accountPhoto = accountDocumentBase64 // Changed from accountPhotoBase64
        )

        if (isEditMode && currentBankId != null) {
            bankDetailsViewModel.updateBankDetail(token, currentBankId!!, bankDetailsRequest)
        } else {
            bankDetailsViewModel.submitBankDetails(token, bankDetailsRequest)
        }
    }
    private fun handleSubmissionError(errorMessage: String?, errors: Map<String, List<String>>?) {
        val generalMessage = errorMessage ?: "An unknown error occurred."
        var specificErrorsText = ""
        errors?.forEach { (field, messages) ->
            val message = messages.joinToString(", ")
            when (field.lowercase(Locale.ROOT)) {
                "applicationid", "application_id" -> specificErrorsText += "App ID: $message\n"
                "accountholdername", "account_holder_name" -> { binding.tilAccountHolderName.error = message; specificErrorsText += "Account Holder: $message\n" }
                "bankname", "bank_name" -> { binding.tilBankName.error = message; specificErrorsText += "Bank Name: $message\n" }
                "ifsccode", "ifsc_code" -> { binding.tilIfscCode.error = message; specificErrorsText += "IFSC: $message\n" }
                "branchname", "branch_name" -> { binding.tilBranchName.error = message; specificErrorsText += "Branch: $message\n" }
                "accountnumber", "account_number" -> { binding.tilAccountNumber.error = message; specificErrorsText += "Account No: $message\n" }
                "accountphoto", "account_photo" -> specificErrorsText += "Account Document: $message\n"
                else -> specificErrorsText += "${field.replaceFirstChar { it.titlecase(Locale.ROOT) }}: $message\n"
            }
        }
        val toastMessage = if (specificErrorsText.isNotEmpty()) "Submission Failed:\n${generalMessage}\n${specificErrorsText.trim()}" else "Submission Failed: $generalMessage"
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
    }

    private fun isValidIfsc(ifsc: String): Boolean {
        val pattern = "^[A-Z]{4}0[A-Z0-9]{6}$".toRegex()
        return pattern.matches(ifsc)
    }

    private fun showLoadingDialog(message: String = "Loading...") {
        if (loadingDialog?.dialog?.isShowing == true && loadingDialog?.arguments?.getString(LoadingDialogFragment.ARG_MESSAGE) == message) return
        hideLoadingDialog()
        loadingDialog = LoadingDialogFragment.newInstance(message)
        if (!isFinishing && !isDestroyed) {
            try { loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG) }
            catch (e: IllegalStateException) { Log.e(TAG, "Error showing dialog", e) }
        }
    }

    private fun hideLoadingDialog() {
        if (loadingDialog?.dialog?.isShowing == true) {
            if (!isFinishing && !isDestroyed) {
                try { loadingDialog?.dismissAllowingStateLoss() }
                catch (e: IllegalStateException) { Log.e(TAG, "Error dismissing dialog", e) }
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
        ifscLookupJob?.cancel()
        hideLoadingDialog()
    }
}
