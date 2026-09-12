package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.local.datastore.ConsentDataStore
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.repository.ConsentRepository
import kotlinx.coroutines.flow.Flow

class DataStoreConsentRepository(
    private val consentDataStore: ConsentDataStore
) : ConsentRepository {

    override fun getConsent(): Flow<UserConsent> {
        return consentDataStore.userConsentFlow
    }

    override suspend fun updateConsent(consent: UserConsent): Result<Unit> {
        return try {
            consentDataStore.updateConsent(consent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setLocationSharingConsent(enabled: Boolean): Result<Unit> {
        return try {
            consentDataStore.setLocationSharingConsent(enabled)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setTrustedContactSharingConsent(enabled: Boolean): Result<Unit> {
        return try {
            consentDataStore.setTrustedContactSharingConsent(enabled)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
