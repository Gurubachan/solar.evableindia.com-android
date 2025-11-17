package com.solar.ev.model.external

import com.google.gson.annotations.SerializedName

// Represents the response from https://ifsc.razorpay.com/{IFSC}
data class RazorpayIfscResponse(
    @SerializedName("SWIFT") val swift: String?,
    @SerializedName("IMPS") val imps: Boolean?,
    @SerializedName("STATE") val state: String?,
    @SerializedName("DISTRICT") val district: String?,
    @SerializedName("MICR") val micr: String?,
    @SerializedName("ADDRESS") val address: String?,
    @SerializedName("NEFT") val neft: Boolean?,
    @SerializedName("CITY") val city: String?,
    @SerializedName("CONTACT") val contact: String?,
    @SerializedName("RTGS") val rtgs: Boolean?,
    @SerializedName("UPI") val upi: Boolean?,
    @SerializedName("CENTRE") val centre: String?,
    @SerializedName("ISO3166") val iso3166: String?,
    @SerializedName("BRANCH") val branch: String?,
    @SerializedName("BANK") val bank: String?,
    @SerializedName("BANKCODE") val bankCode: String?,
    @SerializedName("IFSC") val ifsc: String?
)
