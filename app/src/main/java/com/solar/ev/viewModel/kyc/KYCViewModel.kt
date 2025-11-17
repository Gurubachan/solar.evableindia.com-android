package com.solar.ev.viewModel.kyc

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.DetailDeleteResponse
import com.solar.ev.model.GenericErrorResponse
import com.solar.ev.model.common.DocumentVerificationRequest // New import
import com.solar.ev.model.kyc.KYCSubmitRequest
import com.solar.ev.model.kyc.KYCSubmitResponse
import com.solar.ev.model.kyc.KYCDetails
import com.solar.ev.model.kyc.KYCFetchResponse
import com.solar.ev.model.kyc.KYCUpdateResponse
import com.solar.ev.network.ApiService
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class KYCViewModel(private val apiService: ApiService) : ViewModel() {

    // For submitting KYC
    private val _kycSubmitResult = MutableLiveData<KYCSubmitResult>()
    val kycSubmitResult: LiveData<KYCSubmitResult> = _kycSubmitResult

    // For updating KYC
    private val _kycUpdateResult = MutableLiveData<KYCUpdateResult>()
    val kycUpdateResult: LiveData<KYCUpdateResult> = _kycUpdateResult

    // For fetching existing KYC document
    private val _kycDetails = MutableLiveData<KYCDetails?>()
    val kycDetails: LiveData<KYCDetails?> = _kycDetails

    private val _fetchKycError = MutableLiveData<String?>()
    val fetchKycError: LiveData<String?> = _fetchKycError

    private val _isFetchingKyc = MutableLiveData<Boolean>()
    val isFetchingKyc: LiveData<Boolean> = _isFetchingKyc

    // For deleting KYC Document
    private val _deleteKycDocumentResult = MutableLiveData<DeleteKYCDocumentResult>()
    val deleteKycDocumentResult: LiveData<DeleteKYCDocumentResult> = _deleteKycDocumentResult

    // For verifying KYC Document
    private val _verifyKycDocumentResult = MutableLiveData<VerifyKYCDocumentResult>()
    val verifyKycDocumentResult: LiveData<VerifyKYCDocumentResult> = _verifyKycDocumentResult

    fun submitKYCDocuments(token: String, request: KYCSubmitRequest) {
        _kycSubmitResult.value = KYCSubmitResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.createKYCDocument("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    _kycSubmitResult.postValue(KYCSubmitResult.Success(response.body()!!))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorBody(errorBody, response.code(), "submit KYC")
                    _kycSubmitResult.postValue(KYCSubmitResult.Error(errorMessage))
                }
            } catch (e: HttpException) {
                _kycSubmitResult.postValue(KYCSubmitResult.Error("Network error submitting KYC: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _kycSubmitResult.postValue(KYCSubmitResult.Error("IO error submitting KYC: ${e.message}"))
            } catch (e: Exception) {
                _kycSubmitResult.postValue(KYCSubmitResult.Error("Unexpected error submitting KYC: ${e.message}"))
            }
        }
    }

    fun updateKYCDocuments(token: String, kycId: String, request: KYCSubmitRequest) {
        _kycUpdateResult.value = KYCUpdateResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateKYCDocument("Bearer $token", kycId, request)
                if (response.isSuccessful && response.body() != null) {
                    _kycUpdateResult.postValue(KYCUpdateResult.Success(response.body()!!))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorBody(errorBody, response.code(), "update KYC")
                    _kycUpdateResult.postValue(KYCUpdateResult.Error(errorMessage))
                }
            } catch (e: HttpException) {
                _kycUpdateResult.postValue(KYCUpdateResult.Error("Network error updating KYC: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _kycUpdateResult.postValue(KYCUpdateResult.Error("IO error updating KYC: ${e.message}"))
            } catch (e: Exception) {
                _kycUpdateResult.postValue(KYCUpdateResult.Error("Unexpected error updating KYC: ${e.message}"))
            }
        }
    }

    fun fetchKYCDocument(token: String, kycId: String) {
        _isFetchingKyc.value = true
        _fetchKycError.value = null 
        _kycDetails.value = null    
        viewModelScope.launch {
            try {
                val response = apiService.getKYCDocument("Bearer $token", kycId)
                if (response.isSuccessful) {
                    val kycData = response.body()?.data?.kyc
                    _kycDetails.postValue(kycData) // Can be null if data or kyc is null
                } else {
                    if (response.code() == 404) {
                        _kycDetails.postValue(null) // Document not found
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = parseErrorBody(errorBody, response.code(), "fetch KYC")
                        _fetchKycError.postValue(errorMessage)
                    }
                }
            } catch (e: HttpException) {
                _fetchKycError.postValue("Network error fetching KYC: ${e.message()} (${e.code()})")
            } catch (e: IOException) {
                _fetchKycError.postValue("IO error fetching KYC: ${e.message}")
            } catch (e: Exception) {
                _fetchKycError.postValue("An unexpected error occurred while fetching KYC: ${e.message}")
            }
            finally {
                _isFetchingKyc.postValue(false)
            }
        }
    }

    fun deleteKYCDocumentById(token: String, kycId: String) {
        _deleteKycDocumentResult.value = DeleteKYCDocumentResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<DetailDeleteResponse> = apiService.deleteKYCDocument("Bearer $token", kycId)
                if (response.isSuccessful && response.body()?.status == true) {
                    _deleteKycDocumentResult.postValue(DeleteKYCDocumentResult.Success(response.body()?.message ?: "KYC details deleted successfully."))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseErrorBody(errorBody, response.code(), "delete KYC")
                        else -> "Failed to delete KYC details (Code: ${response.code()})"
                    }
                    _deleteKycDocumentResult.postValue(DeleteKYCDocumentResult.Error(errorMessage, kycId))
                    Log.e("KYCViewModel", "Error deleting KYC $kycId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _deleteKycDocumentResult.postValue(DeleteKYCDocumentResult.Error("Network error deleting KYC: ${e.message()} (${e.code()})", kycId))
            } catch (e: IOException) {
                _deleteKycDocumentResult.postValue(DeleteKYCDocumentResult.Error("IO error deleting KYC: ${e.message}", kycId))
            } catch (e: Exception) {
                _deleteKycDocumentResult.postValue(DeleteKYCDocumentResult.Error("Unexpected error deleting KYC: ${e.message}", kycId))
            }
        }
    }

    fun verifyKYCDocument(token: String, kycId: String, request: DocumentVerificationRequest) {
        _verifyKycDocumentResult.value = VerifyKYCDocumentResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.verifyKycDocument("Bearer $token", kycId, request)
                if (response.isSuccessful && response.body()?.status == true) {
                    _verifyKycDocumentResult.postValue(VerifyKYCDocumentResult.Success(response.body()!!.message))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                     val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseErrorBody(errorBody, response.code(), "verify KYC")
                        else -> "Failed to verify KYC (Code: ${response.code()})"
                    }
                    _verifyKycDocumentResult.postValue(VerifyKYCDocumentResult.Error(errorMessage))
                    Log.e("KYCViewModel", "Error verifying KYC $kycId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _verifyKycDocumentResult.postValue(VerifyKYCDocumentResult.Error("Network error verifying KYC: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _verifyKycDocumentResult.postValue(VerifyKYCDocumentResult.Error("IO error verifying KYC: ${e.message}"))
            } catch (e: Exception) {
                _verifyKycDocumentResult.postValue(VerifyKYCDocumentResult.Error("Unexpected error verifying KYC: ${e.message}"))
            }
        }
    }

    private fun parseErrorBody(errorBody: String?, errorCode: Int, operation: String): String {
        return if (errorBody != null) {
            try {
                val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                // Check for specific validation errors structure if applicable from GenericVerificationResponse for PATCH
                val validationErrors = genericErrorResponse.errors
                if (validationErrors != null && validationErrors.isNotEmpty()) {
                    // Example: Take the first error from the first field
                    return validationErrors.entries.firstOrNull()?.value?.firstOrNull() ?: genericErrorResponse.message ?: "Validation error during $operation"
                }
                genericErrorResponse.message ?: "Unknown error during $operation (Code: $errorCode)"
            } catch (e: JsonSyntaxException) {
                Log.e("KYCViewModel", "Error parsing $operation error body as JSON: $errorBody", e)
                errorBody // Fallback to raw error body
            } catch (e: Exception) {
                Log.e("KYCViewModel", "Unexpected error processing $operation error body: $errorBody", e)
                "Error processing $operation server response (Code: $errorCode)."
            }
        } else {
            "Unknown error during $operation (Code: $errorCode)"
        }
    }
}

// Sealed result for KYC Document Verification
sealed class VerifyKYCDocumentResult {
    object Loading : VerifyKYCDocumentResult()
    data class Success(val message: String) : VerifyKYCDocumentResult()
    data class Error(val errorMessage: String) : VerifyKYCDocumentResult()
}
