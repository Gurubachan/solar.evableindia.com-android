package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class DeleteApplicationResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<Any>? // Or Unit if data is always an empty list and not used
)