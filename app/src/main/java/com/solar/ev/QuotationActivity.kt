package com.solar.ev

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.solar.ev.adapter.QuotationAdapter
import com.solar.ev.databinding.ActivityQuotationBinding
import com.solar.ev.databinding.DialogRemarkBinding
import com.solar.ev.model.quotation.QuotationListItem
import com.solar.ev.model.quotation.QuotationListResponse
import com.solar.ev.model.quotation.QuotationRemarkRequest
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.viewModel.suryaghar.SuryaGharApiResult
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModel
import com.solar.ev.viewModel.suryaghar.SuryaGharViewModelFactory

class QuotationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuotationBinding
    private val viewModel: SuryaGharViewModel by viewModels {
        SuryaGharViewModelFactory(RetrofitInstance.api)
    }
    private lateinit var sessionManager: SessionManager
    private var applicationId: String? = null
    private var userRole: String? = null

    companion object {
        const val EXTRA_APPLICATION_ID = "EXTRA_APPLICATION_ID"
        const val BASE_IMAGE_URL = "https://solar.evableindia.com/core/public/storage/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuotationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        applicationId = intent.getStringExtra(EXTRA_APPLICATION_ID)
        userRole = sessionManager.getUserRole()

        setSupportActionBar(binding.toolbarQuotation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Quotations"

        binding.rvQuotations.layoutManager = LinearLayoutManager(this)

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (userRole == "admin" || userRole == "back-office" || userRole == "supervisor") {
            binding.fabAddQuotation.visibility = View.VISIBLE
            binding.fabAddQuotation.setOnClickListener {
                val intent = Intent(this, CreateQuotationActivity::class.java)
                intent.putExtra(CreateQuotationActivity.EXTRA_APPLICATION_ID, applicationId)
                startActivity(intent)
            }
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadQuotations()
    }

    private fun loadQuotations() {
        val token = sessionManager.getUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session Expired.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.getQuotationsByApplication(token, applicationId!!)
    }

    private fun observeViewModel() {
        viewModel.quotationListResult.observe(this) { result ->
            when (result) {
                is SuryaGharApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is SuryaGharApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (result.data.status && result.data.data != null) {
                        if (result.data.data.isEmpty()) {
                            binding.tvNoQuotations.visibility = View.VISIBLE
                            binding.rvQuotations.visibility = View.GONE
                        } else {
                            binding.tvNoQuotations.visibility = View.GONE
                            binding.rvQuotations.visibility = View.VISIBLE
                            setupQuotations(result.data.data)
                        }
                    } else {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_LONG).show()
                    }
                }
                is SuryaGharApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        val observer = Observer<SuryaGharApiResult<QuotationListResponse>> { result ->
            when (result) {
                is SuryaGharApiResult.Loading -> { /* Handle loading */ }
                is SuryaGharApiResult.Success -> {
                    if (result.data.status) {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_LONG).show()
                        loadQuotations()
                    } else {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_LONG).show()
                    }
                }
                is SuryaGharApiResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.deleteQuotationResult.observe(this, observer)
        viewModel.updateQuotationResult.observe(this, observer)
        viewModel.submitQuotationResult.observe(this, observer)
        viewModel.approveQuotationResult.observe(this, observer)
        viewModel.rejectQuotationResult.observe(this, observer)
        viewModel.requestRevisionQuotationResult.observe(this, observer)
    }

    private fun setupQuotations(quotations: List<QuotationListItem>) {
        val adapter = QuotationAdapter(quotations, userRole) { quotation, action ->
            val token = sessionManager.getUserToken()
            if (token == null) {
                Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
                return@QuotationAdapter
            }

            if (action == "delete" || action == "view" || action == "update") {
                handleAction(token, quotation, action, null)
            } else {
                showRemarkDialog(token, quotation, action)
            }
        }
        binding.rvQuotations.adapter = adapter
    }

    private fun showRemarkDialog(token: String, quotation: QuotationListItem, action: String) {
        val dialogBinding = DialogRemarkBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Remark")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val remark = dialogBinding.etRemark.text.toString()
                if (remark.isNotEmpty()) {
                    handleAction(token, quotation, action, remark)
                    dialog.dismiss()
                } else {
                    dialogBinding.etRemark.error = "Remark is mandatory"
                }
            }
        }
        dialog.show()
    }

    private fun handleAction(token: String, quotation: QuotationListItem, action: String, remark: String?) {
        when (action) {
            "delete" -> {
                AlertDialog.Builder(this)
                    .setTitle("Delete Quotation")
                    .setMessage("Are you sure you want to delete this quotation?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteQuotation(token, quotation.id)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            "view" -> {
                val url = BASE_IMAGE_URL + quotation.quotationFile
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
            "update" -> {
                val intent = Intent(this, CreateQuotationActivity::class.java)
                intent.putExtra(CreateQuotationActivity.EXTRA_APPLICATION_ID, applicationId)
                intent.putExtra("quotation_id", quotation.id) // Pass quotation id for updating
                startActivity(intent)
            }
            "submit" -> {
                viewModel.submitQuotation(token, quotation.id, QuotationRemarkRequest(remark!!))
            }
            "approve" -> {
                viewModel.approveQuotation(token, quotation.id, QuotationRemarkRequest(remark!!))
            }
            "reject" -> {
                viewModel.rejectQuotation(token, quotation.id, QuotationRemarkRequest(remark!!))
            }
            "revision" -> {
                viewModel.requestRevisionQuotation(token, quotation.id, QuotationRemarkRequest(remark!!))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
