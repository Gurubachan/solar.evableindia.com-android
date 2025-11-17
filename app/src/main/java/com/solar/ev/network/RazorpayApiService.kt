package com.solar.ev.network

import com.solar.ev.model.external.RazorpayIfscResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface RazorpayApiService {
    @GET("{ifscCode}") // The base URL will be https://ifsc.razorpay.com/
    suspend fun getIfscDetails(@Path("ifscCode") ifscCode: String): Response<RazorpayIfscResponse>
}
