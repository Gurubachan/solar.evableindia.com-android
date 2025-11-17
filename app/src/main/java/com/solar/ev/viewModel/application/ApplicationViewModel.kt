package com.solar.ev.viewModel.application

import android.util.Log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.solar.ev.model.UploadPhotoResponse
import com.solar.ev.model.application.ApplicationData
import com.solar.ev.model.application.ApplicationListData

import com.solar.ev.model.application.ApplicationRequest
import com.solar.ev.model.application.ApplicationSuccessResponse
import com.solar.ev.network.ApiService
import kotlinx.coroutines.launch



class ApplicationViewModel(private val apiService: ApiService) : ViewModel() {


    sealed class SubmitResult {
        object Loading : SubmitResult()
        data class Success(val response: ApplicationSuccessResponse) : SubmitResult() // Or a generic success response
        data class Error(val errorMessage: String) : SubmitResult()
    }

    private val _submitResult = MutableLiveData<SubmitResult>()
    val submitResult: LiveData<SubmitResult> = _submitResult

    // For fetching details of a single application (for edit mode)
    sealed class ApplicationDetailResult {
        object Loading : ApplicationDetailResult()
        data class Success(val application: ApplicationData?) : ApplicationDetailResult() // application can be null from response
        data class Error(val errorMessage: String) : ApplicationDetailResult()
    }

    private val _applicationDetailResult = MutableLiveData<ApplicationDetailResult>()
    val applicationDetailResult: LiveData<ApplicationDetailResult> = _applicationDetailResult

    // For photo upload result (if handled separately, otherwise can be integrated)
    sealed class UploadPhotoResult {
        object Loading : UploadPhotoResult()
        data class Success(val response: UploadPhotoResponse) : UploadPhotoResult()
        data class Error(val errorMessage: String) : UploadPhotoResult()
    }
    private val _uploadPhotoResult = MutableLiveData<UploadPhotoResult>()
    val uploadPhotoResult: LiveData<UploadPhotoResult> = _uploadPhotoResult


    /**
     * Fetches details of a specific application by its ID.
     */
    fun fetchApplicationDetails(token: String, applicationId: String) {
        _applicationDetailResult.value = ApplicationDetailResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getApplicationDetails(token, applicationId)
                Log.d("AppViewModel",response.toString())
                if (response.isSuccessful) {
                    Log.d("AppViewModel",response.body().toString())
                    _applicationDetailResult.value = ApplicationDetailResult.Success(response.body()?.data)
                } else {
                    val errorMsg = parseErrorBody(response.errorBody()?.string(), response.code())
                    _applicationDetailResult.value = ApplicationDetailResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "FetchAppDetails Error", e)
                _applicationDetailResult.value = ApplicationDetailResult.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Submits a new application.
     * Combines ApplicationRequest (JSON) and an optional photo file part.
     */
    fun submitApplication(token: String, request: ApplicationRequest) {
        _submitResult.value = SubmitResult.Loading
        viewModelScope.launch {
            try {
                // Backend needs to handle ApplicationRequest as JSON and photo as Multipart part
                // This might require a specific Retrofit setup or your API to accept this.
                // A common way is to send all text fields as RequestBody parts if the primary content type is multipart.
                // Or, use @Part for the JSON request and @Part for the file.

                // For simplicity, assuming apiService.submitApplication can take both.
                // If your API expects all data as multipart, you'll need to convert 'request' fields to RequestBody parts.
                val response = apiService.submitApplication(
                    token = token,
                    request = request, // This would be @Body or @Part ApplicationRequest
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        _submitResult.value = SubmitResult.Success(it)
                    } ?: run {
                        _submitResult.value = SubmitResult.Error("Empty success response body.")
                    }
                } else {
                    val errorMsg = parseErrorBody(response.errorBody()?.string(), response.code())
                    _submitResult.value = SubmitResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "SubmitApplication Error", e)
                _submitResult.value = SubmitResult.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Updates an existing application.
     */
    fun updateExistingApplication(
        applicationId: String,
        token: String,
        request: ApplicationRequest,

    ) {
        _submitResult.value = SubmitResult.Loading // Reusing submitResult for update operation
        viewModelScope.launch {
            try {
                // Similar to submitApplication, ensure your Retrofit call is correctly defined.
                val response = apiService.updateApplication(
                    id = applicationId,
                    token = token,
                    applicationRequest = request,

                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        _submitResult.value = SubmitResult.Success(it)
                    } ?: run {
                        _submitResult.value = SubmitResult.Error("Empty success response body on update.")
                    }
                } else {
                    val errorMsg = parseErrorBody(response.errorBody()?.string(), response.code())
                    _submitResult.value = SubmitResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "UpdateApplication Error", e)
                _submitResult.value = SubmitResult.Error("Network error: ${e.message}")
            }
        }
    }


    private fun parseErrorBody(errorBodyString: String?, errorCode: Int): String {
        var displayErrorMessage = "An unknown error occurred (Code: $errorCode)"
        if (!errorBodyString.isNullOrEmpty()) {
            try {
                val gson = Gson()
                // Try parsing as a generic Map for dynamic keys like {"error": "message"}
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val errorMap: Map<String, Any> = gson.fromJson(errorBodyString, type)

                if (errorMap.isNotEmpty()) {
                    displayErrorMessage = when {
                        errorMap.containsKey("message") -> errorMap["message"]?.toString() ?: errorBodyString
                        errorMap.containsKey("error") -> errorMap["error"]?.toString() ?: errorBodyString
                        else -> {
                            val firstEntry = errorMap.entries.firstOrNull()
                            firstEntry?.let { "${it.key}: ${it.value}" } ?: errorBodyString
                        }
                    }
                    if (displayErrorMessage.isBlank() && displayErrorMessage != errorBodyString) {
                        displayErrorMessage = errorBodyString // Fallback if parsed message is blank
                    }
                } else {
                    // JSON was valid but empty, or not an object, use raw string.
                    displayErrorMessage = errorBodyString
                }
            } catch (e: JsonSyntaxException) {
                // Not valid JSON, use the raw error body string.
                Log.e("AppViewModel", "Error parsing error body as JSON map: $errorBodyString", e)
                displayErrorMessage = errorBodyString
            } catch (e: Exception) {
                // Other unexpected errors during parsing.
                Log.e("AppViewModel", "Unexpected error processing error body: $errorBodyString", e)
                displayErrorMessage = "Error processing server response (Code: $errorCode)."
            }
        }
        // Ensure error code is part of the message if not already included by parsing
        if (!displayErrorMessage.contains("(Code: $errorCode)", ignoreCase = true) && errorBodyString != displayErrorMessage) {
            displayErrorMessage += " (Code: $errorCode)"
        }
        return displayErrorMessage
    }
}
