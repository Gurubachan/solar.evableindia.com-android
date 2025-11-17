package com.solar.ev.viewModel.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.solar.ev.model.ApiErrorResponse
import com.solar.ev.model.LoginRequest
import com.solar.ev.model.LoginResponse
import com.solar.ev.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// To represent the different states of the login process
sealed class LoginResult {
    data class Success(val response: LoginResponse) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}
class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun performLogin(email: String, password: String) {
        _loginResult.value = LoginResult.Loading // Notify UI that loading has started
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.loginUser(LoginRequest(email, password))

                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.status) { // Check your API's success condition
                            _loginResult.postValue(LoginResult.Success(it))
                        } else {
                            _loginResult.postValue(LoginResult.Error(it.message ?: "Login failed: Invalid response"))
                        }
                    } ?: run {
                        _loginResult.postValue(LoginResult.Error("Login failed: Empty response body"))
                    }
                } else {
                    // Handle HTTP errors (4xx, 5xx)
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            // Try to parse a structured error response
                            val apiError = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                            apiError.message ?: apiError.errors.message ?: "Error: ${response.code()} ${response.message()}"
                        } catch (e: Exception) {
                            "Error: ${response.code()} ${response.message()} - Invalid error format"
                        }
                    } else {
                        "Error: ${response.code()} ${response.message()}"
                    }
                    _loginResult.postValue(LoginResult.Error(errorMessage))
                }
            } catch (e: HttpException) {
                // HTTP exception (non-2xx responses that Retrofit couldn't handle as 'successful')
                _loginResult.postValue(LoginResult.Error("Network error: ${e.message()}"))
            } catch (e: IOException) {
                // Network error (no internet, server down, etc.)
                _loginResult.postValue(LoginResult.Error("Network error: Please check your connection."))
            } catch (e: Exception) {
                // Other unexpected errors
                _loginResult.postValue(LoginResult.Error("An unexpected error occurred: ${e.message}"))
            }
        }
    }


}