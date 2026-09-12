package com.saferouteai.presentation.profile

import com.saferouteai.data.repository.InMemoryUserProfileRepository
import com.saferouteai.domain.usecase.GetUserProfileUseCase
import com.saferouteai.domain.usecase.SaveUserProfileUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryUserProfileRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryUserProfileRepository()
        viewModel = ProfileViewModel(
            getUserProfileUseCase = GetUserProfileUseCase(repository),
            saveUserProfileUseCase = SaveUserProfileUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialProfileState_loadsCorrectly() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.displayName)
        assertNull(state.errorMessage)

        job.cancel()
    }

    @Test
    fun editingFields_updatesState() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onDisplayNameChanged("Tony Stark")
        viewModel.onPhoneNumberChanged("+1999999999")
        viewModel.onEmailChanged("tony@stark.com")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Tony Stark", state.displayName)
        assertEquals("+1999999999", state.phoneNumber)
        assertEquals("tony@stark.com", state.email)

        job.cancel()
    }

    @Test
    fun saveProfile_successFlow() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onDisplayNameChanged("Steve Rogers")
        viewModel.onPhoneNumberChanged("+1111111111")
        viewModel.saveProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertTrue(state.saveSuccess)
        assertNull(state.errorMessage)

        job.cancel()
    }

    @Test
    fun saveProfile_invalidEmail_recordsError() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onDisplayNameChanged("Steve")
        viewModel.onEmailChanged("invalid-email")
        viewModel.saveProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
        assertTrue(state.errorMessage?.contains("invalid") == true)

        job.cancel()
    }
}
