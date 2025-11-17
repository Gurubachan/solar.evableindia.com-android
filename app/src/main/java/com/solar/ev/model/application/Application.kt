package com.solar.ev.model.application

import com.solar.ev.model.bank.BankInfo
import com.solar.ev.model.discom.DiscomInfo
import com.solar.ev.model.installation.InstallationInfo
import com.solar.ev.model.kyc.KYCDetails
import com.solar.ev.model.user.UserInfo

// This class is assumed to be the data source from the ViewModel for the application list.
// Its fields are aligned to be a source for ApplicationListItem.
data class Application(
    val id: String?, // Changed from applicationId
    val name: String?, // Changed from applicantName
    val gender: String?,
    val dob: String?,
    val email: String?,
    val contactNumber: String?, // Original: val contactNumber: String
    val address: String?,
    val state: String?,
    val district: String?,
    val pincode: String?,
    val appliedBy: String?,
    val status: String?,
    val photo: String?,
    val createdAt: String?, // Original: val createdAt: String
    val updatedAt: String?,
    // Assuming these nullable complex types are part of the full Application model
    val kyc: KYCDetails? = null,
    val bank: BankInfo? = null,
    val user: UserInfo? = null,
    val discom: DiscomInfo? = null,
    val installation: InstallationInfo? = null
)
