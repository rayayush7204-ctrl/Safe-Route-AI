package com.saferouteai.presentation.contacts

import com.saferouteai.domain.model.TrustedContact

data class TrustedContactsUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val isLoading: Boolean = false,
    val selectedContactForEdit: TrustedContact? = null,
    val isAddEditOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
