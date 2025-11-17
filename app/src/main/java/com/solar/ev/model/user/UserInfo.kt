package com.solar.ev.model.user

import com.google.gson.annotations.SerializedName

// Represents the "user" object nested within the Application details fetch response
data class UserInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("email_verified_at") val emailVerifiedAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("profile_photo") val profilePhoto: String?,
    @SerializedName("address_line_1") val addressLine1: String?,
    @SerializedName("address_line_2") val addressLine2: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("login_allowed") val loginAllowed: Boolean?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("profile_completed") val profileCompleted: Boolean?
)
