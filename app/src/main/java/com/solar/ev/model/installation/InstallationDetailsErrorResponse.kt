package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents the 422 validation error response for installation details
data class InstallationDetailsErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: Map<String, List<String>>?
)
