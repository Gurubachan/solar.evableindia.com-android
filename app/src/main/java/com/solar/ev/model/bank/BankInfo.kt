package com.solar.ev.model.bank

import com.google.gson.annotations.SerializedName

// Represents the "bank" object, potentially nested or as a primary response object
data class BankInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("application_id") val applicationId: String?,
    @SerializedName("account_holder_name") val accountHolderName: String?,
    @SerializedName("bank_name") val bankName: String?,
    @SerializedName("ifsc_code") val ifscCode: String?,
    @SerializedName("branch_name") val branchName: String?,
    @SerializedName("account_number") val accountNumber: String?,
    @SerializedName("account_photo") val accountPhoto: String?, // Relative path to the image
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("verification_remark") val verificationRemark: String?,
    @SerializedName("verified_by") val verifiedBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
