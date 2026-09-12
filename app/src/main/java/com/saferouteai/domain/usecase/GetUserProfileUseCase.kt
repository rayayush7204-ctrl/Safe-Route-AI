package com.saferouteai.domain.usecase

import com.saferouteai.domain.model.UserProfile
import com.saferouteai.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

class GetUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> = repository.getUserProfile()
}
