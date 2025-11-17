package com.solar.ev.model.installation

import com.google.gson.annotations.SerializedName

// Represents the "data" object in the installation details fetch response
data class InstallationDetailFetchData(
    @SerializedName("installation") val installation: InstallationInfo?
)
