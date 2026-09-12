package com.saferouteai.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserProfile
import com.saferouteai.domain.repository.UserProfileRepository
import com.saferouteai.domain.usecase.GetUserProfileUseCase
import com.saferouteai.domain.usecase.SaveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { profile ->
                if (profile != null) {
                    _uiState.update { current ->
                        current.copy(
                            displayName = profile.displayName,
                            phoneNumber = profile.phoneNumber,
                            email = profile.email,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name, errorMessage = null, saveSuccess = false) }
    }

    fun onPhoneNumberChanged(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone, errorMessage = null, saveSuccess = false) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null, saveSuccess = false) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }

            val profile = UserProfile(
                displayName = _uiState.value.displayName.trim(),
                phoneNumber = _uiState.value.phoneNumber.trim(),
                email = _uiState.value.email.trim()
            )

            when (val result = saveUserProfileUseCase(profile)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.exception.localizedMessage ?: "Failed to save profile"
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, saveSuccess = false) }
    }

    companion object {
        fun provideFactory(userProfileRepository: UserProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(
                        getUserProfileUseCase = GetUserProfileUseCase(userProfileRepository),
                        saveUserProfileUseCase = SaveUserProfileUseCase(userProfileRepository)
                    ) as T
                }
            }
    }
}
