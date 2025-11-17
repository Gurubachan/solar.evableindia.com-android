package com.solar.ev.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solar.ev.model.GenericErrorResponse // Assuming GenericErrorResponse is in this model package
import retrofit2.Response
import java.io.IOException

object NetworkUtils {

    /**
     * Parses error responses from Retrofit and extracts a standardized error object.
     */
    fun <T : Any> parseError(response: Response<T>): GenericErrorResponse {
        val errorBody = response.errorBody()?.string()
        val gson = Gson()

        return if (errorBody != null) {
            try {
                // Try to parse into the standard GenericErrorResponse structure
                val type = object : TypeToken<GenericErrorResponse>() {}.type
                var genericErrorResponse = gson.fromJson<GenericErrorResponse>(errorBody, type)

                // If data is an empty list (as in some of your examples), make it null for consistency
                if (genericErrorResponse.data is List<*> && (genericErrorResponse.data as List<*>).isEmpty()) {
                     genericErrorResponse = genericErrorResponse.copy(data = null) // Or emptyMap() if your GenericErrorResponse expects a Map
                }
                // If it's not a map (and not null after the above check), and not an empty list, it might be an unexpected format.
                // Adjust this logic based on how your GenericErrorResponse's data field is typed (e.g. Map<String, List<String>>?)
                else if (genericErrorResponse.data != null && genericErrorResponse.data !is Map<*, *>) {
                    // If data is not a Map, and not null, you might want to log this or handle it
                    // For now, let's assume it's an issue and potentially clear it or add to message
                    genericErrorResponse = genericErrorResponse.copy(data = null, message = genericErrorResponse.message + " (Unexpected error data format)")
                }

                genericErrorResponse
            } catch (e: Exception) {
                // Fallback if parsing into GenericErrorResponse fails
                GenericErrorResponse(false, "Error parsing response: ${e.message}", null)
            }
        } else {
            // Fallback for unknown errors or no error body
            GenericErrorResponse(false, "An unknown error occurred (code: ${response.code()})", null)
        }
    }
}
