package com.solar.ev.viewModel.kyc

import com.solar.ev.model.kyc.KYCSubmitResponse
import com.solar.ev.model.kyc.KYCUpdateResponse

sealed class KYCSubmitResult {
    object Loading : KYCSubmitResult()
    data class Success(val response: KYCSubmitResponse) : KYCSubmitResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : KYCSubmitResult()
}



sealed class DeleteKYCDocumentResult {
    object Loading : DeleteKYCDocumentResult()
    data class Success(val message: String) : DeleteKYCDocumentResult()
    data class Error(val errorMessage: String, val detailId: String?) : DeleteKYCDocumentResult()
}

