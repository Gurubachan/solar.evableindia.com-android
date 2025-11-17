package com.solar.ev.viewModel.discom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.network.ApiService

class DiscomDetailsViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscomDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscomDetailsViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
