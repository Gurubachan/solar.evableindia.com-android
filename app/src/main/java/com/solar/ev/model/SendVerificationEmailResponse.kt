package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class SendVerificationEmailResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<Any>? // Or Unit if always empty and not needed
)