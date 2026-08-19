package com.example.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthStatusDto(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: HealthStatusDto
)
