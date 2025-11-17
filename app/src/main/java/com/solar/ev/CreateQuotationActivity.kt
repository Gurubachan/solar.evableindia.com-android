package com.solar.ev

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.solar.ev.databinding.ActivityCreateQuotationBinding
import com.solar.ev.model.quotation.CreateQuotationRequest
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.viewModel.suryaghar.SuryaGharApiResult
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModel
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModelFactory
import java.io.IOException

class CreateQuotationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQuotationBinding
    private val viewModel: SuryaGharViewModel by viewModels {
        SuryaGharViewModelFactory(RetrofitInstance.api)
    }
    private lateinit var sessionManager: SessionManager
    private var applicationId: String? = null
    private var quotationFileBase64: String? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                handleFilePicked(uri)
            }
        }
    }

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQuotationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)

        setSupportActionBar(binding.toolbarCreateQuotation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Quotation"

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val fundingTypes = arrayOf("self_funding", "bank_loan", "private_loan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fundingTypes)
        binding.actFundingType.setAdapter(adapter)

        binding.btnUploadQuotationFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnSubmitQuotation.setOnClickListener {
            submitQuotation()
        }

        observeViewModel()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleFilePicked(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                quotationFileBase64 = "data:application/pdf;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
                binding.tvFileName.text = getFileName(uri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
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

    private fun submitQuotation() {
        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session Expired.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateQuotationRequest(
            applicationId = applicationId!!,
            quotationReferenceId = binding.etQuotationRefId.text.toString(),
            fundingType = binding.actFundingType.text.toString(),
            quotationAmount = binding.etQuotationAmount.text.toString().toDoubleOrNull(),
            quotationFile = quotationFileBase64,
            quotationDetails = binding.etQuotationDetails.text.toString(),
            remarks = binding.etRemarks.text.toString()
        )

        viewModel.createQuotation(token, request)
    }

    private fun observeViewModel() {
        viewModel.createQuotationResult.observe(this) { result ->
            when (result) {
                is SuryaGharApiResult.Loading -> { /* Handle loading */ }
                is SuryaGharApiResult.Success -> {
                    if (result.data.success) {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_LONG).show()
                    }
                }
                is SuryaGharApiResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
