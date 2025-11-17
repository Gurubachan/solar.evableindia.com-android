package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Response for updating user's role, login status, and active status by admin
data class UpdateUserManagementResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserManagementUpdateData? // Updated to use UserManagementUpdateData
)
