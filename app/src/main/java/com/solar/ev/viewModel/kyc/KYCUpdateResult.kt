package com.solar.ev.viewModel.kyc

import com.solar.ev.model.kyc.KYCUpdateResponse

sealed class KYCUpdateResult {
    data class Success(val response: KYCUpdateResponse) : KYCUpdateResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : KYCUpdateResult()
    object Loading : KYCUpdateResult()
}
