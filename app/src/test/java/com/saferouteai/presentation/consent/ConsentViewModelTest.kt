package com.saferouteai.presentation.consent

import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.domain.usecase.GetConsentUseCase
import com.saferouteai.domain.usecase.UpdateConsentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryConsentRepository
    private lateinit var viewModel: ConsentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryConsentRepository()
        viewModel = ConsentViewModel(
            getConsentUseCase = GetConsentUseCase(repository),
            updateConsentUseCase = UpdateConsentUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialConsentState_isStrictlyFalse() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.consent.locationSharingConsent)
        assertFalse(state.consent.trustedContactSharingConsent)
        assertNull(state.errorMessage)

        job.cancel()
    }

    @Test
    fun togglingLocationSharing_updatesConsent() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onLocationSharingConsentToggled(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consent.locationSharingConsent)
        assertFalse("Contacts consent must remain unchanged", state.consent.trustedContactSharingConsent)

        job.cancel()
    }

    @Test
    fun togglingTrustedContactSharing_updatesConsent() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }

        viewModel.onTrustedContactSharingConsentToggled(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consent.trustedContactSharingConsent)
        assertFalse("Location consent must remain unchanged", state.consent.locationSharingConsent)

        job.cancel()
    }
}
