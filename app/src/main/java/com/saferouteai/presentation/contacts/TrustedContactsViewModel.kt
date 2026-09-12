package com.saferouteai.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.repository.TrustedContactsRepository
import com.saferouteai.domain.usecase.AddTrustedContactUseCase
import com.saferouteai.domain.usecase.GetTrustedContactsUseCase
import com.saferouteai.domain.usecase.RemoveTrustedContactUseCase
import com.saferouteai.domain.usecase.SetContactEnabledUseCase
import com.saferouteai.domain.usecase.UpdateTrustedContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrustedContactsViewModel(
    private val getTrustedContactsUseCase: GetTrustedContactsUseCase,
    private val addTrustedContactUseCase: AddTrustedContactUseCase,
    private val updateTrustedContactUseCase: UpdateTrustedContactUseCase,
    private val removeTrustedContactUseCase: RemoveTrustedContactUseCase,
    private val setContactEnabledUseCase: SetContactEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrustedContactsUiState(isLoading = true))
    val uiState: StateFlow<TrustedContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            getTrustedContactsUseCase().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts, isLoading = false) }
            }
        }
    }

    fun openAddContactDialog() {
        _uiState.update { it.copy(isAddEditOpen = true, selectedContactForEdit = null, errorMessage = null) }
    }

    fun openEditContactDialog(contact: TrustedContact) {
        _uiState.update { it.copy(isAddEditOpen = true, selectedContactForEdit = contact, errorMessage = null) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isAddEditOpen = false, selectedContactForEdit = null, errorMessage = null) }
    }

    fun saveContact(contact: TrustedContact) {
        viewModelScope.launch {
            val isExisting = _uiState.value.contacts.any { it.id == contact.id }
            val result = if (isExisting) {
                updateTrustedContactUseCase(contact)
            } else {
                addTrustedContactUseCase(contact)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isAddEditOpen = false,
                            selectedContactForEdit = null,
                            successMessage = if (isExisting) "Contact updated" else "Contact added"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.exception.localizedMessage ?: "Failed to save contact"
                        )
                    }
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            when (val result = removeTrustedContactUseCase(contactId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Contact removed") }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.localizedMessage ?: "Failed to remove contact")
                    }
                }
            }
        }
    }

    fun toggleContactEnabled(contactId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            when (val result = setContactEnabledUseCase(contactId, isEnabled)) {
                is Result.Success -> {
                    // updated reactively via Flow
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.localizedMessage ?: "Failed to update status")
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        fun provideFactory(trustedContactsRepository: TrustedContactsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrustedContactsViewModel(
                        getTrustedContactsUseCase = GetTrustedContactsUseCase(trustedContactsRepository),
                        addTrustedContactUseCase = AddTrustedContactUseCase(trustedContactsRepository),
                        updateTrustedContactUseCase = UpdateTrustedContactUseCase(trustedContactsRepository),
                        removeTrustedContactUseCase = RemoveTrustedContactUseCase(trustedContactsRepository),
                        setContactEnabledUseCase = SetContactEnabledUseCase(trustedContactsRepository)
                    ) as T
                }
            }
    }
}
