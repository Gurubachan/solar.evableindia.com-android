package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName

data class ApplicationErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: ApplicationErrors?
)

data class ApplicationErrors(
    @SerializedName("name") val name: List<String>?,
    @SerializedName("gender") val gender: List<String>?,
    @SerializedName("dob") val dob: List<String>?,
    @SerializedName("email") val email: List<String>?,
    @SerializedName("contact_number") val contactNumber: List<String>?,
    @SerializedName("address") val address: List<String>?,
    @SerializedName("state") val state: List<String>?,
    @SerializedName("district") val district: List<String>?,
    @SerializedName("pincode") val pincode: List<String>?,
    @SerializedName("photo") val photo: List<String>?
    // Add other fields if they can have specific errors
)
