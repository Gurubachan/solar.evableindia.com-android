package com.solar.ev.viewModel.installation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.solar.ev.model.DetailDeleteResponse // Added import
import com.solar.ev.model.GenericErrorResponse
import com.solar.ev.model.common.DocumentVerificationRequest

import com.solar.ev.model.installation.InstallationDetailsErrorResponse
import com.solar.ev.model.installation.InstallationDetailsRequest

import com.solar.ev.model.installation.InstallationInfo
import com.solar.ev.network.ApiService
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class InstallationDetailsViewModel(private val apiService: ApiService) : ViewModel() {

    // For Create operation
    private val _createResult = MutableLiveData<InstallationDetailsResult>()
    val createResult: LiveData<InstallationDetailsResult> = _createResult

    // For Update operation
    private val _updateResult = MutableLiveData<InstallationDetailsResult>()
    val updateResult: LiveData<InstallationDetailsResult> = _updateResult

    // For Fetch operation
    private val _fetchedDetail = MutableLiveData<InstallationInfo?>()
    val fetchedDetail: LiveData<InstallationInfo?> = _fetchedDetail

    private val _isFetching = MutableLiveData<Boolean>()
    val isFetching: LiveData<Boolean> = _isFetching

    private val _fetchError = MutableLiveData<String?>()
    val fetchError: LiveData<String?> = _fetchError

    // For Delete operation
    private val _deleteInstallationDetailResult = MutableLiveData<DeleteInstallationDetailResult>()
    val deleteInstallationDetailResult: LiveData<DeleteInstallationDetailResult> = _deleteInstallationDetailResult

    private val _verifyInstallationResult = MutableLiveData<VerifyInstallationDetailResult>()
    val verifyInstallationResult: LiveData<VerifyInstallationDetailResult> = _verifyInstallationResult

    fun submitInstallationDetails(token: String, request: InstallationDetailsRequest) {
        _createResult.value = InstallationDetailsResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.submitInstallationDetails("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _createResult.postValue(InstallationDetailsResult.Success(response.body()!!))
                } else {
                    handleApiError(response, _createResult, "create installation")
                }
            } catch (e: HttpException) {
                _createResult.postValue(InstallationDetailsResult.Error("Network error (create): ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _createResult.postValue(InstallationDetailsResult.Error("IO error (create): ${e.message}"))
            } catch (e: Exception) {
                _createResult.postValue(InstallationDetailsResult.Error("Unexpected error (create): ${e.message}"))
            }
        }
    }

    fun fetchInstallationDetail(token: String, installationId: String) {
        _isFetching.value = true
        _fetchError.value = null
        _fetchedDetail.value = null 
        viewModelScope.launch {
            try {
                val response = apiService.getInstallationDetail("Bearer $token", installationId)
                if (response.isSuccessful) {
                    _fetchedDetail.postValue(response.body()?.data?.installation)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (response.code() == 404) {
                        "Installation details not found."
                    } else {
                        parseGenericErrorBody(errorBody, response.code(), "fetch installation")
                    }
                    _fetchError.postValue(errorMessage)
                }
            } catch (e: HttpException) {
                _fetchError.postValue("Network error fetching details: ${e.message()} (${e.code()})")
            } catch (e: IOException) {
                _fetchError.postValue("IO error fetching details: ${e.message}")
            } catch (e: Exception) {
                _fetchError.postValue("Unexpected error fetching details: ${e.message}")
            }
            finally {
                _isFetching.postValue(false)
            }
        }
    }

    fun updateInstallationDetail(token: String, installationId: String, request: InstallationDetailsRequest) {
        _updateResult.value = InstallationDetailsResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateInstallationDetail("Bearer $token", installationId, request)
                if (response.isSuccessful && response.body() != null) {
                    _updateResult.postValue(InstallationDetailsResult.Success(response.body()!!))
                } else {
                    handleApiError(response, _updateResult, "update installation")
                }
            } catch (e: HttpException) {
                _updateResult.postValue(InstallationDetailsResult.Error("Network error (update): ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _updateResult.postValue(InstallationDetailsResult.Error("IO error (update): ${e.message}"))
            } catch (e: Exception) {
                _updateResult.postValue(InstallationDetailsResult.Error("Unexpected error (update): ${e.message}"))
            }
        }
    }

    fun deleteInstallationDetailById(token: String, installationId: String) {
        _deleteInstallationDetailResult.value = DeleteInstallationDetailResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<DetailDeleteResponse> = apiService.deleteInstallationDetail("Bearer $token", installationId)
                if (response.isSuccessful && response.body()?.status == true) {
                    _deleteInstallationDetailResult.postValue(DeleteInstallationDetailResult.Success(response.body()?.message ?: "Installation details deleted successfully."))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseGenericErrorBody(errorBody, response.code(), "delete installation")
                        else -> "Failed to delete installation details (Code: ${response.code()})"
                    }
                    _deleteInstallationDetailResult.postValue(DeleteInstallationDetailResult.Error(errorMessage, installationId))
                    Log.e("InstallDetailsVM", "Error deleting detail $installationId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _deleteInstallationDetailResult.postValue(DeleteInstallationDetailResult.Error("Network error deleting detail: ${e.message()} (${e.code()})", installationId))
            } catch (e: IOException) {
                _deleteInstallationDetailResult.postValue(DeleteInstallationDetailResult.Error("IO error deleting detail: ${e.message}", installationId))
            } catch (e: Exception) {
                _deleteInstallationDetailResult.postValue(DeleteInstallationDetailResult.Error("Unexpected error deleting detail: ${e.message}", installationId))
            }
        }
    }



    fun verifyInstallationDocument(token: String, installationId: String, request: DocumentVerificationRequest) {
        _verifyInstallationResult.value = VerifyInstallationDetailResult.Loading
        // ... your API call logic ...
        viewModelScope.launch {
            try {
                val response = apiService.verifyInstallationDetail("Bearer $token", installationId, request)
                    if (response.isSuccessful) {
                        _verifyInstallationResult.value = VerifyInstallationDetailResult.Success("Document verified successfully")
                    } else {
                        _verifyInstallationResult.value = VerifyInstallationDetailResult.Error("Verification failed")
                    }
            } catch (e: Exception) {
                _verifyInstallationResult.value = VerifyInstallationDetailResult.Error("An error occurred: ${e.message}")
            }
        }
    }



    private fun <T> handleApiError(response: retrofit2.Response<T>, resultLiveData: MutableLiveData<InstallationDetailsResult>, operation: String) {
        val errorBody = response.errorBody()?.string()
        if (errorBody != null) {
            try {
                val errorResponse = Gson().fromJson(errorBody, InstallationDetailsErrorResponse::class.java)
                if (errorResponse?.errors != null && errorResponse.errors.isNotEmpty()) {
                    val specificErrors = errorResponse.errors.entries.joinToString("; ") { "${it.key}: ${it.value.joinToString()}" }
                    resultLiveData.postValue(InstallationDetailsResult.Error("${errorResponse.message ?: "Validation failed"}: $specificErrors (Code: ${response.code()})"))
                } else {
                    val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                    resultLiveData.postValue(InstallationDetailsResult.Error(genericErrorResponse.message ?: "Operation failed ($operation) (Code: ${response.code()})"))
                }
            } catch (e: JsonSyntaxException) {
                Log.e("InstallDetailsVM", "Error parsing $operation error body as JSON: $errorBody", e)
                resultLiveData.postValue(InstallationDetailsResult.Error(errorBody))
            } catch (e: Exception) {
                Log.e("InstallDetailsVM", "Unexpected error processing $operation error body: $errorBody", e)
                resultLiveData.postValue(InstallationDetailsResult.Error("Error processing $operation server response (Code: ${response.code()})."))
            }
        } else {
            resultLiveData.postValue(InstallationDetailsResult.Error("Operation failed ($operation) (Code: ${response.code()})"))
        }
    }

    private fun parseGenericErrorBody(errorBody: String?, errorCode: Int, operation: String): String {
        return if (errorBody != null) {
            try {
                val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                genericErrorResponse.message ?: "Unknown error during $operation (Code: $errorCode)"
            } catch (e: JsonSyntaxException) {
                Log.e("InstallDetailsVM", "Error parsing $operation generic error body: $errorBody", e)
                errorBody 
            } catch (e: Exception) {
                Log.e("InstallDetailsVM", "Unexpected error processing $operation generic error body: $errorBody", e)
                "Error processing $operation server response (Code: $errorCode)."
            }
        } else {
            "Unknown error during $operation (Code: $errorCode)"
        }
    }
}

sealed class DeleteInstallationDetailResult {
    object Loading : DeleteInstallationDetailResult()
    data class Success(val message: String) : DeleteInstallationDetailResult()
    data class Error(val errorMessage: String, val detailId: String?) : DeleteInstallationDetailResult()
}

sealed class VerifyInstallationDetailResult {
    data class Success(val message: String) : VerifyInstallationDetailResult()
    data class Error(val errorMessage: String) : VerifyInstallationDetailResult()
    object Loading : VerifyInstallationDetailResult()
    // Add other states as needed
}
