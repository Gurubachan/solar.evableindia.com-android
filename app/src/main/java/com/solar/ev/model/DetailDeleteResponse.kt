package com.solar.ev.model

import com.google.gson.annotations.SerializedName

/**
 * Generic response for deleting details (Bank, KYC, Discom, Installation).
 */
data class DetailDeleteResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<Any>? // Or Unit if data is always an empty list and not used
)
