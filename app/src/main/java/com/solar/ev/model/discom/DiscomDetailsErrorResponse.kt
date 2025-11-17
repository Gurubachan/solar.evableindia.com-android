package com.solar.ev.model.discom

import com.google.gson.annotations.SerializedName

/**
 * Represents the error response structure for Discom Details submission.
 * e.g.:
 * {
 *     "message": "The application id field is required. (and 4 more errors)",
 *     "errors": {
 *         "application_id": ["The application id field is required."],
 *         "discom_name": ["The discom name field is required."],
 *         // ... and so on
 *     }
 * }
 */
data class DiscomDetailsErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: Map<String, List<String>>?
)
