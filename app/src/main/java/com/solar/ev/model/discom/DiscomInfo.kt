package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

// Represents the "discom" object, potentially nested or as a primary response object
data class DiscomInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("application_id") val applicationId: String?,
    @SerializedName("discom_name") val discomName: String?,
    @SerializedName("consumer_number") val consumerNumber: String?,
    @SerializedName("current_load") val currentLoad: String?, // API returns as String, can be converted to Double if needed
    @SerializedName("latest_electric_bill") val latestElectricBill: String?, // Relative path to the PDF
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("verification_remark") val verificationRemark: String?,
    @SerializedName("verified_by") val verifiedBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
