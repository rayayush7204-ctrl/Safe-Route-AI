package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.local.dao.UserProfileDao
import com.saferouteai.data.local.entity.UserProfileEntity
import com.saferouteai.domain.model.UserProfile
import com.saferouteai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserProfileRepository(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(UserProfile.DEFAULT_USER_ID).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(profile))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
