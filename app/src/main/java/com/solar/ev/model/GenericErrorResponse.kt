package com.solar.ev.model

import com.google.gson.annotations.SerializedName

data class GenericErrorResponse(
    @SerializedName("status")
    val status: Boolean,     // To align with API spec and NetworkUtils fallbacks
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("data")
    val data: Any?,          // This is the field NetworkUtils is looking for; Gson will populate it.
                             // NetworkUtils will then try to cast it to Map<String, List<String>> or handle if it's a List.

    // Existing fields, make them nullable with default null if not always present with status & data
    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null,
    
    @SerializedName("error")
    val error: String? = null
)
