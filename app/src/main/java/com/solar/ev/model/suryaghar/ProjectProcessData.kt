package com.solar.ev.model.suryaghar

import com.google.gson.annotations.SerializedName

data class ProjectProcessData(
    @SerializedName("id")
    val id: String,
    @SerializedName("application_id")
    val applicationId: String,
    @SerializedName("project_reference_id")
    val projectReferenceId: String?,
    @SerializedName("applied_date")
    val appliedDate: String?,
    @SerializedName("applied_by")
    val appliedBy: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("remark")
    val remark: String?,
    @SerializedName("acknowledgement")
    val acknowledgement: String?,
    @SerializedName("feasibility")
    val feasibility: String?,
    @SerializedName("nma")
    val nma: String?,
    @SerializedName("etoken")
    val etoken: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("application")
    val application: ApplicationData?,
    @SerializedName("kyc")
    val kyc: KycData?,
    @SerializedName("bank")
    val bank: BankData?,
    @SerializedName("discom")
    val discom: DiscomData?,
    @SerializedName("installation")
    val installation: InstallationData?
)

data class ProjectProcessCreateData(
    @SerializedName("project_id")
    val projectId: String
)

data class ApplicationData(
    @SerializedName("name")
    val name: String?,
    @SerializedName("mobile")
    val mobile: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    @SerializedName("gender")
    val gender: String?
)

data class KycData(
    @SerializedName("aadhar_number")
    val aadharNumber: String?,
    @SerializedName("pan_number")
    val panNumber: String?,
    @SerializedName("aadhar_front_image")
    val aadharFrontImage: String?,
    @SerializedName("aadhar_back_image")
    val aadharBackImage: String?,
    @SerializedName("pan_image")
    val panImage: String?
)

data class BankData(
    @SerializedName("bank_name")
    val bankName: String?,
    @SerializedName("account_number")
    val accountNumber: String?,
    @SerializedName("ifsc_code")
    val ifscCode: String?
)

data class DiscomData(
    @SerializedName("discom_name")
    val discomName: String?,
    @SerializedName("account_number")
    val accountNumber: String?,
    @SerializedName("last_electric_bill")
    val lastElectricBill: String?
)

data class InstallationData(
    @SerializedName("latitude")
    val latitude: String?,
    @SerializedName("longitude")
    val longitude: String?,
    @SerializedName("load_requirement")
    val loadRequirement: String?,
    @SerializedName("installation_area_photo")
    val installationAreaPhoto: String?
)
