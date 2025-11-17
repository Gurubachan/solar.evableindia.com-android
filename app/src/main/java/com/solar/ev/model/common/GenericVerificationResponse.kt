package com.solar.ev.model.common

import com.google.gson.annotations.SerializedName

/**
 * A generic response for document verification endpoints.
 * The 'data' field is a Map to handle dynamic ID keys (e.g., bank_id, kyc_id).
 */
data class GenericVerificationResponse(
    @SerializedName("status")
    val status: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: Map<String, String>? // Example: {"bank_id": "uuid"} or {"kyc_id": "uuid"}
)
