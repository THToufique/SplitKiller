package com.ripp3r.splitkiller.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ripp3r.splitkiller.data.ApkMerger
import com.ripp3r.splitkiller.data.KeystoreManager
import com.ripp3r.splitkiller.model.SignatureScheme
import com.ripp3r.splitkiller.model.SigningKey
import com.ripp3r.splitkiller.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MergeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val apkMerger = ApkMerger(application)
    private val keystoreManager = KeystoreManager(application)
    
    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()
    
    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()
    
    private val _selectedSignatureScheme = MutableStateFlow(SignatureScheme.V1_V2_V3)
    val selectedSignatureScheme: StateFlow<SignatureScheme> = _selectedSignatureScheme.asStateFlow()
    
    private val _selectedKey = MutableStateFlow<SigningKey?>(null)
    val selectedKey: StateFlow<SigningKey?> = _selectedKey.asStateFlow()
    
    private val _availableKeys = MutableStateFlow<List<SigningKey>>(emptyList())
    val availableKeys: StateFlow<List<SigningKey>> = _availableKeys.asStateFlow()
    
    private val _isMerging = MutableStateFlow(false)
    val isMerging: StateFlow<Boolean> = _isMerging.asStateFlow()
    
    private val _mergeSuccess = MutableStateFlow(false)
    val mergeSuccess: StateFlow<Boolean> = _mergeSuccess.asStateFlow()
    
    init {
        loadKeys()
    }
    
    fun loadKeys() {
        viewModelScope.launch {
            val keys = keystoreManager.getAllKeys()
            _availableKeys.value = keys
            _selectedKey.value = keys.find { it.isDefaultKey } ?: keys.firstOrNull()
        }
    }
    
    fun setSelectedFile(uri: Uri, fileName: String) {
        _selectedFileUri.value = uri
        _selectedFileName.value = fileName
        AppLogger.log("Selected file: $fileName", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
    }
    
    fun setSignatureScheme(scheme: SignatureScheme) {
        _selectedSignatureScheme.value = scheme
        AppLogger.log("Signature scheme: ${scheme.name}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
    }
    
    fun setSelectedKey(key: SigningKey?) {
        _selectedKey.value = key
        AppLogger.log("Selected key: ${key?.alias ?: "Unsigned"}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
    }
    
    fun clearSelection() {
        _selectedFileUri.value = null
        _selectedFileName.value = null
    }
    
    fun mergeAndSign() {
        val uri = _selectedFileUri.value
        val scheme = _selectedSignatureScheme.value
        val key = _selectedKey.value
        
        if (uri == null) {
            AppLogger.log("No file selected", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
            return
        }
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isMerging.value = true
            
            try {
                AppLogger.log("Starting merge process...", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                AppLogger.log("File: ${_selectedFileName.value}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                AppLogger.log("Signature: ${scheme.name}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                AppLogger.log("Key: ${key?.alias ?: "Unsigned"}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                
                val result = apkMerger.mergeApk(
                    uri = uri,
                    context = getApplication(),
                    signatureScheme = scheme,
                    signingKey = key,
                    selectedFileName = _selectedFileName.value
                )
                
                result.onSuccess { outputFile ->
                    AppLogger.log("✅ Merge successful: ${outputFile.name}", category = com.ripp3r.splitkiller.util.LogCategory.MERGE)
                    _mergeSuccess.value = true
                }.onFailure { error ->
                    AppLogger.log("✗ Merge failed: ${error.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
                }
            } catch (e: Exception) {
                AppLogger.log("✗ Exception during merge: ${e.message}", com.ripp3r.splitkiller.util.LogLevel.ERROR, com.ripp3r.splitkiller.util.LogCategory.MERGE)
            } finally {
                _isMerging.value = false
            }
        }
    }
    
    fun resetMergeSuccess() {
        _mergeSuccess.value = false
    }
    
    fun clearLog() {
        AppLogger.clearLogs()
    }
}
