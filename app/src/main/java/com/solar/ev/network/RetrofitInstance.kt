package com.solar.ev.network

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    // Base URL for your primary API
    private const val BASE_URL_SOLAR_EV = "https://solar.evableindia.com/api/"
    // Base URL for Razorpay IFSC API
    private const val BASE_URL_RAZORPAY_IFSC = "https://ifsc.razorpay.com/"

    // Standard OkHttpClient setup (can be shared or customized per instance)
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Logs request and response bodies
        }
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json")
                // Content-Type might not be needed for Razorpay GET, but Accept is good
                // For SolarEV POST/PUT, Content-Type: application/json is usually set
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Standard timeout for external quick lookups
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Gson instance (can be shared)
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // Lazy initialization of the SolarEV ApiService
    val api: ApiService by lazy {
        val solarEvClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(UnauthorizedInterceptor()) // Added UnauthorizedInterceptor
            .addInterceptor { chain -> // Specific interceptor for SolarEV if needed
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json") // Important for POST/PUT
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL_SOLAR_EV)
            .client(solarEvClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    // Lazy initialization of the RazorpayApiService
    val razorpayApi: RazorpayApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_RAZORPAY_IFSC)
            .client(createOkHttpClient()) // Using a common OkHttpClient for Razorpay
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RazorpayApiService::class.java)
    }
}
