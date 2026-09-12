package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.repository.TrustedContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryTrustedContactsRepository(
    initialContacts: List<TrustedContact> = emptyList()
) : TrustedContactsRepository {

    private val mutex = Mutex()
    private val contactsMap = LinkedHashMap<String, TrustedContact>().apply {
        initialContacts.forEach { put(it.id, it) }
    }
    private val _contactsState = MutableStateFlow<List<TrustedContact>>(initialContacts)

    override fun getTrustedContacts(): Flow<List<TrustedContact>> = _contactsState.asStateFlow()

    override suspend fun getContactById(id: String): Result<TrustedContact?> = mutex.withLock {
        Result.Success(contactsMap[id])
    }

    override suspend fun addTrustedContact(contact: TrustedContact): Result<Unit> = mutex.withLock {
        contactsMap[contact.id] = contact
        _contactsState.value = contactsMap.values.toList()
        Result.Success(Unit)
    }

    override suspend fun updateTrustedContact(contact: TrustedContact): Result<Unit> = mutex.withLock {
        contactsMap[contact.id] = contact
        _contactsState.value = contactsMap.values.toList()
        Result.Success(Unit)
    }

    override suspend fun removeTrustedContact(contactId: String): Result<Unit> = mutex.withLock {
        contactsMap.remove(contactId)
        _contactsState.value = contactsMap.values.toList()
        Result.Success(Unit)
    }

    override suspend fun setContactEnabled(contactId: String, isEnabled: Boolean): Result<Unit> = mutex.withLock {
        val existing = contactsMap[contactId]
        if (existing != null) {
            contactsMap[contactId] = existing.copy(
                isEnabled = isEnabled,
                updatedAt = System.currentTimeMillis()
            )
            _contactsState.value = contactsMap.values.toList()
        }
        Result.Success(Unit)
    }
}
