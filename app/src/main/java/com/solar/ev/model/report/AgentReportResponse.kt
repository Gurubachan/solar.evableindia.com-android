package com.solar.ev.model.report

import com.google.gson.annotations.SerializedName

data class AgentReportResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<AgentReportItem>?
)
