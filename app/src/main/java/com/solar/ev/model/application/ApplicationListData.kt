package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationListData(
    @SerializedName("applications") val applications: List<ApplicationListItem>?
)
