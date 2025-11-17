package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

data class KYCUpdateData(
    @SerializedName("kyc_id") val kycId: String?
)
