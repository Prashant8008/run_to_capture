package com.example.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AuthErrorType
import com.example.domain.model.AuthResult
import com.example.domain.model.AuthState
import com.example.domain.model.Faction
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val displayNameInput: String = "",
    val selectedFaction: Faction = Faction.CIPHER,
    val isPasswordVisible: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val forgotPasswordSent: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val displayNameError: String? = null,
    val generalError: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPasswordInput = password, passwordError = null, generalError = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayNameInput = name, displayNameError = null, generalError = null) }
    }

    fun onFactionSelected(faction: Faction) {
        _uiState.update { it.copy(selectedFaction = faction) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun setForgotPasswordDialogVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = visible,
                forgotPasswordEmail = if (visible) it.emailInput else "",
                forgotPasswordSent = false
            )
        }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.update { it.copy(forgotPasswordEmail = email) }
    }

    fun submitForgotPassword() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isEmpty() || !email.contains("@")) {
            _uiState.update { it.copy(generalError = "Please enter a valid email address") }
            return
        }
        _uiState.update { it.copy(forgotPasswordSent = true) }
    }

    fun login() {
        val state = _uiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput

        if (email.isEmpty() || !email.contains("@")) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address") }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(passwordError = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            val result = authRepository.loginWithEmail(email, password)
            if (result is AuthResult.Failure) {
                _uiState.update { it.copy(generalError = result.message) }
            }
        }
    }

    fun register() {
        val state = _uiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput
        val confirm = state.confirmPasswordInput
        val displayName = state.displayNameInput.trim()

        if (displayName.length < 2) {
            _uiState.update { it.copy(displayNameError = "Display name must be at least 2 characters") }
            return
        }
        if (email.isEmpty() || !email.contains("@")) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(passwordError = "Password must be at least 8 characters") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(passwordError = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            val result = authRepository.registerWithEmail(
                email = email,
                password = password,
                displayName = displayName,
                faction = state.selectedFaction
            )
            if (result is AuthResult.Failure) {
                _uiState.update { it.copy(generalError = result.message) }
            }
        }
    }

    fun loginWithGoogle(idToken: String, displayName: String? = null) {
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(
                idToken = idToken,
                displayName = displayName,
                faction = _uiState.value.selectedFaction
            )
            if (result is AuthResult.Failure) {
                _uiState.update { it.copy(generalError = result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(generalError = null, emailError = null, passwordError = null, displayNameError = null) }
        authRepository.clearError()
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
