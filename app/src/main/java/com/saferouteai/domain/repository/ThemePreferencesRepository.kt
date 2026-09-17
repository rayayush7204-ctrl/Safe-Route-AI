package com.saferouteai.domain.repository

import com.saferouteai.domain.model.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing local theme and appearance preferences.
 */
interface ThemePreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
