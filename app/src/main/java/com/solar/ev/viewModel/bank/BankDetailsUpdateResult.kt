package com.solar.ev.viewModel.bank

import com.solar.ev.model.bank.BankDetailsUpdateResponse

sealed class BankDetailsUpdateResult {
    data class Success(val response: BankDetailsUpdateResponse) : BankDetailsUpdateResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : BankDetailsUpdateResult()
    object Loading : BankDetailsUpdateResult()
}
