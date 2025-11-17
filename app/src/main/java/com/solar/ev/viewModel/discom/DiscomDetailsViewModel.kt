package com.solar.ev.viewModel.discom

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.solar.ev.model.DetailDeleteResponse
import com.solar.ev.model.GenericErrorResponse
import com.solar.ev.model.common.DocumentVerificationRequest // New import
import com.solar.ev.model.common.GenericVerificationResponse // New import
import com.solar.ev.model.discom.DiscomDetailsErrorResponse
import com.solar.ev.model.discom.DiscomDetailsRequest
import com.solar.ev.model.discom.DiscomDetailsResponse // For Success in DiscomDetailsResult
import com.solar.ev.model.discom.DiscomInfo 
import com.solar.ev.network.ApiService
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DiscomDetailsViewModel(private val apiService: ApiService) : ViewModel() {

    // For Create operation
    private val _createDiscomResult = MutableLiveData<DiscomDetailsResult>()
    val createDiscomResult: LiveData<DiscomDetailsResult> = _createDiscomResult

    // For Update operation
    private val _updateDiscomResult = MutableLiveData<DiscomDetailsResult>()
    val updateDiscomResult: LiveData<DiscomDetailsResult> = _updateDiscomResult

    // For Fetch operation
    private val _fetchedDiscomDetail = MutableLiveData<DiscomInfo?>()
    val fetchedDiscomDetail: LiveData<DiscomInfo?> = _fetchedDiscomDetail

    private val _isFetchingDiscomDetail = MutableLiveData<Boolean>()
    val isFetchingDiscomDetail: LiveData<Boolean> = _isFetchingDiscomDetail

    private val _fetchDiscomDetailError = MutableLiveData<String?>()
    val fetchDiscomDetailError: LiveData<String?> = _fetchDiscomDetailError

    // For Delete operation
    private val _deleteDiscomDetailResult = MutableLiveData<DeleteDiscomDetailResult>()
    val deleteDiscomDetailResult: LiveData<DeleteDiscomDetailResult> = _deleteDiscomDetailResult

    // For Verify operation
    private val _verifyDiscomDetailResult = MutableLiveData<VerifyDiscomDetailResult>()
    val verifyDiscomDetailResult: LiveData<VerifyDiscomDetailResult> = _verifyDiscomDetailResult

    fun submitDiscomDetails(token: String, request: DiscomDetailsRequest) {
        _createDiscomResult.value = DiscomDetailsResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.submitDiscomDetails("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _createDiscomResult.postValue(DiscomDetailsResult.Success(response.body()!!))
                } else {
                    handleDiscomApiError(response, _createDiscomResult, "create")
                }
            } catch (e: HttpException) {
                _createDiscomResult.postValue(DiscomDetailsResult.Error("Network error (create): ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _createDiscomResult.postValue(DiscomDetailsResult.Error("IO error (create): ${e.message}"))
            } catch (e: Exception) {
                _createDiscomResult.postValue(DiscomDetailsResult.Error("Unexpected error (create): ${e.message}"))
            }
        }
    }

    fun fetchDiscomDetail(token: String, discomId: String) {
        _isFetchingDiscomDetail.value = true
        _fetchDiscomDetailError.value = null
        _fetchedDiscomDetail.value = null 
        viewModelScope.launch {
            try {
                val response = apiService.getDiscomDetail("Bearer $token", discomId)
                if (response.isSuccessful) {
                    _fetchedDiscomDetail.postValue(response.body()?.data?.discom)
                } else {
                    if (response.code() == 404) {
                        _fetchedDiscomDetail.postValue(null) // Not found, treat as no existing data
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = parseGenericErrorBody(errorBody, response.code(), "fetch discom")
                        _fetchDiscomDetailError.postValue(errorMessage)
                    }
                }
            } catch (e: HttpException) {
                _fetchDiscomDetailError.postValue("Network error fetching discom: ${e.message()} (${e.code()})")
            } catch (e: IOException) {
                _fetchDiscomDetailError.postValue("IO error fetching discom: ${e.message}")
            } catch (e: Exception) {
                _fetchDiscomDetailError.postValue("Unexpected error fetching discom: ${e.message}")
            }
            finally {
                _isFetchingDiscomDetail.postValue(false)
            }
        }
    }

    fun updateDiscomDetail(token: String, discomId: String, request: DiscomDetailsRequest) {
        _updateDiscomResult.value = DiscomDetailsResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateDiscomDetail("Bearer $token", discomId, request)
                if (response.isSuccessful && response.body() != null) {
                    _updateDiscomResult.postValue(DiscomDetailsResult.Success(response.body()!!))
                } else {
                    handleDiscomApiError(response, _updateDiscomResult, "update")
                }
            } catch (e: HttpException) {
                _updateDiscomResult.postValue(DiscomDetailsResult.Error("Network error (update): ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _updateDiscomResult.postValue(DiscomDetailsResult.Error("IO error (update): ${e.message}"))
            } catch (e: Exception) {
                _updateDiscomResult.postValue(DiscomDetailsResult.Error("Unexpected error (update): ${e.message}"))
            }
        }
    }

    fun deleteDiscomDetailById(token: String, discomId: String) {
        _deleteDiscomDetailResult.value = DeleteDiscomDetailResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<DetailDeleteResponse> = apiService.deleteDiscomDetail("Bearer $token", discomId)
                if (response.isSuccessful && response.body()?.status == true) {
                    _deleteDiscomDetailResult.postValue(DeleteDiscomDetailResult.Success(response.body()?.message ?: "Discom details deleted successfully."))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseGenericErrorBody(errorBody, response.code(), "delete discom")
                        else -> "Failed to delete discom details (Code: ${response.code()})"
                    }
                    _deleteDiscomDetailResult.postValue(DeleteDiscomDetailResult.Error(errorMessage, discomId))
                    Log.e("DiscomDetailsVM", "Error deleting discom detail $discomId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _deleteDiscomDetailResult.postValue(DeleteDiscomDetailResult.Error("Network error deleting discom: ${e.message()} (${e.code()})", discomId))
            } catch (e: IOException) {
                _deleteDiscomDetailResult.postValue(DeleteDiscomDetailResult.Error("IO error deleting discom: ${e.message}", discomId))
            } catch (e: Exception) {
                _deleteDiscomDetailResult.postValue(DeleteDiscomDetailResult.Error("Unexpected error deleting discom: ${e.message}", discomId))
            }
        }
    }

    fun verifyDiscomDocument(token: String, discomId: String, request: DocumentVerificationRequest) {
        _verifyDiscomDetailResult.value = VerifyDiscomDetailResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.verifyDiscomDetail("Bearer $token", discomId, request)
                if (response.isSuccessful && response.body()?.status == true) {
                    _verifyDiscomDetailResult.postValue(VerifyDiscomDetailResult.Success(response.body()!!.message))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseVerificationErrorBody(errorBody, response.code())
                        else -> "Failed to verify Discom details (Code: ${response.code()})"
                    }
                    _verifyDiscomDetailResult.postValue(VerifyDiscomDetailResult.Error(errorMessage))
                    Log.e("DiscomDetailsVM", "Error verifying Discom detail $discomId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _verifyDiscomDetailResult.postValue(VerifyDiscomDetailResult.Error("Network error verifying: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _verifyDiscomDetailResult.postValue(VerifyDiscomDetailResult.Error("IO error verifying: ${e.message}"))
            } catch (e: Exception) {
                _verifyDiscomDetailResult.postValue(VerifyDiscomDetailResult.Error("Unexpected error verifying: ${e.message}"))
            }
        }
    }

    private fun handleDiscomApiError(response: Response<*>, resultLiveData: MutableLiveData<DiscomDetailsResult>, operation: String) {
        val errorBody = response.errorBody()?.string()
        val errorMessage = if (errorBody != null) {
            try {
                val discomErrorResponse = Gson().fromJson(errorBody, DiscomDetailsErrorResponse::class.java)
                if (discomErrorResponse?.errors != null && discomErrorResponse.errors.isNotEmpty()) {
                    val specificErrors = discomErrorResponse.errors.entries.joinToString("; ") { "${it.key}: ${it.value.joinToString()}" }
                    "${discomErrorResponse.message ?: "Validation failed"}: $specificErrors (Code: ${response.code()})"
                } else {
                    val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                    genericErrorResponse.message ?: "Operation failed ($operation) (Code: ${response.code()})"
                }
            } catch (e: JsonSyntaxException) {
                Log.e("DiscomDetailsVM", "Error parsing $operation error body as JSON: $errorBody", e)
                errorBody // Fallback to raw error body
            } catch (e: Exception) {
                Log.e("DiscomDetailsVM", "Unexpected error processing $operation error body: $errorBody", e)
                "Error processing $operation server response (Code: ${response.code()})."
            }
        } else {
            "Operation failed ($operation) (Code: ${response.code()})"
        }
        resultLiveData.postValue(DiscomDetailsResult.Error(errorMessage))
    }

    private fun parseGenericErrorBody(errorBody: String?, errorCode: Int, operation: String): String {
        return if (errorBody != null) {
            try {
                val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                genericErrorResponse.message ?: "Unknown error during $operation (Code: $errorCode)"
            } catch (e: JsonSyntaxException) {
                Log.e("DiscomDetailsVM", "Error parsing $operation generic error body: $errorBody", e)
                errorBody 
            } catch (e: Exception) {
                Log.e("DiscomDetailsVM", "Unexpected error processing $operation generic error body: $errorBody", e)
                "Error processing $operation server response (Code: $errorCode)."
            }
        } else {
            "Unknown error during $operation (Code: $errorCode)"
        }
    }

    private fun parseVerificationErrorBody(errorBody: String?, errorCode: Int): String {
        // This function can be tailored if verification endpoints have a specific error structure beyond GenericErrorResponse
        // For example, if they also use the "errors" map like in the API spec.
        return if (errorBody != null) {
            try {
                // Try parsing as the specific validation error structure first
                val validationErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java) // Assuming GenericErrorResponse includes an 'errors' map
                if (validationErrorResponse.errors != null && validationErrorResponse.errors.isNotEmpty()) {
                    val specificErrors = validationErrorResponse.errors.entries.joinToString("; ") { (key, value) -> "$key: ${value.joinToString()}" }
                    return "${validationErrorResponse.message ?: "Validation Failed"}: $specificErrors (Code: $errorCode)"
                }
                // Fallback to the general message if no specific errors map or if it's empty
                validationErrorResponse.message ?: "Verification failed (Code: $errorCode)"
            } catch (e: JsonSyntaxException) {
                Log.e("DiscomDetailsVM", "Error parsing verification error body as JSON: $errorBody", e)
                errorBody // Fallback to raw error body
            } catch (e: Exception) {
                Log.e("DiscomDetailsVM", "Unexpected error processing verification error body: $errorBody", e)
                "Error processing verification server response (Code: $errorCode)."
            }
        } else {
            "Unknown error during verification (Code: $errorCode)"
        }
    }
}

// Sealed result for Discom Detail Verification
sealed class VerifyDiscomDetailResult {
    object Loading : VerifyDiscomDetailResult()
    data class Success(val message: String) : VerifyDiscomDetailResult()
    data class Error(val errorMessage: String) : VerifyDiscomDetailResult()
}


sealed class DeleteDiscomDetailResult {
    object Loading : DeleteDiscomDetailResult()
    data class Success(val message: String) : DeleteDiscomDetailResult()
    data class Error(val errorMessage: String, val detailId: String?) : DeleteDiscomDetailResult()
}
