package com.saferouteai.domain.usecase

import com.saferouteai.data.repository.InMemoryTrustedContactsRepository
import com.saferouteai.domain.model.ContactRelationship
import com.saferouteai.domain.model.TrustedContact
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrustedContactsUseCasesTest {

    private lateinit var repository: InMemoryTrustedContactsRepository
    private lateinit var getTrustedContactsUseCase: GetTrustedContactsUseCase
    private lateinit var addTrustedContactUseCase: AddTrustedContactUseCase
    private lateinit var updateTrustedContactUseCase: UpdateTrustedContactUseCase
    private lateinit var removeTrustedContactUseCase: RemoveTrustedContactUseCase
    private lateinit var setContactEnabledUseCase: SetContactEnabledUseCase

    @Before
    fun setUp() {
        repository = InMemoryTrustedContactsRepository()
        getTrustedContactsUseCase = GetTrustedContactsUseCase(repository)
        addTrustedContactUseCase = AddTrustedContactUseCase(repository)
        updateTrustedContactUseCase = UpdateTrustedContactUseCase(repository)
        removeTrustedContactUseCase = RemoveTrustedContactUseCase(repository)
        setContactEnabledUseCase = SetContactEnabledUseCase(repository)
    }

    @Test
    fun initialContacts_isEmpty() = runTest {
        val contacts = getTrustedContactsUseCase().first()
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun addValidContact_succeeds() = runTest {
        val contact = TrustedContact(
            name = "Clark Kent",
            relationship = ContactRelationship.FRIEND,
            phoneNumber = "+123456789"
        )
        val result = addTrustedContactUseCase(contact)
        assertTrue(result.isSuccess)

        val list = getTrustedContactsUseCase().first()
        assertEquals(1, list.size)
        assertEquals("Clark Kent", list[0].name)
    }

    @Test
    fun addContactWithBlankName_fails() = runTest {
        val contact = TrustedContact(
            name = "  ",
            phoneNumber = "+123456789"
        )
        val result = addTrustedContactUseCase(contact)
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun addContactWithoutPhoneAndEmail_fails() = runTest {
        val contact = TrustedContact(
            name = "Diana Prince",
            phoneNumber = "",
            email = ""
        )
        val result = addTrustedContactUseCase(contact)
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun updateContact_succeeds() = runTest {
        val contact = TrustedContact(
            name = "Barry Allen",
            phoneNumber = "+100000000"
        )
        addTrustedContactUseCase(contact)

        val updated = contact.copy(phoneNumber = "+199999999")
        val updateResult = updateTrustedContactUseCase(updated)
        assertTrue(updateResult.isSuccess)

        val list = getTrustedContactsUseCase().first()
        assertEquals("+199999999", list[0].phoneNumber)
    }

    @Test
    fun setContactEnabled_togglesState() = runTest {
        val contact = TrustedContact(
            name = "Arthur Curry",
            phoneNumber = "+123456789",
            isEnabled = true
        )
        addTrustedContactUseCase(contact)

        val disableResult = setContactEnabledUseCase(contact.id, false)
        assertTrue(disableResult.isSuccess)

        val list = getTrustedContactsUseCase().first()
        assertFalse(list[0].isEnabled)

        val enableResult = setContactEnabledUseCase(contact.id, true)
        assertTrue(enableResult.isSuccess)
        assertTrue(getTrustedContactsUseCase().first()[0].isEnabled)
    }

    @Test
    fun removeContact_deletesFromRepository() = runTest {
        val contact = TrustedContact(
            name = "Victor Stone",
            phoneNumber = "+123456789"
        )
        addTrustedContactUseCase(contact)
        assertEquals(1, getTrustedContactsUseCase().first().size)

        val removeResult = removeTrustedContactUseCase(contact.id)
        assertTrue(removeResult.isSuccess)
        assertTrue(getTrustedContactsUseCase().first().isEmpty())
    }
}
