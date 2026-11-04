package com.medihistory.ai.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for the backend auth endpoints.
 * Privacy: email is never logged. Tokens are stored only in EncryptedSharedPreferences.
 */

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "email")        val email: String,
    @Json(name = "password")     val password: String,
    @Json(name = "display_name") val displayName: String,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email")    val email: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token")  val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "id")    val id: String,
    @Json(name = "email") val email: String,
)
