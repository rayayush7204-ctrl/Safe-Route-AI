package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.TrustedContactsRepository

class SetContactEnabledUseCase(
    private val repository: TrustedContactsRepository
) {
    suspend operator fun invoke(contactId: String, isEnabled: Boolean): Result<Unit> {
        if (contactId.isBlank()) {
            return Result.Error(IllegalArgumentException("Contact ID cannot be blank."))
        }
        return repository.setContactEnabled(contactId, isEnabled)
    }
}
