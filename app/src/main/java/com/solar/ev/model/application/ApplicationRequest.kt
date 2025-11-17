package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("gender") val gender: String?, // e.g., "male", "female", "other"
    @SerializedName("dob") val dob: String?, // e.g., "YYYY-MM-DD"
    @SerializedName("email") val email: String?,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("pincode") val pincode: String?,
    @SerializedName("photo") val photo: String? // Changed from "phoho" to "photo" as is conventional
)
