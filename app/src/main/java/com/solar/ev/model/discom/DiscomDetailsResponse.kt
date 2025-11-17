package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

data class DiscomDetailsResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: DiscomDetailsData?
)
