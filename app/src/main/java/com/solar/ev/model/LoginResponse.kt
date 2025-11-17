package com.solar.ev.model

import java.util.Date


data class ApiErrorResponse(
    val errors: Errors,
    val message: String,
    val status: Boolean
)
data class Errors(
    val message: String,
)
data class LoginResponse(
    val `data`: Data,
    val message: String,
    val status: Boolean
)

data class Data(
    val token: String,
    val user: User
)


