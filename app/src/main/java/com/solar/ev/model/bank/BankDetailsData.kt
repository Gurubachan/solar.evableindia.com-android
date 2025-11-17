package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

data class BankDetailsData(
    @SerializedName("bank_id") val bankId: String?
)
