package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

data class DiscomDetailsRequest(
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("discom_name") val discomName: String,
    @SerializedName("consumer_number") val consumerNumber: String,
    @SerializedName("current_load") val currentLoad: Double,
    @SerializedName("latest_electric_bill") val latestElectricBill: String? // Made nullable for updates
)
