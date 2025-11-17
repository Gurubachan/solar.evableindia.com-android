package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?, // Format: YYYY-MM-DD
    @SerializedName("phone") val phone: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("address_line_1") val addressLine1: String?,
    @SerializedName("address_line_2") val addressLine2: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("email") val email: String?
)
