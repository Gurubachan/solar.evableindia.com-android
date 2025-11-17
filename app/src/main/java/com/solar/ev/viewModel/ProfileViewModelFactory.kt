package com.solar.ev.viewModel // Or your viewModel package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.sharedPreferences.SessionManager

class ProfileViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}