package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.local.dao.TrustedContactDao
import com.saferouteai.data.local.entity.TrustedContactEntity
import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.repository.TrustedContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTrustedContactsRepository(
    private val trustedContactDao: TrustedContactDao
) : TrustedContactsRepository {

    override fun getTrustedContacts(): Flow<List<TrustedContact>> {
        return trustedContactDao.getAllContacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getContactById(id: String): Result<TrustedContact?> {
        return try {
            val contact = trustedContactDao.getContactById(id)?.toDomain()
            Result.Success(contact)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addTrustedContact(contact: TrustedContact): Result<Unit> {
        return try {
            trustedContactDao.insertOrUpdate(TrustedContactEntity.fromDomain(contact))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateTrustedContact(contact: TrustedContact): Result<Unit> {
        return try {
            trustedContactDao.insertOrUpdate(TrustedContactEntity.fromDomain(contact))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeTrustedContact(contactId: String): Result<Unit> {
        return try {
            trustedContactDao.deleteById(contactId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setContactEnabled(contactId: String, isEnabled: Boolean): Result<Unit> {
        return try {
            trustedContactDao.updateEnabled(
                id = contactId,
                isEnabled = isEnabled,
                updatedAt = System.currentTimeMillis()
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
