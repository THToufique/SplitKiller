package com.ripp3r.splitkiller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ripp3r.splitkiller.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    
    val theme: StateFlow<String> = settingsManager.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System"
    )
    
    val showSystemApps: StateFlow<Boolean> = settingsManager.showSystemAppsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    val autoMerge: StateFlow<Boolean> = settingsManager.autoMergeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    
    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }
    
    fun setShowSystemApps(show: Boolean) {
        viewModelScope.launch {
            settingsManager.setShowSystemApps(show)
        }
    }
    
    fun setAutoMerge(autoMerge: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoMerge(autoMerge)
        }
    }
}
