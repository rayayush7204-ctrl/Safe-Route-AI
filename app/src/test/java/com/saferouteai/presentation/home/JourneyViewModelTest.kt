package com.saferouteai.presentation.home

import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class JourneyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryJourneyRepository
    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryJourneyRepository()
        viewModel = JourneyViewModel(
            startJourneyUseCase = StartJourneyUseCase(repository),
            endJourneyUseCase = EndJourneyUseCase(repository),
            getJourneyStateUseCase = GetJourneyStateUseCase(repository),
            resetJourneyUseCase = ResetJourneyUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isIdleWithNoErrors() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(JourneyState.IDLE, state.journeyState)
        assertFalse(state.isJourneyActive)
        assertFalse(state.isJourneyCompleted)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun startJourney_updatesStateToActive() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.startJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.ACTIVE, state.journeyState)
        assertTrue(state.isJourneyActive)
        assertFalse(state.isJourneyCompleted)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun endJourney_updatesStateToCompleted() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.startJourney()
        advanceUntilIdle()
        assertEquals(JourneyState.ACTIVE, viewModel.uiState.value.journeyState)

        viewModel.endJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.COMPLETED, state.journeyState)
        assertFalse(state.isJourneyActive)
        assertTrue(state.isJourneyCompleted)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun resetJourney_returnsStateToIdle() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.startJourney()
        advanceUntilIdle()
        viewModel.endJourney()
        advanceUntilIdle()
        assertEquals(JourneyState.COMPLETED, viewModel.uiState.value.journeyState)

        viewModel.resetJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.IDLE, state.journeyState)

        collectJob.cancel()
    }

    @Test
    fun endJourney_whenNotActive_recordsErrorMessage() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }

        viewModel.endJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.IDLE, state.journeyState)
        assertTrue(state.errorMessage?.contains("Cannot end journey") == true)

        viewModel.clearError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)

        collectJob.cancel()
    }
}
