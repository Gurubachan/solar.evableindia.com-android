package com.solar.ev.model.suryaghar

import com.google.gson.annotations.SerializedName

data class ProjectProcessCreateRequest(
    @SerializedName("application_id")
    val applicationId: String,
    @SerializedName("project_reference_id")
    val projectReferenceId: String?,
    @SerializedName("applied_date")
    val appliedDate: String?, // Format: "YYYY-MM-DDTHH:mm:ss"
    @SerializedName("applied_by")
    val appliedBy: String?, // UUID of user
    @SerializedName("status")
    val status: String?,
    @SerializedName("remark")
    val remark: String?,
    @SerializedName("acknowledgement")
    val acknowledgement: String?, // Base64 encoded string
    @SerializedName("feasibility")
    val feasibility: String?,     // Base64 encoded string
    @SerializedName("nma")
    val nma: String?,             // Base64 encoded string
    @SerializedName("etoken")
    val etoken: String?           // Base64 encoded string
)
