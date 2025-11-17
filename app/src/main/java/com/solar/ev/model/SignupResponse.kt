package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class SignupResponse(
    @SerializedName("status")
    val status: Boolean?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: ResponseData?
)

data class ResponseData(
    @SerializedName("token")
    val token: String?,
    @SerializedName("user")
    val user: User
)

