package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

data class BankDetailsResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: BankDetailsData?
)
