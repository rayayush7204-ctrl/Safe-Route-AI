package com.saferouteai.domain.usecase

import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.repository.ConsentRepository
import kotlinx.coroutines.flow.Flow

class GetConsentUseCase(
    private val repository: ConsentRepository
) {
    operator fun invoke(): Flow<UserConsent> = repository.getConsent()
}
