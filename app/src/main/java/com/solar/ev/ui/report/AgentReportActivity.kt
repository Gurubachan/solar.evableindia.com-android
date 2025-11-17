package com.solar.ev.ui.report

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.solar.ev.adapter.AgentReportAdapter
import com.solar.ev.databinding.ActivityAgentReportBinding
import com.solar.ev.model.report.AgentReportItem
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import com.solar.ev.viewModel.report.AgentReportViewModel
import com.solar.ev.viewModel.report.AgentReportViewModelFactory
import com.solar.ev.viewModel.suryaghar.SuryaGharApiResult

class AgentReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgentReportBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var agentReportAdapter: AgentReportAdapter

    private val viewModel: AgentReportViewModel by viewModels {
        AgentReportViewModelFactory(RetrofitInstance.api)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgentReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        fetchReportData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarAgentReport)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agent Wise Report"
    }

    private fun setupRecyclerView() {
        agentReportAdapter = AgentReportAdapter()
        binding.rvAgentReport.apply {
            adapter = agentReportAdapter
            layoutManager = LinearLayoutManager(this@AgentReportActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.agentReportResult.observe(this) { result ->
            when (result) {
                is SuryaGharApiResult.Loading -> {
                    binding.progressBarAgentReport.visibility = View.VISIBLE
                    binding.rvAgentReport.visibility = View.GONE
                    binding.tvEmptyViewAgentReport.visibility = View.GONE
                }
                is SuryaGharApiResult.Success -> {
                    binding.progressBarAgentReport.visibility = View.GONE
                    val reportData = result.data.data
                    if (result.data.status && !reportData.isNullOrEmpty()) {
                        agentReportAdapter.submitList(reportData)
                        binding.rvAgentReport.visibility = View.VISIBLE
                        binding.tvEmptyViewAgentReport.visibility = View.GONE
                    } else {
                        binding.rvAgentReport.visibility = View.GONE
                        binding.tvEmptyViewAgentReport.text = result.data.message ?: "No data found."
                        binding.tvEmptyViewAgentReport.visibility = View.VISIBLE
                    }
                }
                is SuryaGharApiResult.Error -> {
                    binding.progressBarAgentReport.visibility = View.GONE
                    binding.rvAgentReport.visibility = View.GONE
                    binding.tvEmptyViewAgentReport.text = result.message ?: "Failed to load report."
                    binding.tvEmptyViewAgentReport.visibility = View.VISIBLE
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchReportData() {
        val token = sessionManager.getUserToken()
        if (token != null) {
            viewModel.fetchAgentReport(token)
        } else {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            binding.progressBarAgentReport.visibility = View.GONE
            binding.tvEmptyViewAgentReport.text = "User session not found."
            binding.tvEmptyViewAgentReport.visibility = View.VISIBLE
            // Optionally, navigate to login screen
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
