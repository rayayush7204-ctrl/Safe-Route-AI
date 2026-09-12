package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.repository.TrustedContactsRepository

class AddTrustedContactUseCase(
    private val repository: TrustedContactsRepository
) {
    suspend operator fun invoke(contact: TrustedContact): Result<Unit> {
        val validationError = contact.validate()
        if (validationError != null) {
            return Result.Error(IllegalArgumentException(validationError))
        }
        val validatedContact = contact.copy(
            name = contact.name.trim(),
            phoneNumber = contact.phoneNumber.trim(),
            email = contact.email.trim(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.addTrustedContact(validatedContact)
    }
}
