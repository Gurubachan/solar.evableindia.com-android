package com.solar.ev.model.quotation

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the server when fetching a list of quotations.
 */
data class QuotationListResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<QuotationListItem>?
)

/**
 * Represents a single item in the quotation list.
 */
data class QuotationListItem(
    @SerializedName("id") val id: String,
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("project_process_id") val projectProcessId: String?,
    @SerializedName("quotation_reference_id") val quotationReferenceId: String?,
    @SerializedName("funding_type") val fundingType: String?,
    @SerializedName("quotation_amount") val quotationAmount: String?,
    @SerializedName("quotation_file") val quotationFile: String?,
    @SerializedName("quotation_details") val quotationDetails: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("applied_by") val appliedBy: User?,
    @SerializedName("approved_by") val approvedBy: User?,
    @SerializedName("submitted_at") val submittedAt: String?,
    @SerializedName("approved_at") val approvedAt: String?,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("project_process") val projectProcess: ProjectProcess?
)

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("profile_photo") val profilePhoto: String?
)

class ProjectProcess {
    // Based on the response, this is null, so we have no information about its structure.
}

data class UpdateQuotationRequest(
    @SerializedName("project_process_id") val projectProcessId: String? = null,
    @SerializedName("quotation_reference_id") val quotationReferenceId: String? = null,
    @SerializedName("funding_type") val fundingType: String? = null,
    @SerializedName("quotation_amount") val quotationAmount: String? = null,
    @SerializedName("quotation_file") val quotationFile: String? = null,
    @SerializedName("quotation_details") val quotationDetails: String? = null,
    @SerializedName("remarks") val remarks: String? = null
)

data class QuotationRemarkRequest(
    @SerializedName("remarks") val remarks: String
)
