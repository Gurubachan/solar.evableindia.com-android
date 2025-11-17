package com.solar.ev.viewModel.bank

import com.solar.ev.model.bank.BankDetailsResponse
import com.solar.ev.model.bank.BankDetailsUpdateResponse

sealed class BankDetailsResult {
    data class Success(val response: BankDetailsResponse) : BankDetailsResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : BankDetailsResult()
    object Loading : BankDetailsResult()
}


sealed class DeleteBankDetailResult {
    object Loading : DeleteBankDetailResult()
    data class Success(val message: String) : DeleteBankDetailResult()
    data class Error(val errorMessage: String, val detailId: String?) : DeleteBankDetailResult()
}

