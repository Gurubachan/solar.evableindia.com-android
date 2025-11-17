package com.solar.ev.viewModel.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.report.AgentReportResponse
import com.solar.ev.network.ApiService
import com.solar.ev.util.NetworkUtils
import com.solar.ev.viewModel.suryaghar.SuryaGharApiResult // Reusing the generic result wrapper
import kotlinx.coroutines.launch

class AgentReportViewModel(private val apiService: ApiService) : ViewModel() {

    private val _agentReportResult = MutableLiveData<SuryaGharApiResult<AgentReportResponse>>()
    val agentReportResult: LiveData<SuryaGharApiResult<AgentReportResponse>> = _agentReportResult

    fun fetchAgentReport(token: String) {
        _agentReportResult.value = SuryaGharApiResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getAgentWiseReport("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    _agentReportResult.value = SuryaGharApiResult.Success(response.body()!!)
                } else {
                    val errorResponse = NetworkUtils.parseError(response) // Assuming NetworkUtils can parse generic errors
                    _agentReportResult.value = SuryaGharApiResult.Error(
                        errorResponse.message ?: "An unknown error occurred while fetching agent report",
                        errorResponse.data as? Map<String, List<String>>?
                    )
                }
            } catch (e: Exception) {
                _agentReportResult.value = SuryaGharApiResult.Error(e.message ?: "An exception occurred while fetching agent report")
            }
        }
    }
}
