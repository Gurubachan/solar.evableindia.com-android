package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationApplyResponse(
    @SerializedName("status")
    val status: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: Data?
) {
    data class Data(
        @SerializedName("application_id")
        val applicationId: String,

        @SerializedName("application_number")
        val applicationNumber: String,

        @SerializedName("applied_date")
        val appliedDate: String // YYYY-MM-DD
    )
}
