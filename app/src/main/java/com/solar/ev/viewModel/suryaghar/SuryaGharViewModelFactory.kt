package com.solar.ev.viewModel.suryaghar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.network.ApiService

class SuryaGharViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuryaGharViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuryaGharViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
