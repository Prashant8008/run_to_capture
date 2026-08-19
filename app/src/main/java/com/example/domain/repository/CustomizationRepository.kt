package com.example.domain.repository

import com.example.domain.model.FlagConfig
import com.example.domain.model.PlayerCustomization
import kotlinx.coroutines.flow.StateFlow

interface CustomizationRepository {
    val customizationState: StateFlow<PlayerCustomization>

    suspend fun loadCustomization(): Result<PlayerCustomization>

    suspend fun saveCustomization(
        territoryColor: String,
        flag: FlagConfig
    ): Result<PlayerCustomization>

    fun setLocalCustomization(customization: PlayerCustomization)
}
