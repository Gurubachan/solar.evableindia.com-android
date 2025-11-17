package com.solar.ev.model

data class LoginRequest(
    val email: String, // Or 'username', matching your API
    val password: String
)
