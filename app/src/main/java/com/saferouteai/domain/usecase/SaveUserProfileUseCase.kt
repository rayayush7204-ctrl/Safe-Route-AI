package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.UserProfile
import com.saferouteai.domain.repository.UserProfileRepository

class SaveUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> {
        val validationError = profile.validate()
        if (validationError != null) {
            return Result.Error(IllegalArgumentException(validationError))
        }
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        return repository.saveUserProfile(updatedProfile)
    }
}
