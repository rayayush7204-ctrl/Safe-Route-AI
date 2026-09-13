package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.repository.ConsentRepository

class UpdateConsentUseCase(
    private val repository: ConsentRepository
) {
    suspend fun setLocationSharingConsent(enabled: Boolean): Result<Unit> {
        return repository.setLocationSharingConsent(enabled)
    }

    suspend fun setTrustedContactSharingConsent(enabled: Boolean): Result<Unit> {
        return repository.setTrustedContactSharingConsent(enabled)
    }

    suspend fun setAudioProcessingConsent(enabled: Boolean): Result<Unit> {
        return repository.setAudioProcessingConsent(enabled)
    }

    suspend fun updateConsent(consent: UserConsent): Result<Unit> {
        return repository.updateConsent(consent.copy(updatedAt = System.currentTimeMillis()))
    }
}
