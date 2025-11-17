package com.solar.ev.model.suryaghar

import com.google.gson.annotations.SerializedName

data class ProjectProcessUpdateRequest(
    @SerializedName("project_reference_id")
    val projectReferenceId: String?,
    @SerializedName("applied_date")
    val appliedDate: String?, // Format: "YYYY-MM-DDTHH:mm:ss"
    @SerializedName("status")
    val status: String?,
    @SerializedName("remark")
    val remark: String?,
    @SerializedName("acknowledgement")
    val acknowledgement: String?, // Base64 encoded string (optional if not updating)
    @SerializedName("feasibility")
    val feasibility: String?,     // Base64 encoded string (optional)
    @SerializedName("nma")
    val nma: String?,             // Base64 encoded string (optional)
    @SerializedName("etoken")
    val etoken: String?           // Base64 encoded string (optional)
)
