package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

// Represents the "data" object in the bank detail update response
data class BankDetailsUpdateData(
    @SerializedName("bank_id") val bankId: String? // Assuming bank_id is returned
)
