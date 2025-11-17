package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents the overall success response for fetching installation details
data class InstallationDetailFetchResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: InstallationDetailFetchData?
)
