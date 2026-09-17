package com.saferouteai.presentation.settings

import com.saferouteai.data.repository.InMemoryThemePreferencesRepository
import com.saferouteai.domain.model.theme.ThemeMode
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryThemePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryThemePreferencesRepository()
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialThemeMode_isSystemDefault() = runTest(testDispatcher) {
        val job = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        job.cancel()
    }

    @Test
    fun setThemeMode_toDark_updatesThemeMode() = runTest(testDispatcher) {
        val job = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        job.cancel()
    }

    @Test
    fun setThemeMode_toLight_updatesThemeMode() = runTest(testDispatcher) {
        val job = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        job.cancel()
    }

    @Test
    fun setThemeMode_backToSystem_updatesThemeMode() = runTest(testDispatcher) {
        val job = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        job.cancel()
    }
}
