package com.solar.ev.viewModel.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solar.ev.model.user.UserInfo
import com.solar.ev.model.user.UpdateUserManagementRequest
import com.solar.ev.model.user.UserListResponse
import com.solar.ev.model.user.UpdateUserManagementResponse
import com.solar.ev.network.ApiService
import com.solar.ev.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.io.IOException

sealed class UserListResult {
    data class Success(val users: List<UserInfo>) : UserListResult()
    data class Error(val message: String) : UserListResult()
    object Loading : UserListResult()
}

sealed class UserUpdateResult {
    data class Success(val user: UserInfo, val originalMessage: String?) : UserUpdateResult()
    data class Error(val message: String, val userId: String) : UserUpdateResult()
    object Loading : UserUpdateResult()
}

class UserManagementViewModel(private val apiService: ApiService) : ViewModel() {

    private val _userListResult = MutableLiveData<UserListResult>()
    val userListResult: LiveData<UserListResult> = _userListResult

    private val _userUpdateResult = MutableLiveData<UserUpdateResult>()
    val userUpdateResult: LiveData<UserUpdateResult> = _userUpdateResult

    fun fetchUsers(token: String) {
        _userListResult.value = UserListResult.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getUsersGroupedByRole("Bearer $token")
                if (response.isSuccessful) {
                    val users = response.body()?.data?.users ?: emptyList()
                    _userListResult.value = UserListResult.Success(users)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _userListResult.value = UserListResult.Error("Failed to fetch users: ${response.code()} - $errorBody")
                }
            } catch (e: IOException) {
                _userListResult.value = UserListResult.Error("Network error fetching users: ${e.message}")
            } catch (e: Exception) {
                _userListResult.value = UserListResult.Error("Error fetching users: ${e.message}")
            }
        }
    }

    fun updateUser(token: String, userId: String, request: UpdateUserManagementRequest) {
        _userUpdateResult.value = UserUpdateResult.Loading // Can be refined to show loading per item
        viewModelScope.launch {
            try {
                val response = apiService.updateUserManagementDetails("Bearer $token", userId, request)
                if (response.isSuccessful && response.body()?.status == true) {
                    response.body()?.data?.user?.let { // Correctly access .user
                        updatedUser -> _userUpdateResult.value = UserUpdateResult.Success(updatedUser, response.body()?.message)
                    } ?: run {
                         _userUpdateResult.value = UserUpdateResult.Error("User data missing in successful response", userId)
                    }
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                    _userUpdateResult.value = UserUpdateResult.Error("Update failed: $errorMsg", userId)
                }
            } catch (e: IOException) {
                _userUpdateResult.value = UserUpdateResult.Error("Network error updating user: ${e.message}", userId)
            } catch (e: Exception) {
                _userUpdateResult.value = UserUpdateResult.Error("Error updating user: ${e.message}", userId)
            }
        }
    }
}
