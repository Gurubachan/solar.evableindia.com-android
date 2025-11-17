package com.solar.ev.viewModel.discom

import com.solar.ev.model.discom.DiscomDetailsResponse

sealed class DiscomDetailsResult {
    data class Success(val response: DiscomDetailsResponse) : DiscomDetailsResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : DiscomDetailsResult()
    object Loading : DiscomDetailsResult()
}
