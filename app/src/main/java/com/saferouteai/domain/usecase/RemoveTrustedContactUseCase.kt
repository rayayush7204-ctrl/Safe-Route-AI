package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.TrustedContactsRepository

class RemoveTrustedContactUseCase(
    private val repository: TrustedContactsRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> {
        if (contactId.isBlank()) {
            return Result.Error(IllegalArgumentException("Contact ID cannot be blank."))
        }
        return repository.removeTrustedContact(contactId)
    }
}
