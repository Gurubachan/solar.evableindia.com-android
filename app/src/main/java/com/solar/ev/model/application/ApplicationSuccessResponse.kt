package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationSuccessResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ApplicationId?
)

data class ApplicationId(
    @SerializedName("application_id") val applicationId: String?
)