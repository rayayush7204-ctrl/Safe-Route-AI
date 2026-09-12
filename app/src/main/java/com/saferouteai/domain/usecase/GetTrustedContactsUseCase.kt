package com.saferouteai.domain.usecase

import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.repository.TrustedContactsRepository
import kotlinx.coroutines.flow.Flow

class GetTrustedContactsUseCase(
    private val repository: TrustedContactsRepository
) {
    operator fun invoke(): Flow<List<TrustedContact>> = repository.getTrustedContacts()
}
