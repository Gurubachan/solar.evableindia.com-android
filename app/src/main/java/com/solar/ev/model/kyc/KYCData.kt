package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

// Represents the "data" object in the fetch response which contains "kyc"
data class KYCData(
    @SerializedName("kyc") val kyc: KYCDetails?
)
