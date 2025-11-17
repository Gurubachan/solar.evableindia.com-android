package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

// Wrapper for the "data" object in the JSON response
data class ApplicationData(
    @SerializedName("application") val application: ApplicationListItem?
    // ApplicationListItem will define the actual application details
)

data class ApplicationDetailResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ApplicationData? // Changed from ApplicationListItem?
)