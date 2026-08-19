package com.example.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val faction: Faction,
    val avatarUrl: String? = null,
    val authProvider: String = "password",
    val territoryColor: String = "cyan",
    val flag: FlagConfig = FlagConfig(),
    val totalAreaSqMeters: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val territoriesCount: Int = 0,
    val territoriesCapturedCount: Int = 0,
    val xp: Long = 0,
    val level: Int = 1,
    val nextLevelXp: Long = 1000,
    val achievements: List<String> = emptyList()
)

sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Unauthenticated(val message: String? = null) : AuthState
    data class Error(val errorType: AuthErrorType, val message: String) : AuthState
}

enum class AuthErrorType {
    INVALID_CREDENTIALS,
    EXISTING_ACCOUNT,
    NETWORK_FAILURE,
    EXPIRED_SESSION,
    GOOGLE_CANCELLATION,
    SERVER_ERROR,
    UNKNOWN
}

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Failure(val errorType: AuthErrorType, val message: String) : AuthResult<Nothing>()
}
