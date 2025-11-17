package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Request body for updating user's role, login status, and active status by admin
data class UpdateUserManagementRequest(
    @SerializedName("role") val role: String?, // "client" or "agent"
    @SerializedName("login_allowed") val loginAllowed: Boolean?,
    @SerializedName("is_active") val isActive: Boolean? // New field
)
