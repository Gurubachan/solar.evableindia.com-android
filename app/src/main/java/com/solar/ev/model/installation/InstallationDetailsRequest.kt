package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

data class InstallationDetailsRequest(
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("capacity_kw") val capacityKw: Double,
    @SerializedName("system_type") val systemType: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("installation_location") val installationLocation: String? // Base64 encoded image data URI, nullable for updates
)
