package com.ripp3r.splitkiller.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLogger {
    private const val TAG = "SplitKiller"
    
    private val _extractionLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val extractionLogs: StateFlow<List<LogEntry>> = _extractionLogs.asStateFlow()
    
    private val _mergeLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val mergeLogs: StateFlow<List<LogEntry>> = _mergeLogs.asStateFlow()
    
    fun log(message: String, level: LogLevel = LogLevel.INFO, category: LogCategory = LogCategory.EXTRACTION) {
        val entry = LogEntry(
            message = message,
            level = level,
            timestamp = System.currentTimeMillis(),
            category = category
        )
        
        // Add to appropriate log stream
        when (category) {
            LogCategory.EXTRACTION -> {
                _extractionLogs.value = (_extractionLogs.value + entry).takeLast(100)
            }
            LogCategory.MERGE -> {
                _mergeLogs.value = (_mergeLogs.value + entry).takeLast(100)
            }
        }
        
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, "[${category.name}] $message")
            LogLevel.INFO -> Log.i(TAG, "[${category.name}] $message")
            LogLevel.WARNING -> Log.w(TAG, "[${category.name}] $message")
            LogLevel.ERROR -> Log.e(TAG, "[${category.name}] $message")
        }
    }
    
    fun clearExtraction() {
        _extractionLogs.value = emptyList()
    }
    
    fun clearMerge() {
        _mergeLogs.value = emptyList()
    }
    
    fun clearLogs() {
        _extractionLogs.value = emptyList()
        _mergeLogs.value = emptyList()
    }
}

data class LogEntry(
    val message: String,
    val level: LogLevel,
    val timestamp: Long,
    val category: LogCategory
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

enum class LogCategory {
    EXTRACTION, MERGE
}
