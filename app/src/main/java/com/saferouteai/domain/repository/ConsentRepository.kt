package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserConsent
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing user consent choices.
 */
interface ConsentRepository {
    /**
     * Observes the current user consent state.
     */
    fun getConsent(): Flow<UserConsent>

    /**
     * Updates user consent flags explicitly.
     */
    suspend fun updateConsent(consent: UserConsent): Result<Unit>

    /**
     * Updates location sharing consent explicitly.
     */
    suspend fun setLocationSharingConsent(enabled: Boolean): Result<Unit>

    /**
     * Updates trusted contact sharing consent explicitly.
     */
    suspend fun setTrustedContactSharingConsent(enabled: Boolean): Result<Unit>
}
