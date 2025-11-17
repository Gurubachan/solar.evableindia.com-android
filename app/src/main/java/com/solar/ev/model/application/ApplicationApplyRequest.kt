package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationApplyRequest(
    @SerializedName("application_number")
    val applicationNumber: String,

    @SerializedName("applied_date")
    val appliedDate: String // YYYY-MM-DD
)
