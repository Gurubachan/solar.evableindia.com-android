package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

data class DiscomDetailsData(
    @SerializedName("discom_id") val discomId: String?
)
