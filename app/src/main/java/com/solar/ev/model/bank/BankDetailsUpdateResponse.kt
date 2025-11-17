package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

// Represents the overall API response for updating bank details
data class BankDetailsUpdateResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: BankDetailsUpdateData?
)
