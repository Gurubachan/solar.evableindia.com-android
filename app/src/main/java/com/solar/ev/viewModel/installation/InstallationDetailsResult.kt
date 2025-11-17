package com.solar.ev.viewModel.installation

import com.solar.ev.model.installation.InstallationDetailsErrorResponse
import com.solar.ev.model.installation.InstallationDetailsResponse

sealed class InstallationDetailsResult {
    object Loading : InstallationDetailsResult()
    data class Success(val response: InstallationDetailsResponse) : InstallationDetailsResult()
    data class Error(val errorMessage: String, val errors: Map<String, List<String>>? = null) : InstallationDetailsResult() {
        constructor(message: String, errorResponse: InstallationDetailsErrorResponse?) : this(message, errorResponse?.errors)
    }
}
