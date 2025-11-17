package com.solar.ev.model.application

import com.google.gson.annotations.SerializedName
import com.solar.ev.model.bank.BankInfo
import com.solar.ev.model.discom.DiscomInfo
import com.solar.ev.model.installation.InstallationInfo
import com.solar.ev.model.kyc.KYCDetails
import com.solar.ev.model.user.UserInfo

data class ApplicationListItem(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("dob") val dob: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("pincode") val pincode: String?,
    @SerializedName("applied_by") val appliedBy: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("kyc") val kyc: KYCDetails?,
    @SerializedName("bank") val bank: BankInfo?,
    @SerializedName("user") val user: UserInfo?,
    @SerializedName("discom") val discom: DiscomInfo?,
    @SerializedName("installation") val installation: InstallationInfo?
) {
    // Computed property to check if all relevant modules are verified
    val isFullyVerified: Boolean
        get() {
            val kycVerified = kyc?.verificationStatus.equals("verified", ignoreCase = true)
            val bankVerified = bank?.verificationStatus.equals("verified", ignoreCase = true)
            val discomVerified = discom?.verificationStatus.equals("verified", ignoreCase = true)
            val installationVerified = installation?.verificationStatus.equals("verified", ignoreCase = true)
            
            return kycVerified && bankVerified && discomVerified && installationVerified
        }
}
