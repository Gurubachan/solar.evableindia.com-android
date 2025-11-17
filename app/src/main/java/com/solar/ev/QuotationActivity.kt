package com.solar.ev

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.solar.ev.adapter.QuotationAdapter
import com.solar.ev.databinding.ActivityQuotationBinding
import com.solar.ev.model.quotation.QuotationListItem
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

        if (applicationId == null) {
            Toast.makeText(this, "Application ID is missing!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (userRole == "admin" || userRole == "back-office") {
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
                    if (result.data.success && result.data.data != null) {
                        setupQuotations(result.data.data)
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

        viewModel.deleteQuotationResult.observe(this) { result ->
            when (result) {
                is SuryaGharApiResult.Loading -> { /* Handle loading */ }
                is SuryaGharApiResult.Success -> {
                    if (result.data.success) {
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
    }

    private fun setupQuotations(quotations: List<QuotationListItem>) {
        val adapter = QuotationAdapter(quotations, userRole) { quotation, action ->
            when (action) {
                "delete" -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Quotation")
                        .setMessage("Are you sure you want to delete this quotation?")
                        .setPositiveButton("Delete") { _, _ ->
                            val token = sessionManager.getUserToken()
                            if (token != null) {
                                viewModel.deleteQuotation(token, quotation.id)
                            } else {
                                Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
        binding.rvQuotations.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
