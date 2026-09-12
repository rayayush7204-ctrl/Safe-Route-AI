package com.saferouteai.presentation.profile

import com.saferouteai.domain.model.UserProfile

data class ProfileUiState(
    val displayName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
