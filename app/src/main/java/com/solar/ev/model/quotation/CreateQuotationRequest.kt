package com.solar.ev.model.quotation

import com.google.gson.annotations.SerializedName

data class CreateQuotationRequest(
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("project_process_id") val projectProcessId: String? = null,
    @SerializedName("quotation_reference_id") val quotationReferenceId: String,
    @SerializedName("funding_type") val fundingType: String,
    @SerializedName("quotation_amount") val quotationAmount: Double?,
    @SerializedName("quotation_file") val quotationFile: String?,
    @SerializedName("quotation_details") val quotationDetails: String,
    @SerializedName("remarks") val remarks: String?
)
