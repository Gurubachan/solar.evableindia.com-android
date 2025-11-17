package com.solar.ev.model.kyc

import com.google.gson.annotations.SerializedName

// Represents the nested "kyc" object in the fetch response
data class KYCDetails(
    @SerializedName("id") val id: String?,
    @SerializedName("application_id") val applicationId: String?,
    @SerializedName("aadhaar_number") val aadhaarNumber: String?,
    @SerializedName("aadhaar_photo_front") val aadhaarPhotoFront: String?,
    @SerializedName("aadhaar_photo_back") val aadhaarPhotoBack: String?,
    @SerializedName("pan_number") val panNumber: String?,
    @SerializedName("pan_photo") val panPhoto: String?,
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("verification_remark") val verificationRemark: String?,
    @SerializedName("verified_by") val verifiedBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
    // We are omitting the nested "application" object for now,
    // as its fields are not directly needed to pre-fill KYCActivity form.
    // It can be added here if needed later.
)