package com.solar.ev.viewModel.application


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.network.ApiService

class MyApplicationsViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyApplicationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyApplicationsViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}