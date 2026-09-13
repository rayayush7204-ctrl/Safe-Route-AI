package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.repository.ConsentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryConsentRepository(
    initialConsent: UserConsent = UserConsent()
) : ConsentRepository {

    private val mutex = Mutex()
    private val _consentState = MutableStateFlow(initialConsent)

    override fun getConsent(): Flow<UserConsent> = _consentState.asStateFlow()

    override suspend fun updateConsent(consent: UserConsent): Result<Unit> = mutex.withLock {
        _consentState.value = consent
        Result.Success(Unit)
    }

    override suspend fun setLocationSharingConsent(enabled: Boolean): Result<Unit> = mutex.withLock {
        _consentState.value = _consentState.value.copy(
            locationSharingConsent = enabled,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    }

    override suspend fun setTrustedContactSharingConsent(enabled: Boolean): Result<Unit> = mutex.withLock {
        _consentState.value = _consentState.value.copy(
            trustedContactSharingConsent = enabled,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    }

    override suspend fun setAudioProcessingConsent(enabled: Boolean): Result<Unit> = mutex.withLock {
        _consentState.value = _consentState.value.copy(
            audioProcessingConsent = enabled,
            updatedAt = System.currentTimeMillis()
        )
        Result.Success(Unit)
    }
}
