package com.saferouteai.data.repository

import com.saferouteai.domain.model.theme.ThemeMode
import com.saferouteai.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryThemePreferencesRepository(
    initialMode: ThemeMode = ThemeMode.SYSTEM
) : ThemePreferencesRepository {

    private val _themeMode = MutableStateFlow(initialMode)
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
