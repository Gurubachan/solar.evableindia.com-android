package com.solar.ev.model.quotation

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the server when fetching a list of quotations.
 */
data class QuotationListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<QuotationListItem>?
)

/**
 * Represents a single item in the quotation list.
 */
data class QuotationListItem(
    @SerializedName("id") val id: String,
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("quotation_reference_id") val quotationReferenceId: String?,
    @SerializedName("funding_type") val fundingType: String?,
    @SerializedName("quotation_amount") val quotationAmount: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)
