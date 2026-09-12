package com.saferouteai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.repository.JourneyRepository
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the UI state and user interactions for the SafeRoute home journey flow.
 */
class JourneyViewModel(
    private val startJourneyUseCase: StartJourneyUseCase,
    private val endJourneyUseCase: EndJourneyUseCase,
    private val getJourneyStateUseCase: GetJourneyStateUseCase,
    private val resetJourneyUseCase: ResetJourneyUseCase? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<JourneyUiState> = combine(
        getJourneyStateUseCase(),
        _isLoading,
        _errorMessage
    ) { state, isLoading, errorMessage ->
        JourneyUiState(
            journeyState = state,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JourneyUiState(journeyState = JourneyState.IDLE)
    )

    fun startJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = startJourneyUseCase()) {
                is Result.Success -> {
                    // State updated reactively via GetJourneyStateUseCase
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.localizedMessage ?: "Failed to start journey"
                }
            }
            _isLoading.value = false
        }
    }

    fun endJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = endJourneyUseCase()) {
                is Result.Success -> {
                    // State updated reactively via GetJourneyStateUseCase
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.localizedMessage ?: "Failed to end journey"
                }
            }
            _isLoading.value = false
        }
    }

    fun resetJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            resetJourneyUseCase?.invoke()
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(journeyRepository: JourneyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JourneyViewModel(
                        startJourneyUseCase = StartJourneyUseCase(journeyRepository),
                        endJourneyUseCase = EndJourneyUseCase(journeyRepository),
                        getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepository),
                        resetJourneyUseCase = ResetJourneyUseCase(journeyRepository)
                    ) as T
                }
            }
    }
}
