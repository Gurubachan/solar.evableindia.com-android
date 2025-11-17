package com.solar.ev.viewModel.application

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.solar.ev.model.DeleteApplicationResponse
// ApplicationListItem is now assumed to be the type directly from ApplicationListResponse
import com.solar.ev.model.application.ApplicationListItem
import com.solar.ev.model.application.ApplicationListResponse 
// com.solar.ev.model.application.Application might be unused here if ApplicationListResponse provides ApplicationListItem directly
import com.solar.ev.network.ApiService
import kotlinx.coroutines.launch
import retrofit2.Response

class MyApplicationsViewModel(private val apiService: ApiService) : ViewModel() {

    // Sealed class to represent different states of fetching applications
    sealed class MyApplicationsResult {
        object Loading : MyApplicationsResult()
        data class Success(val applications: List<ApplicationListItem>) : MyApplicationsResult() // Expects ApplicationListItem
        data class Error(val errorMessage: String) : MyApplicationsResult()
    }

    private val _applicationsResult = MutableLiveData<MyApplicationsResult>()
    val applicationsResult: LiveData<MyApplicationsResult> = _applicationsResult

    // Sealed class for delete operation results
    sealed class DeleteApplicationResult {
        object Loading : DeleteApplicationResult()
        data class Success(val message: String) : DeleteApplicationResult()
        data class Error(val errorMessage: String, val  applicationId: String? = null) : DeleteApplicationResult()
    }

    private val _deleteResult = MutableLiveData<DeleteApplicationResult>()
    val deleteResult: LiveData<DeleteApplicationResult> = _deleteResult

    private val adminLikeRoles = listOf("admin")

    // Removed mapToApplicationListItem helper function. 
    // Assuming ApplicationListResponse directly provides List<ApplicationListItem>.

    fun fetchApplications(token: String, userRole: String) {
        _applicationsResult.value = MyApplicationsResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<ApplicationListResponse> = 
                    if (adminLikeRoles.any { it.equals(userRole, ignoreCase = true) }) {
                        apiService.getAdminApplications(token)
                    } else {
                        apiService.getMyApplications(token)
                    }

                if (response.isSuccessful) {
                    // Assuming response.body().data.applications is List<ApplicationListItem> now
                    val applicationsFromResponse: List<ApplicationListItem>? = response.body()?.data?.applications
                    if (applicationsFromResponse != null) {
                        _applicationsResult.value = MyApplicationsResult.Success(applicationsFromResponse)
                    } else {
                        _applicationsResult.value = MyApplicationsResult.Success(emptyList())
                        Log.w("MyApplicationsViewModel", "Applications list was null despite successful response.")
                    }
                } else {
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
                                    firstEntry?.let { "${it.key}: ${it.value}" } ?: errorBodyString // Line 86
                                }
                            }
                            if (displayErrorMessage.isBlank() && displayErrorMessage != errorBodyString) {
                                displayErrorMessage = errorBodyString
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e("MyApplicationsViewModel", "Error parsing error body as JSON map: $errorBodyString", e)
                            displayErrorMessage = errorBodyString
                        } catch (e: Exception) {
                            Log.e("MyApplicationsViewModel", "Unexpected error processing error body: $errorBodyString", e)
                            displayErrorMessage = "Error processing server response."
                        }
                    } else {
                        displayErrorMessage = "Failed to fetch applications."
                    }

                    val errorCodeSuffix = " (Code: ${response.code()})"
                    if (!displayErrorMessage.contains(errorCodeSuffix, ignoreCase = true)) {
                        displayErrorMessage += errorCodeSuffix
                    }

                    _applicationsResult.value = MyApplicationsResult.Error(displayErrorMessage)
                    Log.e("MyApplicationsViewModel", "API Error: $displayErrorMessage --- Raw Error Body for debug: $errorBodyString")
                }
            } catch (e: Exception) {
                Log.e("MyApplicationsViewModel", "Network or unexpected error fetching applications", e)
                _applicationsResult.value = MyApplicationsResult.Error("Network error or unexpected issue: ${e.message}")
            }
        }
    }

    fun deleteApplicationById(token: String, applicationId: String) {
        _deleteResult.value = DeleteApplicationResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<DeleteApplicationResponse> = apiService.deleteApplication(token, applicationId)

                if (response.isSuccessful && response.body()?.status == true) {
                    _deleteResult.value = DeleteApplicationResult.Success(response.body()?.message ?: "Application deleted successfully.")
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    var displayErrorMessage: String

                    if (response.body()?.status == false && response.body()?.message?.isNotEmpty() == true) {
                         displayErrorMessage = response.body()!!.message
                    } else if (!errorBodyString.isNullOrEmpty()) {
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
                            Log.e("MyApplicationsViewModel", "Error parsing error body for delete: $errorBodyString", e)
                            displayErrorMessage = errorBodyString
                        } catch (e: Exception) {
                            Log.e("MyApplicationsViewModel", "Unexpected error processing error body for delete: $errorBodyString", e)
                            displayErrorMessage = "Error processing server response for delete."
                        }
                    } else {
                        displayErrorMessage = "Failed to delete application."
                    }

                    val errorCodeSuffix = " (Code: ${response.code()})"
                     if (!displayErrorMessage.contains(errorCodeSuffix, ignoreCase = true)) {
                        displayErrorMessage += errorCodeSuffix
                    }
                    _deleteResult.value = DeleteApplicationResult.Error(displayErrorMessage, applicationId)
                    Log.e("MyApplicationsViewModel", "API Error deleting application $applicationId: $displayErrorMessage --- Raw Error Body: $errorBodyString")
                }
            } catch (e: Exception) {
                Log.e("MyApplicationsViewModel", "Network or unexpected error deleting application $applicationId", e)
                _deleteResult.value = DeleteApplicationResult.Error("Network error or unexpected issue: ${e.message}", applicationId)
            }
        }
    }
}
