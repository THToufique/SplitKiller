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
        val THEME_KEY = stringPreferencesKey("theme")
        val SHOW_SYSTEM_APPS_KEY = booleanPreferencesKey("show_system_apps")
        val AUTO_MERGE_KEY = booleanPreferencesKey("auto_merge")
    }
    
    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }
    
    val showSystemAppsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_SYSTEM_APPS_KEY] ?: false
    }
    
    val autoMergeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_MERGE_KEY] ?: true
    }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
    
    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_SYSTEM_APPS_KEY] = show
        }
    }
    
    suspend fun setAutoMerge(autoMerge: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_MERGE_KEY] = autoMerge
        }
    }
}
