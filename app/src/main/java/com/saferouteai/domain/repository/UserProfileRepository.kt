package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing user profile persistence.
 */
interface UserProfileRepository {
    /**
     * Observes the current local user profile, or null if unconfigured.
     */
    fun getUserProfile(): Flow<UserProfile?>

    /**
     * Persists or updates the local user profile.
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
}
