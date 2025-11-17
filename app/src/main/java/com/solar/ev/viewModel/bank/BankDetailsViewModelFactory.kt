package com.solar.ev.viewModel.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.solar.ev.network.ApiService
import com.solar.ev.network.RazorpayApiService // Added import

class BankDetailsViewModelFactory(
    private val apiService: ApiService,
    private val razorpayApiService: RazorpayApiService // Added RazorpayApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BankDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass both services to the ViewModel constructor
            return BankDetailsViewModel(apiService, razorpayApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
