package com.solar.ev.model.common

import com.google.gson.annotations.SerializedName

data class DocumentVerificationRequest(
    @SerializedName("verification_status")
    val verificationStatus: String, // pending, verified, rejected, objection

    @SerializedName("verification_remark")
    val verificationRemark: String?
)
