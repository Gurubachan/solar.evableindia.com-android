package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class DashboardStatsData(
    @SerializedName("total_applications") val totalApplications: Int = 0,
    @SerializedName("kyc") val kyc: ModuleStats = ModuleStats(),
    @SerializedName("bank") val bank: ModuleStats = ModuleStats(),
    @SerializedName("discom") val discom: ModuleStats = ModuleStats(),
    @SerializedName("installation") val installation: ModuleStats = ModuleStats(),
    @SerializedName("fully_completed_applications") val fullyCompletedApplications: Int = 0
)

data class ModuleStats(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("verified") val verified: Int = 0
)

// Wrapper for the full API response
data class DashboardStatsResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: DashboardStatsData?
)
