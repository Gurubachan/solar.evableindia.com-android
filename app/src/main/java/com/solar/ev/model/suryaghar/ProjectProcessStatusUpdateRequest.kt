package com.solar.ev.model.suryaghar

import com.google.gson.annotations.SerializedName

data class ProjectProcessStatusUpdateRequest(
    @SerializedName("status")
    val status: String,
    @SerializedName("remark")
    val remark: String?
)
