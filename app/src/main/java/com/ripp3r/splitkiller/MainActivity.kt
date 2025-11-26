package com.ripp3r.splitkiller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.ripp3r.splitkiller.ui.SplitKillerApp
import com.ripp3r.splitkiller.ui.theme.SplitKillerTheme
import com.ripp3r.splitkiller.util.AppLogger

class MainActivity : ComponentActivity() {
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Silent permission handling
    }
    
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Silent permission handling
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestStoragePermissions()
        
        setContent {
            val settingsManager = remember { com.ripp3r.splitkiller.data.SettingsManager(this) }
            val themePreference by settingsManager.themeFlow.collectAsState(initial = "System")
            
            SplitKillerTheme(themePreference = themePreference) {
                SplitKillerApp()
            }
        }
    }
    
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Request MANAGE_EXTERNAL_STORAGE for full access
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    AppLogger.log("Failed to open storage settings", com.ripp3r.splitkiller.util.LogLevel.ERROR)
                }
            }
        } else {
            // Android 10 and below - Request traditional permissions
            val permissions = mutableListOf<String>()
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            
            if (permissions.isNotEmpty()) {
                storagePermissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }
}