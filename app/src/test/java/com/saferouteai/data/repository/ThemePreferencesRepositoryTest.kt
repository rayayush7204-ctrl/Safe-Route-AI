package com.saferouteai.data.repository

import com.saferouteai.domain.model.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThemePreferencesRepositoryTest {

    private lateinit var repository: InMemoryThemePreferencesRepository

    @Before
    fun setUp() {
        repository = InMemoryThemePreferencesRepository()
    }

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun settingThemeMode_persistsCorrectly() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }
}
