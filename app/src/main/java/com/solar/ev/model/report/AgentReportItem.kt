package com.solar.ev.model.report

import com.google.gson.annotations.SerializedName

data class AgentReportItem(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_role")
    val userRole: String,
    @SerializedName("applications_count")
    val applicationsCount: Int,
    @SerializedName("kyc_count")
    val kycCount: Int,
    @SerializedName("bank_count")
    val bankCount: Int,
    @SerializedName("discom_count")
    val discomCount: Int,
    @SerializedName("installation_count")
    val installationCount: Int
)
