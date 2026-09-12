package com.saferouteai.presentation.contacts

import com.saferouteai.data.repository.InMemoryTrustedContactsRepository
import com.saferouteai.domain.model.ContactRelationship
import com.saferouteai.domain.model.TrustedContact
import com.saferouteai.domain.usecase.AddTrustedContactUseCase
import com.saferouteai.domain.usecase.GetTrustedContactsUseCase
import com.saferouteai.domain.usecase.RemoveTrustedContactUseCase
import com.saferouteai.domain.usecase.SetContactEnabledUseCase
import com.saferouteai.domain.usecase.UpdateTrustedContactUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrustedContactsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryTrustedContactsRepository
    private lateinit var viewModel: TrustedContactsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryTrustedContactsRepository()
        viewModel = TrustedContactsViewModel(
            getTrustedContactsUseCase = GetTrustedContactsUseCase(repository),
            addTrustedContactUseCase = AddTrustedContactUseCase(repository),
            updateTrustedContactUseCase = UpdateTrustedContactUseCase(repository),
            removeTrustedContactUseCase = RemoveTrustedContactUseCase(repository),
            setContactEnabledUseCase = SetContactEnabledUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialContactsState_isEmpty() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.contacts.isEmpty())
        assertFalse(state.isAddEditOpen)

        job.cancel()
    }

    @Test
    fun openAndDismissAddDialog() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.openAddContactDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddEditOpen)
        assertNull(viewModel.uiState.value.selectedContactForEdit)

        viewModel.dismissDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAddEditOpen)

        job.cancel()
    }

    @Test
    fun saveContact_addsNewContact() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        val contact = TrustedContact(
            name = "Pepper Potts",
            relationship = ContactRelationship.PARTNER,
            phoneNumber = "+1888888888"
        )
        viewModel.saveContact(contact)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.contacts.size)
        assertEquals("Pepper Potts", state.contacts[0].name)
        assertFalse(state.isAddEditOpen)
        assertNotNull(state.successMessage)

        job.cancel()
    }

    @Test
    fun toggleContactEnabled_updatesState() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        val contact = TrustedContact(
            name = "Happy Hogan",
            phoneNumber = "+1777777777",
            isEnabled = true
        )
        viewModel.saveContact(contact)
        advanceUntilIdle()

        viewModel.toggleContactEnabled(contact.id, false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.contacts[0].isEnabled)

        job.cancel()
    }

    @Test
    fun deleteContact_removesFromState() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        val contact = TrustedContact(
            name = "Rhodey",
            phoneNumber = "+1666666666"
        )
        viewModel.saveContact(contact)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.contacts.size)

        viewModel.deleteContact(contact.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.contacts.isEmpty())
        assertEquals("Contact removed", state.successMessage)

        job.cancel()
    }
}
