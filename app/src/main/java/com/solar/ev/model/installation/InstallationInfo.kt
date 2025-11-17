package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents the "installation" object in the fetch response
data class InstallationInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("application_id") val applicationId: String?,
    @SerializedName("capacity_kw") val capacityKw: String?, // API returns as String, can be converted to Double if needed
    @SerializedName("system_type") val systemType: String?,
    @SerializedName("latitude") val latitude: String?, // API returns as String, can be converted to Double if needed
    @SerializedName("longitude") val longitude: String?, // API returns as String, can be converted to Double if needed
    @SerializedName("installation_location") val installationLocation: String?, // Relative path to the image
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("verification_remark") val verificationRemark: String?,
    @SerializedName("verified_by") val verifiedBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
