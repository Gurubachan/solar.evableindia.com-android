package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ProfileData
)

data class ProfileData(
    @SerializedName("user") val user: User
)