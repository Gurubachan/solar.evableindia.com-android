package com.solar.ev.viewModel

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.User // Import User
import com.solar.ev.model.ProfileResponse // Import ProfileResponse
import com.solar.ev.model.UploadPhotoData
import com.solar.ev.model.UploadPhotoRequest
import com.solar.ev.network.RetrofitInstance
import com.solar.ev.sharedPreferences.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStream
import kotlin.io.encoding.Base64

// Result wrapper for API calls
sealed class ProfileResult {
    // Changed to hold User? directly
    data class Success(val user: User?) : ProfileResult()
    data class Error(val message: String, val cause: Exception? = null) : ProfileResult()
    object Loading : ProfileResult()
}

// Define a simple result sealed class for this specific action
sealed class SendVerificationEmailResult {
    object Success : SendVerificationEmailResult()
    data class Error(val message: String) : SendVerificationEmailResult()
    object Loading : SendVerificationEmailResult()
}

sealed class UploadPhotoResult {
    data class Success(val photoData: UploadPhotoData?) : UploadPhotoResult()
    data class Error(val message: String) : UploadPhotoResult()
    object Loading : UploadPhotoResult()
}
class ProfileViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _userProfileResult = MutableLiveData<ProfileResult>()
    val userProfileResult: LiveData<ProfileResult> = _userProfileResult

    fun fetchUserProfile() {
        viewModelScope.launch {
            _userProfileResult.value = ProfileResult.Loading
            val token = sessionManager.getUserToken()
            if (token == null) {
                _userProfileResult.value = ProfileResult.Error("Authentication token not found.")
                return@launch
            }

            try {
                val response = RetrofitInstance.api.getProfile("Bearer $token")
                Log.d("ProfileViewModel", "API Response: $response") // Log the raw response

                if (response.isSuccessful) {
                    val profileResponse: ProfileResponse? = response.body()
                    Log.d("ProfileViewModel", "Response Body: $profileResponse")

                    if (profileResponse != null && profileResponse.status) {
                        // Extract User object
                        val user: User? = profileResponse.data.user
                        _userProfileResult.value = ProfileResult.Success(user)
                        Log.d("ProfileViewModel", "User data: $user")
                    } else {
                        // Handle cases where response.isSuccessful is true, but your app-level status is false
                        _userProfileResult.value = ProfileResult.Error(profileResponse?.message ?: "Profile data not found or status false.")
                        Log.e("ProfileViewModel", "Profile fetch not successful: ${profileResponse?.message}")
                    }
                } else {
                    // Handle unsuccessful HTTP responses (4xx, 5xx)
                    val errorBody = response.errorBody()?.string()
                    _userProfileResult.value = ProfileResult.Error("API error: ${response.code()} - $errorBody")
                    Log.e("ProfileViewModel", "API Error: ${response.code()} - $errorBody")
                }

            } catch (e: IOException) { // Network error
                _userProfileResult.value = ProfileResult.Error("Network error: ${e.message}", e)
                Log.e("ProfileViewModel", "Network error", e)
            } catch (e: HttpException) { // Should be caught by response.isSuccessful check, but good for safety
                val errorBody = e.response()?.errorBody()?.string()
                _userProfileResult.value = ProfileResult.Error("HTTP API error: ${e.code()} - $errorBody", e)
                Log.e("ProfileViewModel", "HttpException", e)
            } catch (e: Exception) { // Other errors
                _userProfileResult.value = ProfileResult.Error("An unexpected error occurred: ${e.message}", e)
                Log.e("ProfileViewModel", "Unexpected error", e)
            }
        }
    }


    private val _sendVerificationEmailStatus = MutableLiveData<SendVerificationEmailResult>()
    val sendVerificationEmailStatus: LiveData<SendVerificationEmailResult> = _sendVerificationEmailStatus

    fun sendVerificationEmail() {
        viewModelScope.launch {
            _sendVerificationEmailStatus.value = SendVerificationEmailResult.Loading
            val token = sessionManager.getUserToken() // Get the auth token
            if (token == null) {
                _sendVerificationEmailStatus.value = SendVerificationEmailResult.Error("User not authenticated.")
                return@launch
            }

            try {
                val response = RetrofitInstance.api.sendVerificationEmail("Bearer $token")
                if (response.isSuccessful && response.body()?.status == true) {
                    _sendVerificationEmailStatus.value = SendVerificationEmailResult.Success
                    Log.d("ProfileViewModel", "Verification email sent: ${response.body()?.message}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = response.body()?.message ?: errorBody ?: "Unknown error"
                    _sendVerificationEmailStatus.value = SendVerificationEmailResult.Error("Failed to send verification email: $message")
                    Log.e("ProfileViewModel", "Failed to send verification email: ${response.code()} - $message")
                }
            } catch (e: IOException) {
                Log.e("ProfileViewModel", "Network error sending verification email", e)
                _sendVerificationEmailStatus.value = SendVerificationEmailResult.Error("Network error: ${e.message}")
            } catch (e: HttpException) {
                Log.e("ProfileViewModel", "HTTP error sending verification email", e)
                _sendVerificationEmailStatus.value = SendVerificationEmailResult.Error("HTTP error: ${e.message}")
            }
            catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sending verification email: ${e.message}", e)
                _sendVerificationEmailStatus.value = SendVerificationEmailResult.Error("Exception: ${e.message}")
            }
        }
    }

    private val _uploadPhotoResult = MutableLiveData<UploadPhotoResult>()
    val uploadPhotoResult: LiveData<UploadPhotoResult> = _uploadPhotoResult

    fun uploadProfilePhoto(base64Image: String) {
        viewModelScope.launch {
            _uploadPhotoResult.value = UploadPhotoResult.Loading
            val token = sessionManager.getUserToken()
            if (token == null) {
                _uploadPhotoResult.value = UploadPhotoResult.Error("User not authenticated.")
                return@launch
            }
            try {
                val request = UploadPhotoRequest(profilePhotoBase64 = "data:image/png;base64,$base64Image")
                val response = RetrofitInstance.api.uploadProfilePhoto("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    _uploadPhotoResult.value = UploadPhotoResult.Success(response.body()?.data)
                    // Successfully uploaded photo, now refresh the whole profile
                    // to get all updated user details including the new photo URL in User object.
                    fetchUserProfile() // Re-fetch the entire profile
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.body()?.message ?: "Upload failed"
                    _uploadPhotoResult.value = UploadPhotoResult.Error(errorMsg)
                    Log.e("ProfileViewModel", "Photo upload API error: $errorMsg, Code: ${response.code()}")
                }
            } catch (e: Exception) {
                _uploadPhotoResult.value = UploadPhotoResult.Error("Exception during photo upload: ${e.message}")
                Log.e("ProfileViewModel", "Photo upload exception", e)
            }
        }
    }


}
