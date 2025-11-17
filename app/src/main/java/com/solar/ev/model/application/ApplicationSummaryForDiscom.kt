package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

// Represents the nested "application" object within the fetched DiscomInfo response
data class ApplicationSummaryForDiscom(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("status") val status: String?
)
