package com.ripp3r.splitkiller.viewmodel

import android.app.Application
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ripp3r.splitkiller.data.AppExtractor
import com.ripp3r.splitkiller.model.AppInfo
import com.ripp3r.splitkiller.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ExtractViewModel(application: Application) : AndroidViewModel(application) {
    
    private val appExtractor = AppExtractor(application)
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()
    
    private val _showConfirmDialog = MutableStateFlow<AppInfo?>(null)
    val showConfirmDialog: StateFlow<AppInfo?> = _showConfirmDialog.asStateFlow()
    
    private val _extractionSuccess = MutableStateFlow(false)
    val extractionSuccess: StateFlow<Boolean> = _extractionSuccess.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()
    
    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()
    
    init {
        loadApps()
    }
    
    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedApps = appExtractor.getInstalledApps(includeSystemApps = _showSystemApps.value)
            _apps.value = loadedApps
            filterApps()
            _isLoading.value = false
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps()
    }
    
    fun toggleSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
        loadApps()
    }
    
    private fun filterApps() {
        val query = _searchQuery.value.lowercase()
        _filteredApps.value = if (query.isBlank()) {
            _apps.value
        } else {
            _apps.value.filter {
                it.appName.lowercase().contains(query) ||
                it.packageName.lowercase().contains(query)
            }
        }
    }
    
    fun toggleAppSelection(packageName: String) {
        _selectedApps.value = if (_selectedApps.value.contains(packageName)) {
            _selectedApps.value - packageName
        } else {
            _selectedApps.value + packageName
        }
    }
    
    fun clearSelection() {
        _selectedApps.value = emptySet()
    }
    
    fun showExtractConfirmation(appInfo: AppInfo) {
        _showConfirmDialog.value = appInfo
    }
    
    fun dismissConfirmDialog() {
        _showConfirmDialog.value = null
    }
    
    fun extractApp(appInfo: AppInfo) {
        viewModelScope.launch {
            _showConfirmDialog.value = null
            
            // Use /storage/emulated/0/SplitKiller
            val outputDir = File(Environment.getExternalStorageDirectory(), "SplitKiller")
            
            // Create folder if it doesn't exist
            if (!outputDir.exists()) {
                val created = outputDir.mkdirs()
                if (created) {
                    AppLogger.log("Created folder: ${outputDir.absolutePath}")
                } else {
                    AppLogger.log("Failed to create folder", com.ripp3r.splitkiller.util.LogLevel.ERROR)
                }
            }
            
            AppLogger.log("Extracting ${appInfo.appName}...")
            
            val result = appExtractor.extractApp(appInfo, outputDir)
            result.onSuccess { file ->
                AppLogger.log("✓ Saved: ${file.name}")
                _extractionSuccess.value = true
            }.onFailure { error ->
                AppLogger.log("✗ Failed: ${error.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
            }
        }
    }
    
    fun extractSelectedApps() {
        if (_selectedApps.value.isEmpty()) return
        
        val appsToExtract = _apps.value.filter { _selectedApps.value.contains(it.packageName) }
        val selectedCount = appsToExtract.size
        
        // Clear selection immediately
        _selectedApps.value = emptySet()
        
        // Navigate to main screen
        _extractionSuccess.value = true
        
        // Use GlobalScope to prevent cancellation when navigating away
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isExtracting.value = true
            
            AppLogger.log("Starting extraction of $selectedCount apps...")
            
            val outputDir = File(Environment.getExternalStorageDirectory(), "SplitKiller")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            var successCount = 0
            appsToExtract.forEachIndexed { index, app ->
                try {
                    AppLogger.log("[${index + 1}/$selectedCount] Extracting ${app.appName}...")
                    
                    val result = appExtractor.extractApp(app, outputDir)
                    result.onSuccess { file ->
                        successCount++
                        AppLogger.log("✓ Saved: ${file.name}")
                    }.onFailure { error ->
                        AppLogger.log("✗ Failed: ${error.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
                    }
                } catch (e: Exception) {
                    AppLogger.log("✗ Exception for ${app.appName}: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR)
                }
            }
            
            AppLogger.log("✅ Extraction complete: $successCount/$selectedCount successful")
            _isExtracting.value = false
        }
    }
    
    fun resetExtractionSuccess() {
        _extractionSuccess.value = false
    }
}
