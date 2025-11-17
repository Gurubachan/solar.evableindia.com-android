package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Represents the nested "data" object within the UserManagementUpdateResponse,
// which in turn contains the "user" object.
data class UserManagementUpdateData(
    @SerializedName("user") val user: UserInfo?
)
