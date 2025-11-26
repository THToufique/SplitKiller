package com.ripp3r.splitkiller.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel { INFO, WARNING, ERROR, SUCCESS }
enum class LogCategory { MERGE, EXTRACTION }

data class LogEntry(
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AppLogger {
    private val _mergeLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val mergeLogs: StateFlow<List<LogEntry>> = _mergeLogs.asStateFlow()

    private val _extractionLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val extractionLogs: StateFlow<List<LogEntry>> = _extractionLogs.asStateFlow()

    fun log(category: LogCategory, level: LogLevel, message: String) {
        val entry = LogEntry(level, message)
        when (category) {
            LogCategory.MERGE -> _mergeLogs.value = _mergeLogs.value + entry
            LogCategory.EXTRACTION -> _extractionLogs.value = _extractionLogs.value + entry
        }
        
        val tag = "SplitKiller_${category.name}"
        when (level) {
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.SUCCESS -> Log.i(tag, "✓ $message")
        }
    }

    fun clearLogs(category: LogCategory) {
        when (category) {
            LogCategory.MERGE -> _mergeLogs.value = emptyList()
            LogCategory.EXTRACTION -> _extractionLogs.value = emptyList()
        }
    }
}
