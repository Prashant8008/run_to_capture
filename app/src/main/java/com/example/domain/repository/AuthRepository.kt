package com.example.domain.repository

import com.example.domain.model.AuthResult
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.Faction
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>

    suspend fun checkSession(): AuthResult<AuthUser>

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        faction: Faction
    ): AuthResult<AuthUser>

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): AuthResult<AuthUser>

    suspend fun loginWithGoogle(
        idToken: String,
        displayName: String? = null,
        faction: Faction? = null
    ): AuthResult<AuthUser>

    suspend fun refreshToken(): AuthResult<AuthUser>

    suspend fun logout(): AuthResult<Unit>
    
    suspend fun awardXpAndArea(areaSqMeters: Double)
    
    suspend fun awardProgression(sources: List<Pair<com.example.core.progression.XpSource, Int>>, newAreaSqMeters: Double = 0.0, newDistanceMeters: Double = 0.0, territoriesCaptured: Int = 0)

    fun clearError()
}
