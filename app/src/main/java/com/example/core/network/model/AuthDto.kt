package com.example.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "faction") val faction: String = "CIPHER"
)

@JsonClass(generateAdapter = true)
data class UserLoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequestDto(
    @Json(name = "id_token") val idToken: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "faction") val faction: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class LogoutRequestDto(
    @Json(name = "refresh_token") val refreshToken: String? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "faction") val faction: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "auth_provider") val authProvider: String,
    @Json(name = "territory_color") val territoryColor: String? = "cyan",
    @Json(name = "flag_config") val flagConfig: FlagConfigDto? = null,
    @Json(name = "total_area_sq_meters") val totalAreaSqMeters: Double = 0.0,
    @Json(name = "total_distance_meters") val totalDistanceMeters: Double = 0.0,
    @Json(name = "territories_count") val territoriesCount: Int = 0,
    @Json(name = "territories_captured_count") val territoriesCapturedCount: Int = 0,
    @Json(name = "xp") val xp: Long = 0,
    @Json(name = "level") val level: Int = 1,
    @Json(name = "next_level_xp") val nextLevelXp: Long = 1000,
    @Json(name = "achievements") val achievements: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FlagConfigDto(
    @Json(name = "background") val background: String = "navy",
    @Json(name = "pattern") val pattern: String = "diagonal",
    @Json(name = "emblem") val emblem: String = "wolf",
    @Json(name = "border") val border: String = "gold"
)

@JsonClass(generateAdapter = true)
data class CustomizationUpdateRequestDto(
    @Json(name = "territory_color") val territoryColor: String,
    @Json(name = "flag") val flag: FlagConfigDto
)

@JsonClass(generateAdapter = true)
data class CustomizationResponseDto(
    @Json(name = "territory_color") val territoryColor: String,
    @Json(name = "territory_color_hex") val territoryColorHex: String,
    @Json(name = "is_custom_color") val isCustomColor: Boolean,
    @Json(name = "flag") val flag: FlagConfigDto,
    @Json(name = "map_visibility_status") val mapVisibilityStatus: String = "OPTIMAL_VISIBILITY"
)

@JsonClass(generateAdapter = true)
data class CustomizationOptionsDto(
    @Json(name = "standard_colors") val standardColors: Map<String, String>,
    @Json(name = "backgrounds") val backgrounds: List<String>,
    @Json(name = "patterns") val patterns: List<String>,
    @Json(name = "emblems") val emblems: List<String>,
    @Json(name = "borders") val borders: List<String>
)

@JsonClass(generateAdapter = true)
data class TokenPairDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class ApiResponseDto<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)
