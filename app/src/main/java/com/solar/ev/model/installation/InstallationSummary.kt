package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents a summary of installation details, potentially nested in ApplicationListItem
data class InstallationSummary(
    @SerializedName("id") val id: String?
    // Add other summary fields if your API provides them here
)
