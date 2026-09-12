package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing trusted emergency contacts.
 */
interface TrustedContactsRepository {
    /**
     * Observes the stream of configured trusted contacts.
     */
    fun getTrustedContacts(): Flow<List<TrustedContact>>

    /**
     * Retrieves a single contact by its identifier.
     */
    suspend fun getContactById(id: String): Result<TrustedContact?>

    /**
     * Adds a new trusted contact.
     */
    suspend fun addTrustedContact(contact: TrustedContact): Result<Unit>

    /**
     * Updates an existing trusted contact.
     */
    suspend fun updateTrustedContact(contact: TrustedContact): Result<Unit>

    /**
     * Removes a contact by ID.
     */
    suspend fun removeTrustedContact(contactId: String): Result<Unit>

    /**
     * Toggles whether a contact is enabled or disabled.
     */
    suspend fun setContactEnabled(contactId: String, isEnabled: Boolean): Result<Unit>
}
