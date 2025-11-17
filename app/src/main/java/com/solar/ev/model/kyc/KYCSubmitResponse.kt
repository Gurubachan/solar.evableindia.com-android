package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

data class KYCSubmitResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: KYCUpdateData // Can be null or a specific data object if API returns one
)
