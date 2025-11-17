package com.solar.ev.viewModel.installation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.network.ApiService

class InstallationDetailsViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InstallationDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InstallationDetailsViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
