package com.saferouteai.presentation.consent

import com.saferouteai.domain.model.UserConsent

data class ConsentUiState(
    val consent: UserConsent = UserConsent(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
