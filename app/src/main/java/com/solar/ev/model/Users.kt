package com.solar.ev.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName // Import SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.Date
@Parcelize
data class User(
    @SerializedName("address_line_1") val address_line_1: String?,
    @SerializedName("address_line_2") val address_line_2: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("created_at") val created_at: Date?,
    @SerializedName("date_of_birth") val date_of_birth: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("email_verified_at") val email_verified_at: Date?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("id") val id: String, // Assuming ID is non-null & primary identifier
    @SerializedName("is_active") val is_active: Boolean?,
    @SerializedName("login_allowed") val login_allowed: Boolean?,
    @SerializedName("name") val name: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("postal_code") val postal_code: String?,
    @SerializedName("profile_completed") val profile_completed: Boolean?,
    @SerializedName("profile_photo") val profile_photo: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("updated_at") val updated_at: Date?
): Parcelable
