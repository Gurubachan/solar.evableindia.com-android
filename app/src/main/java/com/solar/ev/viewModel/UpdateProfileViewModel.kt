package com.solar.ev.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solar.ev.model.ProfileResponse
import com.solar.ev.model.UpdateProfileRequest
import com.solar.ev.model.User
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Result wrapper for Update Profile API calls
sealed class UpdateProfileResult {
    data class Success(val user: User?) : UpdateProfileResult() // Return updated user
    data class Error(val message: String, val errors: Map<String, List<String>>? = null) : UpdateProfileResult()
    object Loading : UpdateProfileResult()
}

// For parsing the specific error response
data class ApiErrorResponse(
    val message: String?,
    val errors: Map<String, List<String>>?
)

class UpdateProfileViewModelFactory(private val sessionManager: SessionManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdateProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UpdateProfileViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class UpdateProfileViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _updateProfileResult = MutableLiveData<UpdateProfileResult>()
    val updateProfileResult: LiveData<UpdateProfileResult> = _updateProfileResult

    fun updateProfile(updateProfileRequest: UpdateProfileRequest) {
        viewModelScope.launch {
            _updateProfileResult.value = UpdateProfileResult.Loading
            val token = sessionManager.getUserToken()
            if (token == null) {
                _updateProfileResult.value = UpdateProfileResult.Error("Authentication token not found.")
                return@launch
            }

            try {
                val response = RetrofitInstance.api.updateProfile("Bearer $token", updateProfileRequest)
                Log.d("UpdateProfileVM", "API Response: $response")

                if (response.isSuccessful) {
                    val profileResponse: ProfileResponse? = response.body()
                    Log.d("UpdateProfileVM", "Response Body: $profileResponse")
                    if (profileResponse != null && profileResponse.status) {
                        _updateProfileResult.value = UpdateProfileResult.Success(profileResponse.data.user)
                    } else {
                        // Backend indicates success in HTTP, but logical error in response body
                        _updateProfileResult.value = UpdateProfileResult.Error(
                            profileResponse?.message ?: "Failed to update profile."
                        )
                    }
                } else {
                    // HTTP error (4xx, 5xx)
                    val errorBodyString = response.errorBody()?.string()
                    Log.e("UpdateProfileVM", "API Error Body: $errorBodyString")
                    if (errorBodyString != null) {
                        try {
                            val gson = Gson()
                            val errorResponseType = object : TypeToken<ApiErrorResponse>() {}.type
                            val apiErrorResponse: ApiErrorResponse? = gson.fromJson(errorBodyString, errorResponseType)
                            _updateProfileResult.value = UpdateProfileResult.Error(
                                apiErrorResponse?.message ?: "An error occurred.",
                                apiErrorResponse?.errors
                            )
                        } catch (e: Exception) {
                            // JSON parsing error
                            _updateProfileResult.value = UpdateProfileResult.Error(
                                "Error parsing error response: ${e.message}"
                            )
                        }
                    } else {
                        _updateProfileResult.value = UpdateProfileResult.Error(
                            "API error: ${response.code()} - Unknown error"
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e("UpdateProfileVM", "Network error", e)
                _updateProfileResult.value = UpdateProfileResult.Error("Network error: ${e.message}")
            } catch (e: HttpException) { // Should be caught by response.isSuccessful, but good for safety
                Log.e("UpdateProfileVM", "HTTP Exception", e)
                _updateProfileResult.value = UpdateProfileResult.Error("HTTP error: ${e.message}")
            }
            catch (e: Exception) {
                Log.e("UpdateProfileVM", "Unexpected error", e)
                _updateProfileResult.value = UpdateProfileResult.Error("An unexpected error occurred: ${e.message}")
            }
        }
    }
}
