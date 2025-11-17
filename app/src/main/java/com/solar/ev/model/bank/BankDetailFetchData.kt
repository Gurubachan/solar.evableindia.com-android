package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

// Represents the "data" object containing a single bank detail for fetch by ID
data class BankDetailFetchData(
    @SerializedName("bank") val bank: BankInfo? // Assuming the key is "bank"
)
