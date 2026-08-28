package com.example.data.repository

import com.example.core.network.Run2CaptureApiService
import com.example.core.network.model.GoogleAuthRequestDto
import com.example.core.network.model.LogoutRequestDto
import com.example.core.network.model.RefreshTokenRequestDto
import com.example.core.network.model.TokenPairDto
import com.example.core.network.model.UserDto
import com.example.core.network.model.UserLoginRequestDto
import com.example.core.network.model.UserRegisterRequestDto
import com.example.core.security.SecureStorage
import com.example.domain.model.AuthErrorType
import com.example.domain.model.AuthResult
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.Faction
import com.example.domain.repository.AuthRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepositoryImpl(
    private val apiService: Run2CaptureApiService,
    private val secureStorage: SecureStorage,
    private val moshi: Moshi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val userAdapter by lazy { moshi.adapter(UserDto::class.java) }

    init {
        // Restore session from secure storage
        val cachedUserJson = secureStorage.userJson
        val token = secureStorage.accessToken
        if (!token.isNullOrEmpty() && !cachedUserJson.isNullOrEmpty()) {
            try {
                val userDto = userAdapter.fromJson(cachedUserJson)
                if (userDto != null) {
                    _authState.value = AuthState.Authenticated(userDto.toDomain())
                } else {
                    _authState.value = AuthState.Unauthenticated()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated()
            }
        } else {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    override suspend fun checkSession(): AuthResult<AuthUser> = withContext(ioDispatcher) {
        val token = secureStorage.accessToken
        if (token.isNullOrEmpty()) {
            _authState.value = AuthState.Unauthenticated()
            return@withContext AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "No active session")
        }

        try {
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body()?.data != null) {
                val userDto = response.body()!!.data!!
                val user = userDto.toDomain()
                persistUser(userDto)
                _authState.value = AuthState.Authenticated(user)
                AuthResult.Success(user)
            } else if (response.code() == 401) {
                // Try refresh token
                refreshToken()
            } else {
                // Check if we have cached user for offline capability
                val cached = getCachedUser()
                if (cached != null) {
                    _authState.value = AuthState.Authenticated(cached)
                    AuthResult.Success(cached)
                } else {
                    _authState.value = AuthState.Unauthenticated("Session expired")
                    AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "Session expired")
                }
            }
        } catch (e: IOException) {
            // Offline fallback: Use cached session if valid
            val cached = getCachedUser()
            if (cached != null) {
                _authState.value = AuthState.Authenticated(cached)
                AuthResult.Success(cached)
            } else {
                _authState.value = AuthState.Error(AuthErrorType.NETWORK_FAILURE, "Cannot connect to server. Check connection.")
                AuthResult.Failure(AuthErrorType.NETWORK_FAILURE, "Network error: ${e.message}")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(AuthErrorType.SERVER_ERROR, e.message ?: "Unknown error")
            AuthResult.Failure(AuthErrorType.SERVER_ERROR, e.message ?: "Unknown error")
        }
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        faction: Faction
    ): AuthResult<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading
        try {
            val request = UserRegisterRequestDto(
                email = email.trim(),
                password = password,
                displayName = displayName.trim(),
                faction = faction.id
            )
            val response = apiService.register(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val tokenPair = response.body()!!.data!!
                handleSuccessfulAuth(tokenPair)
            } else {
                val code = response.code()
                val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Registration failed"
                val errorType = if (code == 409) AuthErrorType.EXISTING_ACCOUNT else AuthErrorType.SERVER_ERROR
                _authState.value = AuthState.Error(errorType, errorMsg)
                AuthResult.Failure(errorType, errorMsg)
            }
        } catch (e: IOException) {
            val msg = "Cannot connect to server. Please check your internet connection."
            _authState.value = AuthState.Error(AuthErrorType.NETWORK_FAILURE, msg)
            AuthResult.Failure(AuthErrorType.NETWORK_FAILURE, msg)
        } catch (e: Exception) {
            val msg = e.message ?: "Registration error"
            _authState.value = AuthState.Error(AuthErrorType.SERVER_ERROR, msg)
            AuthResult.Failure(AuthErrorType.SERVER_ERROR, msg)
        }
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String
    ): AuthResult<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading
        try {
            val request = UserLoginRequestDto(
                email = email.trim(),
                password = password
            )
            val response = apiService.login(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val tokenPair = response.body()!!.data!!
                handleSuccessfulAuth(tokenPair)
            } else {
                val code = response.code()
                val errorMsg = if (code == 401 || code == 404) "Account not found or incorrect password. Please register first." else "Authentication failed"
                val errorType = if (code == 401 || code == 404) AuthErrorType.INVALID_CREDENTIALS else AuthErrorType.SERVER_ERROR
                _authState.value = AuthState.Error(errorType, errorMsg)
                AuthResult.Failure(errorType, errorMsg)
            }
        } catch (e: IOException) {
            val msg = "Cannot connect to server at 152.67.1.252:8000. Please check your network."
            _authState.value = AuthState.Error(AuthErrorType.NETWORK_FAILURE, msg)
            AuthResult.Failure(AuthErrorType.NETWORK_FAILURE, msg)
        } catch (e: Exception) {
            val msg = e.message ?: "Login error"
            _authState.value = AuthState.Error(AuthErrorType.SERVER_ERROR, msg)
            AuthResult.Failure(AuthErrorType.SERVER_ERROR, msg)
        }
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        displayName: String?,
        faction: Faction?
    ): AuthResult<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading
        try {
            val request = GoogleAuthRequestDto(
                idToken = idToken,
                displayName = displayName,
                faction = faction?.id
            )
            val response = apiService.authWithGoogle(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val tokenPair = response.body()!!.data!!
                handleSuccessfulAuth(tokenPair)
            } else {
                val errorMsg = response.body()?.message ?: "Google authentication failed"
                _authState.value = AuthState.Error(AuthErrorType.GOOGLE_CANCELLATION, errorMsg)
                AuthResult.Failure(AuthErrorType.GOOGLE_CANCELLATION, errorMsg)
            }
        } catch (e: IOException) {
            val msg = "Cannot connect to server for Google Sign-In"
            _authState.value = AuthState.Error(AuthErrorType.NETWORK_FAILURE, msg)
            AuthResult.Failure(AuthErrorType.NETWORK_FAILURE, msg)
        } catch (e: Exception) {
            val msg = e.message ?: "Google auth error"
            _authState.value = AuthState.Error(AuthErrorType.GOOGLE_CANCELLATION, msg)
            AuthResult.Failure(AuthErrorType.GOOGLE_CANCELLATION, msg)
        }
    }

    override suspend fun refreshToken(): AuthResult<AuthUser> = withContext(ioDispatcher) {
        val refreshToken = secureStorage.refreshToken
        if (refreshToken.isNullOrEmpty()) {
            secureStorage.clearAll()
            _authState.value = AuthState.Unauthenticated("Session expired")
            return@withContext AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "No refresh token")
        }

        try {
            val response = apiService.refreshToken(RefreshTokenRequestDto(refreshToken))
            if (response.isSuccessful && response.body()?.data != null) {
                val tokenPair = response.body()!!.data!!
                handleSuccessfulAuth(tokenPair)
            } else {
                secureStorage.clearAll()
                _authState.value = AuthState.Unauthenticated("Session expired. Please log in again.")
                AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "Session expired")
            }
        } catch (e: Exception) {
            val cached = getCachedUser()
            if (cached != null) {
                _authState.value = AuthState.Authenticated(cached)
                AuthResult.Success(cached)
            } else {
                secureStorage.clearAll()
                _authState.value = AuthState.Unauthenticated()
                AuthResult.Failure(AuthErrorType.NETWORK_FAILURE, "Failed to refresh token")
            }
        }
    }

    override suspend fun logout(): AuthResult<Unit> = withContext(ioDispatcher) {
        try {
            val token = secureStorage.accessToken
            val refresh = secureStorage.refreshToken
            if (!token.isNullOrEmpty()) {
                apiService.logout("Bearer $token", LogoutRequestDto(refreshToken = refresh))
            }
        } catch (_: Exception) {
            // Ignore network errors during logout
        } finally {
            secureStorage.clearAll()
            _authState.value = AuthState.Unauthenticated()
        }
        AuthResult.Success(Unit)
    }

    override suspend fun awardXpAndArea(areaSqMeters: Double) = withContext(ioDispatcher) {
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            val updatedUser = currentState.user.copy(
                totalAreaSqMeters = currentState.user.totalAreaSqMeters + areaSqMeters
            )
            val updatedDto = updatedUser.toDto()
            persistUser(updatedDto)
            _authState.value = AuthState.Authenticated(updatedUser)
        }
    }

    override suspend fun awardProgression(
        sources: List<Pair<com.example.core.progression.XpSource, Int>>,
        newAreaSqMeters: Double,
        newDistanceMeters: Double,
        territoriesCaptured: Int
    ) = withContext(ioDispatcher) {
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            val engine = com.example.core.progression.ProgressionEngine()
            var user = currentState.user.copy(
                totalAreaSqMeters = currentState.user.totalAreaSqMeters + newAreaSqMeters,
                totalDistanceMeters = currentState.user.totalDistanceMeters + newDistanceMeters,
                territoriesCapturedCount = currentState.user.territoriesCapturedCount + territoriesCaptured,
                territoriesCount = currentState.user.territoriesCount + territoriesCaptured
            )
            user = engine.applyXp(user, sources)
            user = engine.checkAchievements(user)
            
            val updatedDto = user.toDto()
            persistUser(updatedDto)
            _authState.value = AuthState.Authenticated(user)
        }
    }

    override fun clearError() {
        if (_authState.value is AuthState.Error) {
            val cached = getCachedUser()
            if (cached != null) {
                _authState.value = AuthState.Authenticated(cached)
            } else {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    private fun handleSuccessfulAuth(tokenPair: TokenPairDto): AuthResult<AuthUser> {
        secureStorage.accessToken = tokenPair.accessToken
        secureStorage.refreshToken = tokenPair.refreshToken
        persistUser(tokenPair.user)
        val user = tokenPair.user.toDomain()
        _authState.value = AuthState.Authenticated(user)
        return AuthResult.Success(user)
    }

    private fun persistUser(userDto: UserDto) {
        try {
            val json = userAdapter.toJson(userDto)
            secureStorage.userJson = json
        } catch (_: Exception) {}
    }

    private fun getCachedUser(): AuthUser? {
        val json = secureStorage.userJson ?: return null
        return try {
            userAdapter.fromJson(json)?.toDomain()
        } catch (_: Exception) {
            null
        }
    }

    private fun UserDto.toDomain(): AuthUser {
        val flag = flagConfig?.let {
            com.example.domain.model.FlagConfig(
                background = it.background,
                pattern = it.pattern,
                emblem = it.emblem,
                border = it.border
            )
        } ?: com.example.domain.model.FlagConfig()

        return AuthUser(
            id = id,
            email = email,
            displayName = displayName,
            faction = Faction.fromId(faction),
            avatarUrl = avatarUrl,
            authProvider = authProvider,
            territoryColor = territoryColor ?: "cyan",
            flag = flag,
            totalAreaSqMeters = totalAreaSqMeters,
            totalDistanceMeters = totalDistanceMeters,
            territoriesCount = territoriesCount,
            territoriesCapturedCount = territoriesCapturedCount,
            xp = xp,
            level = level,
            nextLevelXp = nextLevelXp,
            achievements = achievements
        )
    }

    private fun AuthUser.toDto(): UserDto {
        val flagDto = com.example.core.network.model.FlagConfigDto(
            background = flag.background,
            pattern = flag.pattern,
            emblem = flag.emblem,
            border = flag.border
        )

        return UserDto(
            id = id,
            email = email,
            displayName = displayName,
            faction = faction.id,
            avatarUrl = avatarUrl,
            authProvider = authProvider,
            territoryColor = territoryColor,
            flagConfig = flagDto,
            totalAreaSqMeters = totalAreaSqMeters,
            totalDistanceMeters = totalDistanceMeters,
            territoriesCount = territoriesCount,
            territoriesCapturedCount = territoriesCapturedCount,
            xp = xp,
            level = level,
            nextLevelXp = nextLevelXp,
            achievements = achievements
        )
    }
}
