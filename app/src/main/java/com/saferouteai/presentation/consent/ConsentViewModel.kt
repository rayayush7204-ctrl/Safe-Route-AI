package com.saferouteai.presentation.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.usecase.GetConsentUseCase
import com.saferouteai.domain.usecase.UpdateConsentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConsentViewModel(
    private val getConsentUseCase: GetConsentUseCase,
    private val updateConsentUseCase: UpdateConsentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsentUiState(isLoading = true))
    val uiState: StateFlow<ConsentUiState> = _uiState.asStateFlow()

    init {
        loadConsent()
    }

    private fun loadConsent() {
        viewModelScope.launch {
            getConsentUseCase().collect { consent ->
                _uiState.update { it.copy(consent = consent, isLoading = false) }
            }
        }
    }

    fun onLocationSharingConsentToggled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = updateConsentUseCase.setLocationSharingConsent(enabled)) {
                is Result.Success -> {
                    // updated reactively
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.localizedMessage ?: "Failed to update consent")
                    }
                }
            }
        }
    }

    fun onTrustedContactSharingConsentToggled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = updateConsentUseCase.setTrustedContactSharingConsent(enabled)) {
                is Result.Success -> {
                    // updated reactively
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.localizedMessage ?: "Failed to update consent")
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(consentRepository: ConsentRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ConsentViewModel(
                        getConsentUseCase = GetConsentUseCase(consentRepository),
                        updateConsentUseCase = UpdateConsentUseCase(consentRepository)
                    ) as T
                }
            }
    }
}
