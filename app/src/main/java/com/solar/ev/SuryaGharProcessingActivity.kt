package com.solar.ev

import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.solar.ev.adapter.QuotationAdapter
import com.solar.ev.databinding.ActivitySuryaGharProcessingBinding
import com.solar.ev.model.application.ApplicationDetailResponse
import com.solar.ev.model.quotation.QuotationListItem
import com.solar.ev.model.suryaghar.*
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.ui.common.LoadingDialogFragment
import com.solar.ev.viewModel.suryaghar.SuryaGharApiResult
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModel
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SuryaGharProcessingActivity : BaseActivity() {

    private lateinit var binding: ActivitySuryaGharProcessingBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: SuryaGharViewModel by viewModels {
        SuryaGharViewModelFactory(RetrofitInstance.api)
    }

    private var isEditMode: Boolean = false
    private var applicationId: String? = null
    private var projectProcessId: String? = null
    private var applicantNameIntent: String? = null
    private var userRole: String? = null

    private var loadingDialog: LoadingDialogFragment? = null
    // Atomic counter to manage concurrent loading states for initial data fetch
    private val loadingRequestCounter = AtomicInteger(0)

    // Document handling variables
    private var acknowledgementUri: Uri? = null
    private var feasibilityUri: Uri? = null
    private var nmaUri: Uri? = null
    private var etokenUri: Uri? = null
    private var acknowledgementBase64: String? = null
    private var feasibilityBase64: String? = null
    private var nmaBase64: String? = null
    private var etokenBase64: String? = null
    private var existingAcknowledgementPath: String? = null
    private var existingFeasibilityPath: String? = null
    private var existingNmaPath: String? = null
    private var existingEtokenPath: String? = null
    private var currentPickingDocType: Int = 0

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    private val uiDateFormat = SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val EXTRA_APPLICANT_NAME = "EXTRA_APPLICANT_NAME"
        private const val TAG = "SuryaGharActivity"
        private const val BASE_STORAGE_URL = "https://solar.evableindia.com/core/public/storage/"
        private const val DOC_TYPE_ACKNOWLEDGEMENT = 1
        private const val DOC_TYPE_FEASIBILITY = 2
        private const val DOC_TYPE_NMA = 3
        private const val DOC_TYPE_ETOKEN = 4
        private val STATUS_OPTIONS = arrayOf("applied", "processed", "pending_approval", "approved", "rejected", "on_hold", "completed", "cancelled")
    }

    private val documentPickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            it.data?.data?.let { uri -> handleDocumentPicked(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuryaGharProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getUserRole()
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        applicantNameIntent = intent.getStringExtra(EXTRA_APPLICANT_NAME)

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.tvApplicationIdDisplay.text = "Application ID: $applicationId"

        setupToolbar()
        setupUI()
        setupListeners()
        observeViewModel()

        loadInitialData()

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadInitialData() {
        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session Expired.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.getProjectProcessById(token, applicationId!!)
        viewModel.getApplicationDetails(token, applicationId!!)
    }

    private fun populateForm(data: ProjectProcessData) {
        isEditMode = true
        projectProcessId = data.id.toString()
        binding.etProjectReferenceId.setText(data.projectReferenceId)

        try {
            apiDateFormat.parse(data.appliedDate)?.let { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                updateDateInView(calendar, binding.etAppliedDate)
            } ?: binding.etAppliedDate.setText(data.appliedDate)
        } catch (e: ParseException) {
            Log.e(TAG, "Failed to parse date from API: ${data.appliedDate}", e)
            binding.etAppliedDate.setText(data.appliedDate)
        }

        binding.actStatus.setText(data.status, false)
        binding.etRemark.setText(data.remark)

        if (data.status == "applied" && (userRole == "admin" || userRole == "back-office")) {
            binding.quotationsCard.visibility = View.VISIBLE
        } else {
            binding.quotationsCard.visibility = View.GONE
        }

        existingAcknowledgementPath = data.acknowledgement
        existingFeasibilityPath = data.feasibility
        existingNmaPath = data.nma
        existingEtokenPath = data.etoken

        updatePreview(binding.ivAcknowledgementPreview, null, getMimeTypeFromPath(data.acknowledgement), data.acknowledgement)
        updatePreview(binding.ivFeasibilityPreview, null, getMimeTypeFromPath(data.feasibility), data.feasibility)
        updatePreview(binding.ivNmaPreview, null, getMimeTypeFromPath(data.nma), data.nma)
        updatePreview(binding.ivEtokenPreview, null, getMimeTypeFromPath(data.etoken), data.etoken)

        if (userRole == "admin" || userRole == "subadmin") {
            binding.verificationSectionSuryaGhar.visibility = View.VISIBLE
            binding.actVerificationStatusSuryaGhar.setText(data.status, false)
            binding.etVerificationRemarkSuryaGhar.setText(data.remark)
        } else {
            binding.verificationSectionSuryaGhar.visibility = View.GONE
        }
        binding.btnDeleteProjectProcess.visibility = if (isEditMode) View.VISIBLE else View.GONE
        setupToolbar()
    }

    private fun populateApplicationDetails(data: ApplicationDetailResponse) {
        val applicant = data.data?.application
        if (applicant != null) {
            applicantNameIntent = applicant.name
            binding.tvApplicantName.text = "Applicant Name: ${applicant.name}"
            binding.tvApplicantMobile.text = "Mobile: ${applicant.contactNumber}"
            binding.tvApplicantEmail.text = "Email: ${applicant.email}"
            binding.tvApplicantAddress.text = "Address: ${applicant.address}"
            binding.tvApplicantDob.text = "Date of Birth: ${applicant.dob}"
            binding.tvApplicantGender.text = "Gender: ${applicant.gender}"
            setupToolbar()
        }

        val kyc = data.data?.application?.kyc
        if (kyc != null) {
            binding.tvAadharNumber.text = "Aadhar Number: ${kyc.aadhaarNumber}"
            binding.tvPanNumber.text = "PAN Number: ${kyc.panNumber}"
            binding.btnDownloadAadharFront.setOnClickListener { kyc.aadhaarPhotoFront?.let { it1 -> downloadFile(it1) } }
            binding.btnDownloadAadharBack.setOnClickListener { kyc.aadhaarPhotoBack?.let { it1 -> downloadFile(it1) } }
            binding.btnDownloadPan.setOnClickListener { kyc.panPhoto?.let { it1 -> downloadFile(it1) } }
        }

        val bank = data.data?.application?.bank
        if (bank != null) {
            binding.tvBankName.text = "Bank Name: ${bank.bankName}"
            binding.tvBankAccountNumber.text = "Account Number: ${bank.accountNumber}"
            binding.tvBankIfsc.text = "IFSC Code: ${bank.ifscCode}"
            binding.tvBankBranch.text = "Branch: ${bank.branchName}"
            binding.btnDownloadPassbook.setOnClickListener { bank.accountPhoto?.let { it1 -> downloadFile(it1) } }
        }

        val discom = data.data?.application?.discom
        if (discom != null) {
            binding.tvDiscomName.text = "Discom Name: ${discom.discomName}"
            binding.tvDiscomAccountNumber.text = "Account Number: ${discom.consumerNumber}"
            binding.btnDownloadElectricBill.setOnClickListener { discom.latestElectricBill?.let { it1 -> downloadFile(it1) } }
        }

        val installation = data.data?.application?.installation
        if (installation != null) {
            binding.tvLatitude.text = "Latitude: ${installation.latitude}"
            binding.tvLongitude.text = "Longitude: ${installation.longitude}"
            binding.tvLoadRequirement.text = "Load Requirement: ${installation.capacityKw}"
            binding.btnDownloadInstallationPhoto.setOnClickListener { installation.installationLocation?.let { it1 -> downloadFile(it1) } }
        }
    }

    private fun downloadFile(path: String) {
        if (path.isBlank()) {
            Toast.makeText(this, "File path is invalid.", Toast.LENGTH_SHORT).show()
            return
        }

        val fullUrl = BASE_STORAGE_URL + path
        val request = DownloadManager.Request(Uri.parse(fullUrl))
            .setTitle(path.substringAfterLast('/'))
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, path.substringAfterLast('/'))

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            downloadManager.enqueue(request)
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
            Toast.makeText(this, "Failed to start download.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarSuryaGharProcessing)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        val toolbarTitle = if (!applicantNameIntent.isNullOrBlank()) applicantNameIntent else "Surya Ghar Process"
        val processType = if (isEditMode) "Edit Details" else "Create New"
        supportActionBar?.title = toolbarTitle
        supportActionBar?.subtitle = "Surya Ghar App ID: $applicationId ($processType)"
    }

    private fun setupUI() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, STATUS_OPTIONS)
        binding.actStatus.setAdapter(statusAdapter)
        binding.actVerificationStatusSuryaGhar.setAdapter(statusAdapter)
        if (!isEditMode) {
            setDefaultAppliedDate()
            binding.btnDeleteProjectProcess.visibility = View.GONE
            binding.verificationSectionSuryaGhar.visibility = View.GONE
        }
    }

    private fun setDefaultAppliedDate() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        updateDateInView(calendar, binding.etAppliedDate)
    }

    private fun updateDateInView(calendar: Calendar, editText: TextInputEditText) {
        editText.setText(uiDateFormat.format(calendar.time))
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        try {
            binding.etAppliedDate.text.toString().takeIf { it.isNotEmpty() }?.let {
                uiDateFormat.parse(it)?.let { date -> calendar.time = date }
            }
        } catch (e: ParseException) {
            Log.e(TAG, "Error parsing date from UI for DatePickerDialog: ${e.message}")
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, monthOfYear)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val timeCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            try {
                binding.etAppliedDate.text.toString().takeIf { it.isNotEmpty() }?.let {
                    uiDateFormat.parse(it)?.let { date -> timeCalendar.time = date }
                }
            } catch (e: Exception) { /* Falls back to current time */ }

            selectedCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            selectedCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            selectedCalendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
            updateDateInView(selectedCalendar, binding.etAppliedDate)
        }

        DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setupListeners() {
        binding.etAppliedDate.setOnClickListener { showDatePickerDialog() }
        binding.btnUploadAcknowledgement.setOnClickListener { openFilePicker(DOC_TYPE_ACKNOWLEDGEMENT) }
        binding.btnUploadFeasibility.setOnClickListener { openFilePicker(DOC_TYPE_FEASIBILITY) }
        binding.btnUploadNma.setOnClickListener { openFilePicker(DOC_TYPE_NMA) }
        binding.btnUploadEtoken.setOnClickListener { openFilePicker(DOC_TYPE_ETOKEN) }

        binding.ivAcknowledgementPreview.setOnClickListener { viewDocument(acknowledgementUri, existingAcknowledgementPath) }
        binding.ivFeasibilityPreview.setOnClickListener { viewDocument(feasibilityUri, existingFeasibilityPath) }
        binding.ivNmaPreview.setOnClickListener { viewDocument(nmaUri, existingNmaPath) }
        binding.ivEtokenPreview.setOnClickListener { viewDocument(etokenUri, existingEtokenPath) }

        binding.btnSubmitProjectProcess.setOnClickListener { handleSubmitData() }
        binding.btnDeleteProjectProcess.setOnClickListener { handleDeleteProjectProcess() }
        binding.btnSubmitVerificationSuryaGhar.setOnClickListener { handleSubmitVerification() }

        binding.btnViewQuotations.setOnClickListener {
            val intent = Intent(this, QuotationActivity::class.java)
            intent.putExtra(QuotationActivity.EXTRA_APPLICATION_ID, applicationId)
            startActivity(intent)
        }
    }

    private fun openFilePicker(docType: Int) {
        currentPickingDocType = docType
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "application/pdf"))
        }
        try {
            documentPickerLauncher.launch(Intent.createChooser(intent, "Select Document (Image or PDF)"))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDocumentPicked(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        val fileName = getFileName(uri)

        if (mimeType !in listOf("image/jpeg", "image/png", "application/pdf")) {
            Toast.makeText(this, "Unsupported file type: $mimeType. Please select JPG, PNG, or PDF.", Toast.LENGTH_LONG).show()
            return
        }

        uriToBase64String(uri) { base64String ->
            if (base64String == null) {
                Toast.makeText(this, "Failed to process file: $fileName", Toast.LENGTH_SHORT).show()
                return@uriToBase64String
            }

            when (currentPickingDocType) {
                DOC_TYPE_ACKNOWLEDGEMENT -> {
                    acknowledgementUri = uri
                    acknowledgementBase64 = base64String
                    existingAcknowledgementPath = null
                    updatePreview(binding.ivAcknowledgementPreview, uri, mimeType)
                }
                DOC_TYPE_FEASIBILITY -> {
                    feasibilityUri = uri
                    feasibilityBase64 = base64String
                    existingFeasibilityPath = null
                    updatePreview(binding.ivFeasibilityPreview, uri, mimeType)
                }
                DOC_TYPE_NMA -> {
                    nmaUri = uri
                    nmaBase64 = base64String
                    existingNmaPath = null
                    updatePreview(binding.ivNmaPreview, uri, mimeType)
                }
                DOC_TYPE_ETOKEN -> {
                    etokenUri = uri
                    etokenBase64 = base64String
                    existingEtokenPath = null
                    updatePreview(binding.ivEtokenPreview, uri, mimeType)
                }
            }
            Toast.makeText(this, "$fileName selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePreview(imageView: ImageView, uri: Uri?, mimeType: String?, remotePath: String? = null) {
        imageView.visibility = View.VISIBLE
        imageView.isClickable = true
        if (uri != null) {
            if (mimeType?.startsWith("image/") == true) {
                Glide.with(this).load(uri).placeholder(R.drawable.image_upload_placeholder).error(R.drawable.ic_broken_image).into(imageView)
            } else if (mimeType == "application/pdf") {
                imageView.setImageResource(R.drawable.ic_pdf_placeholder)
            } else {
                imageView.setImageResource(R.drawable.ic_broken_image)
            }
        } else if (!remotePath.isNullOrEmpty()) {
            val fullUrl = BASE_STORAGE_URL + remotePath
            val inferredMimeType = getMimeTypeFromPath(remotePath)
            if (inferredMimeType?.startsWith("image/") == true) {
                Glide.with(this).load(fullUrl).placeholder(R.drawable.image_upload_placeholder).error(R.drawable.ic_broken_image).into(imageView)
            } else if (inferredMimeType == "application/pdf") {
                imageView.setImageResource(R.drawable.ic_pdf_placeholder)
            } else {
                imageView.setImageResource(R.drawable.ic_document_placeholder)
            }
        } else {
            imageView.setImageResource(R.drawable.image_upload_placeholder)
            imageView.visibility = View.GONE
            imageView.isClickable = false
        }
    }

    private fun getMimeTypeFromPath(filePath: String?): String? {
        if (filePath.isNullOrEmpty()) return null
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath.lowercase(Locale.ROOT))
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun uriToBase64String(uri: Uri, callback: (String?) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                val mimeType = contentResolver.getType(uri)
                if (mimeType !in listOf("image/jpeg", "image/png", "application/pdf")) {
                    launch(Dispatchers.Main) { callback(null) }
                    return@launch
                }
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUri = "data:$mimeType;base64,$base64String"
                    launch(Dispatchers.Main) { callback(dataUri) }
                } ?: launch(Dispatchers.Main) { callback(null) }
            } catch (e: IOException) {
                Log.e(TAG, "Error converting URI to Base64: ${e.message}", e)
                launch(Dispatchers.Main) { callback(null) }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                fileName = fileName?.substring(cut + 1)
            }
        }
        return fileName
    }

    private fun viewDocument(localUri: Uri?, remotePath: String?) {
        val uriToView: Uri? = localUri ?: remotePath?.let { Uri.parse(BASE_STORAGE_URL + it) }
        if (uriToView == null) {
            Toast.makeText(this, "No document to view.", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = if (localUri != null) contentResolver.getType(localUri) else getMimeTypeFromPath(remotePath)
        if (mimeType != null && mimeType.startsWith("image/")) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToView, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No application can handle this image.", Toast.LENGTH_SHORT).show()
            }
        } else {
            downloadAndOpenFile(uriToView.toString())
        }
    }

    private fun downloadAndOpenFile(url: String) {
        val path = try { Uri.parse(url).path ?: "downloadfile" } catch (e: Exception) { "downloadfile" }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(path.split("/").lastOrNull() ?: "Downloading file")
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, path.split("/").last())
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun clearFormErrors() {
        binding.tilProjectReferenceId.error = null
        binding.tilAppliedDate.error = null
        binding.tilStatus.error = null
        binding.tilRemark.error = null
        binding.tilVerificationStatusSuryaGhar.error = null
        binding.tilVerificationRemarkSuryaGhar.error = null
    }

    private fun handleSubmitData() {
        clearFormErrors()
        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        val projectRefId = binding.etProjectReferenceId.text.toString().trim().takeIf { it.isNotEmpty() }
        val appliedDateString = binding.etAppliedDate.text.toString().trim()
        val status = binding.actStatus.text.toString().trim()
        val remark = binding.etRemark.text.toString().trim().takeIf { it.isNotEmpty() }

        if (appliedDateString.isEmpty()) {
            binding.tilAppliedDate.error = "Applied date is required."; return
        }
        if (status.isEmpty()) {
            binding.tilStatus.error = "Status is required."; return
        }

        if (isEditMode) {
            val request = ProjectProcessUpdateRequest(
                projectReferenceId = projectRefId,
                appliedDate = appliedDateString,
                status = status,
                remark = remark,
                acknowledgement = acknowledgementBase64,
                feasibility = feasibilityBase64,
                nma = nmaBase64,
                etoken = etokenBase64
            )
            viewModel.updateProjectProcess(token, projectProcessId!!, request)
        } else {
            val appliedBy = sessionManager.getUserId()
            if (appliedBy == null) {
                Toast.makeText(this, "User ID not found. Cannot create process.", Toast.LENGTH_SHORT).show()
                return
            }
            val request = ProjectProcessCreateRequest(
                applicationId = applicationId!!,
                projectReferenceId = projectRefId,
                appliedDate = appliedDateString,
                appliedBy = appliedBy,
                status = status,
                remark = remark,
                acknowledgement = acknowledgementBase64,
                feasibility = feasibilityBase64,
                nma = nmaBase64,
                etoken = etokenBase64
            )
            viewModel.createProjectProcess(token, request)
        }
    }

    private fun handleDeleteProjectProcess() {
        projectProcessId?.let {
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this project process record?")
                .setPositiveButton("Delete") { _, _ ->
                    clearFormErrors()
                    val token = sessionManager.getUserToken()
                    if (token != null) {
                        viewModel.deleteProjectProcess(token, it)
                    } else {
                        Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun handleSubmitVerification() {
        projectProcessId?.let { id ->
            clearFormErrors()
            val token = sessionManager.getUserToken()
            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show(); return
            }
            val status = binding.actVerificationStatusSuryaGhar.text.toString()
            val remark = binding.etVerificationRemarkSuryaGhar.text.toString().trim().takeIf { it.isNotEmpty() }

            if (status.isEmpty()) {
                binding.tilVerificationStatusSuryaGhar.error = "Status is required for verification."; return
            }
            val request = ProjectProcessStatusUpdateRequest(status = status, remark = remark)
            viewModel.updateProjectProcessStatus(token, id, request)
        }
    }

    private fun observeViewModel() {
        // --- OBSERVERS FOR INITIAL DATA ---
        viewModel.detailResult.observe(this) { result ->
            handleInitialDataApiResponse(result, "Fetching Details...") { response ->
                populateForm(response.data!!)
            }
        }

        viewModel.applicationDetailResult.observe(this) { result ->
            handleInitialDataApiResponse(result, "Fetching Application Details...") { response ->
                populateApplicationDetails(response)
            }
        }

        // --- OBSERVERS FOR USER ACTIONS (Create, Update, Delete) ---
        viewModel.createResult.observe(this) { result ->
            handleActionApiResponse(result, "Submitting data...") { response ->
                Toast.makeText(this, response.message ?: "Created successfully", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            handleActionApiResponse(result, "Updating data...") { response ->
                Toast.makeText(this, response.message ?: "Updated successfully", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            handleActionApiResponse(result, "Deleting record...") { response ->
                Toast.makeText(this, response.message ?: "Deleted successfully", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        viewModel.statusUpdateResult.observe(this) { result ->
            handleActionApiResponse(result, "Updating status...") { response ->
                Toast.makeText(this, response.message ?: "Status updated", Toast.LENGTH_LONG).show()
                response.data?.let { populateForm(it) }
            }
        }
    }

    private fun <T> handleInitialDataApiResponse(result: SuryaGharApiResult<T>, loadingMessage: String, onSuccess: (T) -> Unit) {
        when (result) {
            is SuryaGharApiResult.Loading -> incrementLoadingCounter(loadingMessage)
            is SuryaGharApiResult.Success -> {
                decrementLoadingCounter()
                val responseData = result.data
                if (getStatusFromResult(responseData) == true && getDataFromResult(responseData) != null) {
                    onSuccess(responseData)
                } else {
                    Log.i(TAG, "Initial data fetch logical error for : ${getMessageFromResult(responseData)}")
                }
            }
            is SuryaGharApiResult.Error -> {
                decrementLoadingCounter()
                if (result.isNotFound()) {
                    Log.i(TAG, "Initial data fetch returned 404 . Assuming create mode.")
                } else {
                    handleApiError(result.message, result.errors, shouldFinish = true)
                }
            }
        }
    }

    private fun <T> handleActionApiResponse(result: SuryaGharApiResult<T>, loadingMessage: String, onSuccess: (T) -> Unit) {
        when (result) {
            is SuryaGharApiResult.Loading -> showLoading(loadingMessage)
            is SuryaGharApiResult.Success -> {
                hideLoading()
                if (getStatusFromResult(result.data) == true) {
                    onSuccess(result.data)
                } else {
                    handleApiError(getMessageFromResult(result.data) ?: "Action failed", getErrorsFromResult(result.data))
                }
            }
            is SuryaGharApiResult.Error -> {
                handleApiError(result.message, result.errors)
            }
        }
    }

    private fun handleApiError(message: String, errors: Map<String, List<String>>?, shouldFinish: Boolean = false) {
        hideLoading()
        try {
            clearFormErrors()
            val unhandledErrorMessages = StringBuilder()
            errors?.forEach { (field, messages) ->
                val errorMsg = messages.joinToString(", ")
                when (field.lowercase(Locale.ROOT)) {
                    "project_reference_id" -> binding.tilProjectReferenceId.error = errorMsg
                    "applied_date" -> binding.tilAppliedDate.error = errorMsg
                    "status" -> {
                        if (binding.verificationSectionSuryaGhar.visibility == View.VISIBLE && binding.tilVerificationStatusSuryaGhar.isFocusable) {
                            binding.tilVerificationStatusSuryaGhar.error = errorMsg
                        } else {
                            binding.tilStatus.error = errorMsg
                        }
                    }
                    "remark" -> {
                        if (binding.verificationSectionSuryaGhar.visibility == View.VISIBLE && binding.tilVerificationRemarkSuryaGhar.isFocusable) {
                            binding.tilVerificationRemarkSuryaGhar.error = errorMsg
                        } else {
                            binding.tilRemark.error = errorMsg
                        }
                    }
                    else -> unhandledErrorMessages.append("${field.replaceFirstChar { it.titlecase(Locale.getDefault()) }}: $errorMsg\n")
                }
            }

            val displayMessage = StringBuilder(message)
            if (unhandledErrorMessages.isNotEmpty()) {
                displayMessage.append("\n\nSpecific Issues:\n$unhandledErrorMessages")
            }

            Toast.makeText(this, displayMessage.toString().trim(), Toast.LENGTH_LONG).show()
            Log.e(TAG, "API Error: $message, Details: ${errors?.toString()}")


        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred within handleApiError", e)
            Toast.makeText(this, "An unexpected error occurred. Check logs for details.", Toast.LENGTH_LONG).show()
            
        }
    }

    private fun incrementLoadingCounter(message: String) {
        if (loadingRequestCounter.getAndIncrement() == 0) {
            showLoading(message)
        }
    }

    private fun decrementLoadingCounter() {
        if (loadingRequestCounter.decrementAndGet() == 0) {
            hideLoading()
        }
    }

    private fun showLoading(message: String = "Loading...") {
        if (loadingDialog?.dialog?.isShowing == true) {
            loadingDialog?.updateMessage(message)
            return
        }
        hideLoading() // Dismiss any existing dialog before showing a new one
        loadingDialog = LoadingDialogFragment.newInstance(message)
        if (!isFinishing && !isDestroyed) {
            try {
                loadingDialog?.show(supportFragmentManager, LoadingDialogFragment.TAG)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error showing loading dialog", e)
            }
        }
    }

    private fun hideLoading() {
        if (loadingDialog?.dialog?.isShowing == true) {
            if (!isFinishing && !isDestroyed) {
                try {
                    loadingDialog?.dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error dismissing loading dialog", e)
                }
            }
        }
        loadingDialog = null
    }

    private fun getStatusFromResult(data: Any?): Boolean? = try { data?.javaClass?.getMethod("getStatus")?.invoke(data) as? Boolean } catch (e: Exception) { null }
    private fun getMessageFromResult(data: Any?): String? = try { data?.javaClass?.getMethod("getMessage")?.invoke(data) as? String } catch (e: Exception) { null }
    private fun getDataFromResult(data: Any?): Any? = try { data?.javaClass?.getMethod("getData")?.invoke(data) } catch (e: Exception) { null }
    private fun getErrorsFromResult(data: Any?): Map<String, List<String>>? {
        val payload = getDataFromResult(data)
        return if (payload is Map<*, *>) payload as? Map<String, List<String>> else null
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
        hideLoading()
    }
    // FIX: Add this extension function to define what "isNotFound" means.
    fun SuryaGharApiResult.Error.isNotFound(): Boolean {
        return this.message.contains("404")
    }
}


