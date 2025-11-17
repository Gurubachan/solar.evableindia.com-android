package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Represents the "data" object in the user list response,
// containing a list of users.
data class UserListData(
    @SerializedName("users") val users: List<UserInfo>?
)
