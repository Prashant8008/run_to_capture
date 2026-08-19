package com.example.feature.competitive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Challenge
import com.example.domain.model.LeaderboardCategory
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.LeaderboardPeriod
import com.example.domain.repository.CompetitiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompetitiveUiState(
    val selectedTab: Int = 0, // 0 = Leaderboards, 1 = Challenges
    val selectedCategory: LeaderboardCategory = LeaderboardCategory.TERRITORY,
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.WEEKLY,
    val leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    val challenges: List<Challenge> = emptyList(),
    val isLoading: Boolean = false
)

class CompetitiveViewModel(
    private val competitiveRepository: CompetitiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompetitiveUiState())
    val uiState: StateFlow<CompetitiveUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
        loadChallenges()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun selectCategory(category: LeaderboardCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadLeaderboard()
    }

    fun selectPeriod(period: LeaderboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val category = _uiState.value.selectedCategory
            val period = _uiState.value.selectedPeriod
            competitiveRepository.getLeaderboard(category, period).collect { entries ->
                _uiState.update { it.copy(leaderboardEntries = entries, isLoading = false) }
            }
        }
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            competitiveRepository.getActiveChallenges().collect { challenges ->
                _uiState.update { it.copy(challenges = challenges) }
            }
        }
    }

    class Factory(
        private val competitiveRepository: CompetitiveRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompetitiveViewModel(competitiveRepository) as T
        }
    }
}
