package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

data class DiscomDetailFetchData(
    @SerializedName("discom") val discom: DiscomInfo?
)
