package com.example.feature.customization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AuthState
import com.example.domain.model.Faction
import com.example.domain.model.FlagBackground
import com.example.domain.model.FlagBorder
import com.example.domain.model.FlagConfig
import com.example.domain.model.FlagEmblem
import com.example.domain.model.FlagPattern
import com.example.domain.model.MapContrastValidator
import com.example.domain.model.PlayerCustomization
import com.example.domain.model.StandardTerritoryColor
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CustomizationTab(val label: String) {
    COLOR("TERRITORY COLOR"),
    BACKGROUND("BACKGROUND"),
    PATTERN("PATTERN"),
    EMBLEM("EMBLEM"),
    BORDER("BORDER")
}

data class FlagCreatorUiState(
    val territoryColor: String = "cyan",
    val flag: FlagConfig = FlagConfig(),
    val username: String = "OPERATIVE",
    val faction: Faction = Faction.CIPHER,
    val avatarUrl: String? = null,
    val activeTab: CustomizationTab = CustomizationTab.COLOR,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class FlagCreatorViewModel(
    private val customizationRepository: CustomizationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlagCreatorUiState())
    val uiState: StateFlow<FlagCreatorUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Subscribe to Auth State for user details
            authRepository.authState.collect { authState ->
                if (authState is AuthState.Authenticated) {
                    _uiState.update { current ->
                        current.copy(
                            username = authState.user.displayName,
                            faction = authState.user.faction,
                            avatarUrl = authState.user.avatarUrl,
                            territoryColor = authState.user.territoryColor,
                            flag = authState.user.flag
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            customizationRepository.customizationState.collect { custom ->
                _uiState.update { current ->
                    current.copy(
                        territoryColor = custom.territoryColor,
                        flag = custom.flag
                    )
                }
            }
        }

        // Fetch remote customization
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = customizationRepository.loadCustomization()
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { custom ->
                _uiState.update { it.copy(territoryColor = custom.territoryColor, flag = custom.flag) }
            }
        }
    }

    fun setTab(tab: CustomizationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectTerritoryColor(color: String) {
        _uiState.update { it.copy(territoryColor = color, errorMessage = null) }
    }

    fun selectBackground(bgId: String) {
        _uiState.update { current ->
            current.copy(
                flag = current.flag.copy(background = bgId),
                errorMessage = null
            )
        }
    }

    fun selectPattern(patternId: String) {
        _uiState.update { current ->
            current.copy(
                flag = current.flag.copy(pattern = patternId),
                errorMessage = null
            )
        }
    }

    fun selectEmblem(emblemId: String) {
        _uiState.update { current ->
            current.copy(
                flag = current.flag.copy(emblem = emblemId),
                errorMessage = null
            )
        }
    }

    fun selectBorder(borderId: String) {
        _uiState.update { current ->
            current.copy(
                flag = current.flag.copy(border = borderId),
                errorMessage = null
            )
        }
    }

    fun saveCustomization(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val color = currentState.territoryColor.trim()

            // Validate color
            val standard = StandardTerritoryColor.fromId(color)
            if (standard == null) {
                val hex = if (color.startsWith("#")) color else "#$color"
                val check = MapContrastValidator.validate(hex)
                if (!check.isValid) {
                    _uiState.update { it.copy(errorMessage = check.message) }
                    return@launch
                }
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = customizationRepository.saveCustomization(
                territoryColor = color,
                flag = currentState.flag
            )
            _uiState.update { it.copy(isSaving = false) }

            result.fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            territoryColor = saved.territoryColor,
                            flag = saved.flag,
                            saveSuccessMessage = "Visual Identity & Flag successfully deployed to command network!"
                        )
                    }
                    onSuccess?.invoke()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to save visual customization")
                    }
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(saveSuccessMessage = null, errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            customizationRepository: CustomizationRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FlagCreatorViewModel(customizationRepository, authRepository) as T
            }
        }
    }
}
