package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents the "data" object in the installation details creation success response
data class InstallationDetailsData(
    @SerializedName("installation_id") val installationId: String?
)
