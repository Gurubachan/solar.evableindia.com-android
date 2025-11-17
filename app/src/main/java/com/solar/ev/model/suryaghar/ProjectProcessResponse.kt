package com.solar.ev.model.suryaghar

import com.google.gson.annotations.SerializedName

data class ProjectProcessResponse<T>(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: T?
)
