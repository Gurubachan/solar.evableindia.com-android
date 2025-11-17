package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class UploadPhotoResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UploadPhotoData?
)

data class UploadPhotoData(
    @SerializedName("profile_photo") val profilePhoto: String?, // This is the relative path
    @SerializedName("profile_photo_url") val profilePhotoUrl: String?
)