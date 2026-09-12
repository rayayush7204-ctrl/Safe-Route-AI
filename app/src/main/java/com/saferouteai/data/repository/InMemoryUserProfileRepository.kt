package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserProfile
import com.saferouteai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserProfileRepository(
    initialProfile: UserProfile? = null
) : UserProfileRepository {

    private val mutex = Mutex()
    private val _profileState = MutableStateFlow<UserProfile?>(initialProfile)

    override fun getUserProfile(): Flow<UserProfile?> = _profileState.asStateFlow()

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = mutex.withLock {
        _profileState.value = profile
        Result.Success(Unit)
    }
}
