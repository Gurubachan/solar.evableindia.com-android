package com.solar.ev.viewModel.signup

import android.app.Application // Added Application import
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.SignupRequest
// import com.solar.ev.model.SignupResponse // Not directly used here, but good to keep if methods return it
import com.solar.ev.network.ApiService
import com.solar.ev.sharedPreferences.SessionManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONException

// 1. Sealed class for UI results/states
sealed class SignupResult {
    data class Success(val message: String) : SignupResult()
    data class Error(val errorMessage: String) : SignupResult()
    object Loading : SignupResult()
}

// 2. Event wrapper for LiveData
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

// 3. SignupViewModel
class SignupViewModel(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
) : ViewModel() {
    companion object {
        private const val TAG = "#SignupViewModel"
    }
    private val _signupResult = MutableLiveData<Event<SignupResult>>()
    val signupResult: LiveData<Event<SignupResult>> = _signupResult

    fun signupUser(signupRequest: SignupRequest) {
        _signupResult.value = Event(SignupResult.Loading)
        viewModelScope.launch {
            try {
                val response = apiService.signupUser(signupRequest)
                if (response.isSuccessful) {
                    val signupBody = response.body()
                    Log.d(TAG, "Attempting to sign up user: $signupRequest")
                    if (signupBody?.status == true && signupBody.data?.token != null && signupBody.data.user?.email != null) {
                        sessionManager.saveAuthToken(signupBody.data.token)
                        sessionManager.saveUserData(signupBody.data.user)

                        _signupResult.value = Event(SignupResult.Success(signupBody.message ?: "Signup Successful! Please login."))
                    } else {
                        val errorMessage = signupBody?.message ?: "Signup failed: Invalid response data from server."
                        _signupResult.value = Event(SignupResult.Error(errorMessage))
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMessage = parseErrorMessage(errorBodyString, response.message())
                    _signupResult.value = Event(SignupResult.Error("API Error: $errorMessage"))
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
                _signupResult.value = Event(SignupResult.Error("Signup failed: ${e.message ?: "Unknown network error"}"))
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?, defaultMessage: String): String {
        if (errorBody.isNullOrEmpty()) {
            return defaultMessage
        }
        return try {
            val errorJson = JSONObject(errorBody)
            errorJson.getString("message")
        } catch (e: JSONException) {
            errorBody
        }
    }
}

// 4. ViewModelFactory to provide dependencies to SignupViewModel
@Suppress("UNCHECKED_CAST")
class SignupViewModelFactory(
    private val application: Application, // Changed from SessionManager
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
            // Create SessionManager instance here
            val sessionManager = SessionManager(application.applicationContext)
            return SignupViewModel(apiService, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
