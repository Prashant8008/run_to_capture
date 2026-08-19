package com.example.data.repository

import com.example.core.network.Run2CaptureApiService
import com.example.core.network.model.CustomizationUpdateRequestDto
import com.example.core.network.model.FlagConfigDto
import com.example.core.network.model.UserDto
import com.example.core.security.SecureStorage
import com.example.domain.model.FlagConfig
import com.example.domain.model.MapContrastValidator
import com.example.domain.model.PlayerCustomization
import com.example.domain.model.StandardTerritoryColor
import com.example.domain.repository.CustomizationRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

class CustomizationRepositoryImpl(
    private val apiService: Run2CaptureApiService,
    private val secureStorage: SecureStorage,
    private val moshi: Moshi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CustomizationRepository {

    private val _customizationState = MutableStateFlow(PlayerCustomization())
    override val customizationState: StateFlow<PlayerCustomization> = _customizationState.asStateFlow()

    private val userAdapter by lazy { moshi.adapter(UserDto::class.java) }

    init {
        // Load initial state from cached user if available
        val cachedUserJson = secureStorage.userJson
        if (!cachedUserJson.isNullOrEmpty()) {
            try {
                val userDto = userAdapter.fromJson(cachedUserJson)
                if (userDto != null) {
                    val color = userDto.territoryColor ?: "cyan"
                    val flag = userDto.flagConfig?.let {
                        FlagConfig(
                            background = it.background,
                            pattern = it.pattern,
                            emblem = it.emblem,
                            border = it.border
                        )
                    } ?: FlagConfig()
                    _customizationState.value = PlayerCustomization(
                        territoryColor = color,
                        flag = flag
                    )
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun loadCustomization(): Result<PlayerCustomization> = withContext(ioDispatcher) {
        val token = secureStorage.accessToken
        if (token.isNullOrEmpty()) {
            return@withContext Result.success(_customizationState.value)
        }

        try {
            val response = apiService.getCustomization("Bearer $token")
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val flag = FlagConfig(
                    background = data.flag.background,
                    pattern = data.flag.pattern,
                    emblem = data.flag.emblem,
                    border = data.flag.border
                )
                val custom = PlayerCustomization(
                    territoryColor = data.territoryColor,
                    flag = flag
                )
                _customizationState.value = custom
                updateCachedUserCustomization(custom)
                Result.success(custom)
            } else {
                Result.success(_customizationState.value)
            }
        } catch (e: IOException) {
            // Offline / fallback
            Result.success(_customizationState.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCustomization(
        territoryColor: String,
        flag: FlagConfig
    ): Result<PlayerCustomization> = withContext(ioDispatcher) {
        val cleanColor = territoryColor.trim()

        // Validate color if custom
        val standard = StandardTerritoryColor.fromId(cleanColor)
        if (standard == null) {
            val hex = if (cleanColor.startsWith("#")) cleanColor else "#$cleanColor"
            val validation = MapContrastValidator.validate(hex)
            if (!validation.isValid) {
                return@withContext Result.failure(IllegalArgumentException(validation.message))
            }
        }

        val updatedCustomization = PlayerCustomization(
            territoryColor = cleanColor,
            flag = flag
        )

        val token = secureStorage.accessToken
        if (token.isNullOrEmpty()) {
            // Local dev mode
            _customizationState.value = updatedCustomization
            updateCachedUserCustomization(updatedCustomization)
            return@withContext Result.success(updatedCustomization)
        }

        try {
            val request = CustomizationUpdateRequestDto(
                territoryColor = cleanColor,
                flag = FlagConfigDto(
                    background = flag.background,
                    pattern = flag.pattern,
                    emblem = flag.emblem,
                    border = flag.border
                )
            )
            val response = apiService.updateCustomization("Bearer $token", request)
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val savedCustom = PlayerCustomization(
                    territoryColor = data.territoryColor,
                    flag = FlagConfig(
                        background = data.flag.background,
                        pattern = data.flag.pattern,
                        emblem = data.flag.emblem,
                        border = data.flag.border
                    )
                )
                _customizationState.value = savedCustom
                updateCachedUserCustomization(savedCustom)
                Result.success(savedCustom)
            } else {
                // Fallback to local apply
                _customizationState.value = updatedCustomization
                updateCachedUserCustomization(updatedCustomization)
                Result.success(updatedCustomization)
            }
        } catch (e: IOException) {
            // Fallback to local persistence during offline or local dev
            _customizationState.value = updatedCustomization
            updateCachedUserCustomization(updatedCustomization)
            Result.success(updatedCustomization)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setLocalCustomization(customization: PlayerCustomization) {
        _customizationState.value = customization
        updateCachedUserCustomization(customization)
    }

    private fun updateCachedUserCustomization(customization: PlayerCustomization) {
        val cachedUserJson = secureStorage.userJson ?: return
        try {
            val userDto = userAdapter.fromJson(cachedUserJson) ?: return
            val updatedUserDto = userDto.copy(
                territoryColor = customization.territoryColor,
                flagConfig = FlagConfigDto(
                    background = customization.flag.background,
                    pattern = customization.flag.pattern,
                    emblem = customization.flag.emblem,
                    border = customization.flag.border
                )
            )
            secureStorage.userJson = userAdapter.toJson(updatedUserDto)
        } catch (_: Exception) {}
    }
}
