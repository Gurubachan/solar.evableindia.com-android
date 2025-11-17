package com.solar.ev.viewModel.bank

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.solar.ev.model.DetailDeleteResponse
import com.solar.ev.model.GenericErrorResponse
import com.solar.ev.model.bank.BankDetailsRequest
import com.solar.ev.model.bank.BankInfo
import com.solar.ev.model.common.DocumentVerificationRequest // New import
import com.solar.ev.model.common.GenericVerificationResponse // New import
import com.solar.ev.model.external.RazorpayIfscResponse
import com.solar.ev.network.ApiService
import com.solar.ev.network.RazorpayApiService
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BankDetailsViewModel(
    private val apiService: ApiService,
    private val razorpayApiService: RazorpayApiService
) : ViewModel() {

    // For submitting/creating bank details
    private val _bankDetailsResult = MutableLiveData<BankDetailsResult>()
    val bankDetailsResult: LiveData<BankDetailsResult> = _bankDetailsResult

    // For updating bank details
    private val _bankUpdateResult = MutableLiveData<BankDetailsUpdateResult>()
    val bankUpdateResult: LiveData<BankDetailsUpdateResult> = _bankUpdateResult

    // For fetching existing bank details
    private val _bankDetail = MutableLiveData<BankInfo?>()
    val bankDetail: LiveData<BankInfo?> = _bankDetail

    private val _fetchBankDetailError = MutableLiveData<String?>()
    val fetchBankDetailError: LiveData<String?> = _fetchBankDetailError

    private val _isFetchingBankDetail = MutableLiveData<Boolean>()
    val isFetchingBankDetail: LiveData<Boolean> = _isFetchingBankDetail

    // For fetching IFSC details from Razorpay
    private val _ifscDetails = MutableLiveData<RazorpayIfscResponse?>()
    val ifscDetails: LiveData<RazorpayIfscResponse?> = _ifscDetails

    private val _ifscFetchError = MutableLiveData<String?>()
    val ifscFetchError: LiveData<String?> = _ifscFetchError

    private val _isFetchingIfsc = MutableLiveData<Boolean>()
    val isFetchingIfsc: LiveData<Boolean> = _isFetchingIfsc

    // For deleting bank details
    private val _deleteBankDetailResult = MutableLiveData<DeleteBankDetailResult>()
    val deleteBankDetailResult: LiveData<DeleteBankDetailResult> = _deleteBankDetailResult

    // For verifying bank details
    private val _verifyBankDetailResult = MutableLiveData<VerifyBankDetailResult>()
    val verifyBankDetailResult: LiveData<VerifyBankDetailResult> = _verifyBankDetailResult

    fun submitBankDetails(token: String, request: BankDetailsRequest) {
        _bankDetailsResult.value = BankDetailsResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.submitBankDetails("Bearer $token", request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _bankDetailsResult.postValue(BankDetailsResult.Success(it))
                    } ?: _bankDetailsResult.postValue(BankDetailsResult.Error("Empty response body for submit"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseGenericErrorBody(errorBody, response.code(), "submit bank details")
                    _bankDetailsResult.postValue(BankDetailsResult.Error(errorMessage))
                }
            } catch (e: HttpException) {
                _bankDetailsResult.postValue(BankDetailsResult.Error("Network error: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _bankDetailsResult.postValue(BankDetailsResult.Error("IO error: ${e.message}"))
            } catch (e: Exception) {
                _bankDetailsResult.postValue(BankDetailsResult.Error("An unexpected error occurred: ${e.message}"))
            }
        }
    }

    fun updateBankDetail(token: String, bankId: String, request: BankDetailsRequest) {
        _bankUpdateResult.value = BankDetailsUpdateResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateBankDetail("Bearer $token", bankId, request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _bankUpdateResult.postValue(BankDetailsUpdateResult.Success(it))
                    } ?: _bankUpdateResult.postValue(BankDetailsUpdateResult.Error("Empty response body for update"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseGenericErrorBody(errorBody, response.code(), "update bank details")
                    _bankUpdateResult.postValue(BankDetailsUpdateResult.Error(errorMessage))
                }
            } catch (e: HttpException) {
                _bankUpdateResult.postValue(BankDetailsUpdateResult.Error("Network error during update: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _bankUpdateResult.postValue(BankDetailsUpdateResult.Error("IO error during update: ${e.message}"))
            } catch (e: Exception) {
                _bankUpdateResult.postValue(BankDetailsUpdateResult.Error("An unexpected error occurred during update: ${e.message}"))
            }
        }
    }

    fun fetchBankDetail(token: String, bankId: String) {
        _isFetchingBankDetail.value = true
        _fetchBankDetailError.value = null
        _bankDetail.value = null
        viewModelScope.launch {
            try {
                val response = apiService.getBankDetail("Bearer $token", bankId)
                if (response.isSuccessful) {
                    val fetchedBankInfo = response.body()?.data?.bank
                    if (fetchedBankInfo != null) {
                        _bankDetail.postValue(fetchedBankInfo)
                    } else {
                        _bankDetail.postValue(null) 
                    }
                } else {
                    if (response.code() == 404) {
                        _bankDetail.postValue(null) 
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = parseGenericErrorBody(errorBody, response.code(), "fetch bank detail")
                        _fetchBankDetailError.postValue(errorMessage)
                    }
                }
            } catch (e: HttpException) {
                _fetchBankDetailError.postValue("Network error fetching bank detail: ${e.message()} (${e.code()})")
            } catch (e: IOException) {
                _fetchBankDetailError.postValue("IO error fetching bank detail: ${e.message}")
            } catch (e: Exception) {
                _fetchBankDetailError.postValue("An unexpected error occurred fetching bank detail: ${e.message}")
            }
            finally {
                _isFetchingBankDetail.postValue(false)
            }
        }
    }

    fun deleteBankDetailById(token: String, bankDetailId: String) {
        _deleteBankDetailResult.value = DeleteBankDetailResult.Loading
        viewModelScope.launch {
            try {
                val response: Response<DetailDeleteResponse> = apiService.deleteBankDetail("Bearer $token", bankDetailId)
                if (response.isSuccessful && response.body()?.status == true) {
                    _deleteBankDetailResult.postValue(DeleteBankDetailResult.Success(response.body()?.message ?: "Bank details deleted successfully."))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseGenericErrorBody(errorBody, response.code(), "delete bank detail")
                        else -> "Failed to delete bank details (Code: ${response.code()})"
                    }
                    _deleteBankDetailResult.postValue(DeleteBankDetailResult.Error(errorMessage, bankDetailId))
                    Log.e("BankDetailsVM", "Error deleting bank detail $bankDetailId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _deleteBankDetailResult.postValue(DeleteBankDetailResult.Error("Network error deleting: ${e.message()} (${e.code()})", bankDetailId))
            } catch (e: IOException) {
                _deleteBankDetailResult.postValue(DeleteBankDetailResult.Error("IO error deleting: ${e.message}", bankDetailId))
            } catch (e: Exception) {
                _deleteBankDetailResult.postValue(DeleteBankDetailResult.Error("Unexpected error deleting: ${e.message}", bankDetailId))
            }
        }
    }

    fun verifyBankDetailDocument(token: String, bankId: String, request: DocumentVerificationRequest) {
        _verifyBankDetailResult.value = VerifyBankDetailResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.verifyBankDetail("Bearer $token", bankId, request)
                if (response.isSuccessful && response.body()?.status == true) {
                    _verifyBankDetailResult.postValue(VerifyBankDetailResult.Success(response.body()!!.message))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val responseMessage = response.body()?.message
                    val errorMessage = when {
                        response.body()?.status == false && !responseMessage.isNullOrEmpty() -> responseMessage
                        !errorBody.isNullOrEmpty() -> parseVerificationErrorBody(errorBody, response.code())
                        else -> "Failed to verify bank details (Code: ${response.code()})"
                    }
                    _verifyBankDetailResult.postValue(VerifyBankDetailResult.Error(errorMessage))
                    Log.e("BankDetailsVM", "Error verifying bank detail $bankId: $errorMessage - Raw: $errorBody")
                }
            } catch (e: HttpException) {
                _verifyBankDetailResult.postValue(VerifyBankDetailResult.Error("Network error verifying: ${e.message()} (${e.code()})"))
            } catch (e: IOException) {
                _verifyBankDetailResult.postValue(VerifyBankDetailResult.Error("IO error verifying: ${e.message}"))
            } catch (e: Exception) {
                _verifyBankDetailResult.postValue(VerifyBankDetailResult.Error("Unexpected error verifying: ${e.message}"))
            }
        }
    }

    fun fetchIfscDetails(ifscCode: String) {
        _isFetchingIfsc.value = true
        _ifscFetchError.value = null
        _ifscDetails.value = null 
        viewModelScope.launch {
            try {
                val response = razorpayApiService.getIfscDetails(ifscCode)
                if (response.isSuccessful) {
                    _ifscDetails.postValue(response.body())
                } else {
                    if (response.code() == 404) {
                        _ifscFetchError.postValue("Invalid IFSC code. Please check and try again.")
                    } else {
                         val errorBody = response.errorBody()?.string()
                         val errorMessage = if (errorBody != null && errorBody.contains("message")) {
                             try {
                                 val genericError = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                                 genericError.message ?: "Failed to fetch IFSC (Code: ${response.code()}) - $errorBody"
                             } catch (e: JsonSyntaxException) {
                                 "Error parsing IFSC error (Code: ${response.code()}) - $errorBody"
                             }
                         } else {
                            "Failed to fetch IFSC (Code: ${response.code()}) - $errorBody"
                         }                        
                        _ifscFetchError.postValue(errorMessage)
                    }
                }
            } catch (e: HttpException) {
                _ifscFetchError.postValue("Network error fetching IFSC: ${e.message()} (${e.code()})")
            } catch (e: IOException) {
                _ifscFetchError.postValue("IO error fetching IFSC: ${e.message}")
            } catch (e: Exception) {
                _ifscFetchError.postValue("An unexpected error occurred fetching IFSC: ${e.message}")
            }
            finally {
                _isFetchingIfsc.postValue(false)
            }
        }
    }

    private fun parseGenericErrorBody(errorBody: String?, errorCode: Int, operation: String): String {
        return if (errorBody != null) {
            try {
                val genericErrorResponse = Gson().fromJson(errorBody, GenericErrorResponse::class.java)
                genericErrorResponse.message ?: "Unknown error during $operation (Code: $errorCode)"
            } catch (e: JsonSyntaxException) {
                Log.e("BankDetailsVM", "Error parsing $operation error body as JSON: $errorBody", e)
                errorBody 
            } catch (e: Exception) {
                Log.e("BankDetailsVM", "Unexpected error processing $operation error body: $errorBody", e)
                "Error processing $operation server response (Code: $errorCode)."
            }
        } else {
            "Unknown error during $operation (Code: $errorCode)"
        }
    }

    private fun parseVerificationErrorBody(errorBody: String?, errorCode: Int): String {
        // Assuming GenericVerificationResponse for errors too, or a similar structure.
        // If validation errors have a specific structure like {"message": "Error", "errors": {...}}, this needs adjustment.
        // For now, using parseGenericErrorBody. Adapt if specific parsing for validation errors is needed.
        return parseGenericErrorBody(errorBody, errorCode, "document verification")
    }
}

// Sealed result for Bank Detail Verification
sealed class VerifyBankDetailResult {
    object Loading : VerifyBankDetailResult()
    data class Success(val message: String) : VerifyBankDetailResult()
    data class Error(val errorMessage: String) : VerifyBankDetailResult()
}
