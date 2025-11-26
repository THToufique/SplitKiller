package com.ripp3r.splitkiller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ripp3r.splitkiller.data.KeystoreManager
import com.ripp3r.splitkiller.model.SigningKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyManagerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val keystoreManager = KeystoreManager(application)
    
    private val _keys = MutableStateFlow<List<SigningKey>>(emptyList())
    val keys: StateFlow<List<SigningKey>> = _keys.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadKeys()
    }
    
    fun loadKeys() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedKeys = keystoreManager.getAllKeys()
            _keys.value = loadedKeys
            _isLoading.value = false
        }
    }
    
    fun createKey(alias: String, password: String, validityYears: Int, cn: String, ou: String, o: String, l: String, st: String, c: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = keystoreManager.createKey(alias, password, validityYears, cn, ou, o, l, st, c)
            result.onSuccess {
                loadKeys()
            }.onFailure {
                _errorMessage.value = it.message
            }
            _isLoading.value = false
        }
    }
    
    fun deleteKey(keyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = keystoreManager.deleteKey(keyId)
            result.onSuccess {
                loadKeys()
            }.onFailure {
                _errorMessage.value = it.message
            }
            _isLoading.value = false
        }
    }
    
    fun deleteAllKeys() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = keystoreManager.deleteAllKeys()
            result.onSuccess {
                loadKeys()
            }.onFailure {
                _errorMessage.value = it.message
            }
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
