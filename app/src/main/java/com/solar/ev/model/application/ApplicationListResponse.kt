package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationListResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ApplicationListData?
)

