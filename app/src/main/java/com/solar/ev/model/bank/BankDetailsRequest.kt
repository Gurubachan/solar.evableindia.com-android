package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

data class BankDetailsRequest(
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("account_holder_name") val accountHolderName: String,
    @SerializedName("bank_name") val bankName: String,
    @SerializedName("ifsc_code") val ifscCode: String,
    @SerializedName("branch_name") val branchName: String,
    @SerializedName("account_number") val accountNumber: String,
    @SerializedName("account_photo") val accountPhoto: String? // Base64 encoded image string, nullable if optional
)
