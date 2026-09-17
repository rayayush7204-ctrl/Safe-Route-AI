package com.saferouteai.data.repository

import com.saferouteai.data.local.datastore.ThemePreferencesDataStore
import com.saferouteai.domain.model.theme.ThemeMode
import com.saferouteai.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow

class DataStoreThemePreferencesRepository(
    private val themePreferencesDataStore: ThemePreferencesDataStore
) : ThemePreferencesRepository {

    override val themeMode: Flow<ThemeMode> = themePreferencesDataStore.themeModeFlow

    override suspend fun setThemeMode(mode: ThemeMode) {
        themePreferencesDataStore.setThemeMode(mode)
    }
}
