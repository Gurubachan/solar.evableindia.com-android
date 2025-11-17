package com.solar.ev.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.solar.ev.model.DashboardStatsData
import com.solar.ev.model.DashboardStatsResponse
import com.solar.ev.network.ApiService
import kotlinx.coroutines.launch
import retrofit2.Response

// ApplicationSummaryCounts is replaced by DashboardStatsData directly

class MainViewModel(private val apiService: ApiService) : ViewModel() {

    sealed class SummaryResult {
        object Loading : SummaryResult()
        data class Success(val stats: DashboardStatsData) : SummaryResult() // Changed to use DashboardStatsData
        data class Error(val errorMessage: String) : SummaryResult()
    }

    private val _summaryResult = MutableLiveData<SummaryResult>()
    val summaryResult: LiveData<SummaryResult> = _summaryResult

    // userRole might not be needed if the stats endpoint doesn't require it, 
    // but keeping it in the function signature for now if your API needs it for other reasons.
    fun loadApplicationSummaryCounts(token: String, userRole: String?) { // userRole can be nullable if not strictly needed by API
        _summaryResult.value = SummaryResult.Loading
        viewModelScope.launch {
            try {
                // ASSUMPTION: You will add getDashboardStats(token) to your ApiService interface
                // e.g., @GET("v1/dashboard/stats") suspend fun getDashboardStats(@Header("Authorization") token: String): Response<DashboardStatsResponse>
                val response: Response<DashboardStatsResponse> = apiService.getDashboardStats(token)

                if (response.isSuccessful) {
                    val statsData = response.body()?.data
                    if (statsData != null) {
                        _summaryResult.value = SummaryResult.Success(statsData)
                    } else {
                        _summaryResult.value = SummaryResult.Error("No data received from dashboard stats.")
                        Log.w("MainViewModel", "Dashboard stats data was null despite successful response.")
                    }
                } else {
                    handleApiError(response)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Network or unexpected error fetching dashboard stats", e)
                _summaryResult.value = SummaryResult.Error("Network error: ${e.message}")
            }
        }
    }

    // Removed calculateCounts as data comes directly from API

    private fun handleApiError(response: Response<*>) {
        val errorBodyString = response.errorBody()?.string()
        var displayErrorMessage: String

        if (!errorBodyString.isNullOrEmpty()) {
            try {
                val gson = Gson()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val errorMap: Map<String, Any> = gson.fromJson(errorBodyString, type)

                displayErrorMessage = when {
                    errorMap.containsKey("message") -> errorMap["message"]?.toString() ?: errorBodyString
                    errorMap.containsKey("error") -> errorMap["error"]?.toString() ?: errorBodyString
                    else -> {
                        val firstEntry = errorMap.entries.firstOrNull()
                        firstEntry?.let { "${it.key}: ${it.value}" } ?: errorBodyString
                    }
                }
                if (displayErrorMessage.isBlank() && displayErrorMessage != errorBodyString) {
                    displayErrorMessage = errorBodyString
                }
            } catch (e: JsonSyntaxException) {
                Log.e("MainViewModel", "Error parsing error body: $errorBodyString", e)
                displayErrorMessage = errorBodyString
            } catch (e: Exception) {
                Log.e("MainViewModel", "Unexpected error processing error body: $errorBodyString", e)
                displayErrorMessage = "Error processing server response."
            }
        } else {
            displayErrorMessage = "Failed to fetch data."
        }

        val errorCodeSuffix = " (Code: ${response.code()})"
        if (!displayErrorMessage.contains(errorCodeSuffix, ignoreCase = true)) {
            displayErrorMessage += errorCodeSuffix
        }
        _summaryResult.value = SummaryResult.Error(displayErrorMessage)
        Log.e("MainViewModel", "API Error: $displayErrorMessage")
    }
}
