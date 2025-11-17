package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class UploadPhotoRequest(
    @SerializedName("profile_photo") val profilePhotoBase64: String
)