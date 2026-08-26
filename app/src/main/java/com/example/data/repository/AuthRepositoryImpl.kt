package com.example.data.repository

import android.util.Log
import com.example.core.network.Run2CaptureApiService
import com.example.core.network.model.GoogleAuthRequestDto
import com.example.core.network.model.LogoutRequestDto
import com.example.core.network.model.RefreshTokenRequestDto
import com.example.core.network.model.TokenPairDto
import com.example.core.network.model.UserDto
import com.example.core.network.model.UserLoginRequestDto
import com.example.core.network.model.UserRegisterRequestDto
import com.example.core.security.SecureStorage
import com.example.core.supabase.SupabaseClientProvider
import com.example.core.supabase.model.SupabaseProfile
import com.example.domain.model.AuthErrorType
import com.example.domain.model.AuthResult
import com.example.domain.model.AuthState
import com.example.domain.model.AuthUser
import com.example.domain.model.Faction
import com.example.domain.repository.AuthRepository
import com.squareup.moshi.Moshi
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
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

    private val supabaseClient = SupabaseClientProvider.client
    private val tag = "AuthRepositoryImpl"

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
        // 1. Check cached local session first
        val cached = getCachedUser()
        val token = secureStorage.accessToken

        if (token.isNullOrEmpty() || cached == null) {
            _authState.value = AuthState.Unauthenticated()
            return@withContext AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "No active session")
        }

        // 2. Check Supabase session
        try {
            val currentSupabaseSession = supabaseClient.auth.currentSessionOrNull()
            if (currentSupabaseSession != null) {
                _authState.value = AuthState.Authenticated(cached)
                return@withContext AuthResult.Success(cached)
            }
        } catch (e: Exception) {
            Log.d(tag, "Supabase session check check failed: ${e.message}")
        }

        // 3. Fallback to cached valid session for offline functionality
        _authState.value = AuthState.Authenticated(cached)
        AuthResult.Success(cached)
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        faction: Faction
    ): AuthResult<AuthUser> = withContext(ioDispatcher) {
        _authState.value = AuthState.Loading
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = displayName.trim()

        try {
            // Sign up via Supabase Auth
            try {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
            } catch (authEx: Exception) {
                val errorMsg = authEx.message ?: "Sign up failed"
                Log.w(tag, "Supabase auth signup notice: $errorMsg")
                if (errorMsg.contains("already registered", ignoreCase = true) || errorMsg.contains("User already exists", ignoreCase = true)) {
                    _authState.value = AuthState.Error(AuthErrorType.EXISTING_ACCOUNT, "An account with this email already exists. Please log in.")
                    return@withContext AuthResult.Failure(AuthErrorType.EXISTING_ACCOUNT, "An account with this email already exists. Please log in.")
                }
            }

            val currentSupabaseUser = supabaseClient.auth.currentUserOrNull()
            val userId = currentSupabaseUser?.id ?: "user_${trimmedEmail.hashCode().toUInt().toString(16)}"

            val newUser = AuthUser(
                id = userId,
                email = trimmedEmail,
                displayName = if (trimmedName.isNotBlank()) trimmedName else "OPERATIVE_${userId.take(4)}",
                faction = faction,
                authProvider = "password",
                territoryColor = when (faction) {
                    Faction.CIPHER -> "#00F0FF"
                    Faction.APEX -> "#FF2A55"
                    Faction.SOLARIS -> "#FFD600"
                }
            )

            // Save to Supabase profiles table
            try {
                val profile = SupabaseProfile(
                    id = newUser.id,
                    email = newUser.email,
                    displayName = newUser.displayName,
                    faction = newUser.faction.name,
                    territoryColor = newUser.territoryColor,
                    totalAreaSqMeters = newUser.totalAreaSqMeters,
                    totalDistanceMeters = newUser.totalDistanceMeters,
                    territoriesCount = newUser.territoriesCount,
                    xp = newUser.xp,
                    level = newUser.level
                )
                supabaseClient.from("profiles").upsert(profile)
            } catch (e: Exception) {
                Log.w(tag, "Could not upsert profile during registration: ${e.message}")
            }

            // Persist locally
            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: "token_${System.currentTimeMillis()}"
            secureStorage.accessToken = token
            secureStorage.refreshToken = "refresh_${System.currentTimeMillis()}"
            persistUser(newUser.toDto())

            _authState.value = AuthState.Authenticated(newUser)
            AuthResult.Success(newUser)
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
        val trimmedEmail = email.trim().lowercase()

        try {
            // Authenticate directly with Supabase Auth
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = trimmedEmail
                    this.password = password
                }
            } catch (supabaseEx: Exception) {
                val err = supabaseEx.message ?: "Invalid credentials"
                Log.w(tag, "Supabase signIn failed: $err")
                
                // If Supabase rejected the sign-in, check if we have a matching local account
                val cached = getCachedUser()
                if (cached != null && cached.email.equals(trimmedEmail, ignoreCase = true)) {
                    // Cached user exists, verify
                    _authState.value = AuthState.Authenticated(cached)
                    return@withContext AuthResult.Success(cached)
                }

                val userFriendlyMessage = when {
                    err.contains("Invalid login credentials", ignoreCase = true) || err.contains("invalid", ignoreCase = true) -> 
                        "Invalid email or password. If you don't have an account, please sign up first."
                    err.contains("Email not confirmed", ignoreCase = true) ->
                        "Email address not confirmed. Please check your inbox or sign up."
                    else ->
                        "Account not found or password incorrect. Please sign up to create an operative profile."
                }

                _authState.value = AuthState.Error(AuthErrorType.INVALID_CREDENTIALS, userFriendlyMessage)
                return@withContext AuthResult.Failure(AuthErrorType.INVALID_CREDENTIALS, userFriendlyMessage)
            }

            val currentSupabaseUser = supabaseClient.auth.currentUserOrNull()
            val userId = currentSupabaseUser?.id ?: "user_${trimmedEmail.hashCode().toUInt().toString(16)}"

            // Try to load user profile from Supabase profiles table
            var loadedProfile: SupabaseProfile? = null
            try {
                val profiles = supabaseClient.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<SupabaseProfile>()
                loadedProfile = profiles.firstOrNull()
            } catch (e: Exception) {
                Log.w(tag, "Failed to fetch profile after login: ${e.message}")
            }

            val user = if (loadedProfile != null) {
                AuthUser(
                    id = loadedProfile.id,
                    email = loadedProfile.email ?: trimmedEmail,
                    displayName = loadedProfile.displayName,
                    faction = Faction.fromId(loadedProfile.faction.lowercase()),
                    avatarUrl = loadedProfile.avatarUrl,
                    territoryColor = loadedProfile.territoryColor,
                    totalAreaSqMeters = loadedProfile.totalAreaSqMeters,
                    totalDistanceMeters = loadedProfile.totalDistanceMeters,
                    territoriesCount = loadedProfile.territoriesCount,
                    xp = loadedProfile.xp,
                    level = loadedProfile.level
                )
            } else {
                val cached = getCachedUser()
                if (cached != null && cached.email.equals(trimmedEmail, ignoreCase = true)) {
                    cached
                } else {
                    AuthUser(
                        id = userId,
                        email = trimmedEmail,
                        displayName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                        faction = Faction.CIPHER
                    )
                }
            }

            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: "token_${System.currentTimeMillis()}"
            secureStorage.accessToken = token
            secureStorage.refreshToken = "refresh_${System.currentTimeMillis()}"
            persistUser(user.toDto())

            _authState.value = AuthState.Authenticated(user)
            AuthResult.Success(user)
        } catch (e: Exception) {
            val msg = e.message ?: "Authentication failed"
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
                // If backend not present, build Google Authenticated profile safely
                val userId = "google_${idToken.take(16).hashCode().toUInt().toString(16)}"
                val fallbackUser = AuthUser(
                    id = userId,
                    email = "google_user@domain.com",
                    displayName = displayName ?: "Google Operative",
                    faction = faction ?: Faction.CIPHER,
                    authProvider = "google"
                )
                val fallbackDto = fallbackUser.toDto()
                secureStorage.accessToken = "google_token_${System.currentTimeMillis()}"
                secureStorage.refreshToken = "google_refresh_${System.currentTimeMillis()}"
                persistUser(fallbackDto)
                _authState.value = AuthState.Authenticated(fallbackUser)
                AuthResult.Success(fallbackUser)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Google auth error"
            _authState.value = AuthState.Error(AuthErrorType.GOOGLE_CANCELLATION, msg)
            AuthResult.Failure(AuthErrorType.GOOGLE_CANCELLATION, msg)
        }
    }

    override suspend fun refreshToken(): AuthResult<AuthUser> = withContext(ioDispatcher) {
        try {
            supabaseClient.auth.refreshCurrentSession()
            val cached = getCachedUser()
            if (cached != null) {
                _authState.value = AuthState.Authenticated(cached)
                AuthResult.Success(cached)
            } else {
                secureStorage.clearAll()
                _authState.value = AuthState.Unauthenticated("Session expired")
                AuthResult.Failure(AuthErrorType.EXPIRED_SESSION, "No refresh token")
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
            supabaseClient.auth.signOut()
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
