package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

data class KYCSubmitRequest(
    @SerializedName("application_id") val applicationId: String?,
    @SerializedName("aadhaar_number") val aadhaarNumber: String,
    @SerializedName("aadhaar_photo_front") val aadhaarPhotoFront: String?, // Base64 encoded string with data URI prefix
    @SerializedName("aadhaar_photo_back") val aadhaarPhotoBack: String?,  // Base64 encoded string with data URI prefix
    @SerializedName("pan_number") val panNumber: String,
    @SerializedName("pan_photo") val panPhoto: String?                 // Base64 encoded string with data URI prefix
)
