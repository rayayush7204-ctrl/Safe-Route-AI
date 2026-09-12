package com.saferouteai.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saferouteai.domain.model.UserConsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.consentDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_consent_prefs")

class ConsentDataStore(private val context: Context) {

    private object PreferencesKeys {
        val LOCATION_SHARING_CONSENT = booleanPreferencesKey("location_sharing_consent")
        val TRUSTED_CONTACT_SHARING_CONSENT = booleanPreferencesKey("trusted_contact_sharing_consent")
        val UPDATED_AT = longPreferencesKey("consent_updated_at")
    }

    val userConsentFlow: Flow<UserConsent> = context.consentDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserConsent(
                locationSharingConsent = preferences[PreferencesKeys.LOCATION_SHARING_CONSENT] ?: false,
                trustedContactSharingConsent = preferences[PreferencesKeys.TRUSTED_CONTACT_SHARING_CONSENT] ?: false,
                updatedAt = preferences[PreferencesKeys.UPDATED_AT] ?: System.currentTimeMillis()
            )
        }

    suspend fun setLocationSharingConsent(enabled: Boolean) {
        context.consentDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCATION_SHARING_CONSENT] = enabled
            preferences[PreferencesKeys.UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setTrustedContactSharingConsent(enabled: Boolean) {
        context.consentDataStore.edit { preferences ->
            preferences[PreferencesKeys.TRUSTED_CONTACT_SHARING_CONSENT] = enabled
            preferences[PreferencesKeys.UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateConsent(consent: UserConsent) {
        context.consentDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCATION_SHARING_CONSENT] = consent.locationSharingConsent
            preferences[PreferencesKeys.TRUSTED_CONTACT_SHARING_CONSENT] = consent.trustedContactSharingConsent
            preferences[PreferencesKeys.UPDATED_AT] = consent.updatedAt
        }
    }
}
