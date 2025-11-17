package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Represents the overall API response for fetching the list of users grouped by role.
data class UserListResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UserListData?
)
