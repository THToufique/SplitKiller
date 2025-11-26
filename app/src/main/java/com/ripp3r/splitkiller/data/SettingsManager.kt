package com.ripp3r.splitkiller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val SHOW_SYSTEM_APPS_KEY = booleanPreferencesKey("show_system_apps")
    }

    val theme: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val showSystemApps: Flow<Boolean> = context.dataStore.data.map { it[SHOW_SYSTEM_APPS_KEY] ?: false }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { it[SHOW_SYSTEM_APPS_KEY] = show }
    }
}
