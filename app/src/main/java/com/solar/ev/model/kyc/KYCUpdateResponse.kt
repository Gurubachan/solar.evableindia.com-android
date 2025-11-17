package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

data class KYCUpdateResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: KYCUpdateData?
)
