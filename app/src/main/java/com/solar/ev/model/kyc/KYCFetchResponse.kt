package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

// Represents the top-level response for fetching KYC documents
data class KYCFetchResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: KYCData?
)
