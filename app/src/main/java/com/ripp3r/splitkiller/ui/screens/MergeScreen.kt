package com.ripp3r.splitkiller.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ripp3r.splitkiller.model.SignatureScheme
import com.ripp3r.splitkiller.util.AppLogger
import com.ripp3r.splitkiller.util.LogLevel
import com.ripp3r.splitkiller.viewmodel.MergeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(navController: NavController, viewModel: MergeViewModel) {
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val selectedFileName by viewModel.selectedFileName.collectAsState()
    val selectedScheme by viewModel.selectedSignatureScheme.collectAsState()
    val selectedKey by viewModel.selectedKey.collectAsState()
    val availableKeys by viewModel.availableKeys.collectAsState()
    val logs by AppLogger.mergeLogs.collectAsState()
    val listState = rememberLazyListState()
    
    var showSchemeMenu by remember { mutableStateOf(false) }
    var showKeyMenu by remember { mutableStateOf(false) }
    var showFileBrowser by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadKeys()
    }
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    if (showFileBrowser) {
        FileBrowserSheet(
            onDismiss = { showFileBrowser = false },
            onFileSelected = { file ->
                viewModel.setSelectedFile(
                    android.net.Uri.fromFile(file),
                    file.name
                )
            }
        )
    }
    
    if (showSchemeMenu) {
        ModalBottomSheet(
            onDismissRequest = { showSchemeMenu = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Signature Scheme", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                SignatureScheme.values().forEach { scheme ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            viewModel.setSignatureScheme(scheme)
                            showSchemeMenu = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (scheme == selectedScheme) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (scheme == selectedScheme) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(scheme.displayName, fontWeight = if (scheme == selectedScheme) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    
    if (showKeyMenu) {
        ModalBottomSheet(
            onDismissRequest = { showKeyMenu = false },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Signing Key", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                availableKeys.forEach { key ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            viewModel.setSelectedKey(key)
                            showKeyMenu = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (key == selectedKey) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (key == selectedKey) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(key.alias, fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Normal)
                                if (key.isDefaultKey) {
                                    Text("Default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        showKeyMenu = false
                        navController.navigate("keys")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Custom Key")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge APK", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File Selection
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showFileBrowser = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Select APK/XAPK", fontWeight = FontWeight.Medium)
                        if (selectedFileName != null) {
                            Text(selectedFileName!!, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
            
            // Signature Scheme
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showSchemeMenu = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Signature Scheme", fontWeight = FontWeight.Medium)
                        Text(selectedScheme.displayName, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            
            // Signing Key
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showKeyMenu = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Signing Key", fontWeight = FontWeight.Medium)
                        Text(selectedKey?.alias ?: "None", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            
            // Merge Button
            Button(
                onClick = { viewModel.mergeAndSign() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFileUri != null
            ) {
                Icon(Icons.Default.MergeType, null)
                Spacer(Modifier.width(8.dp))
                Text("Merge & Sign")
            }
            
            Spacer(Modifier.height(8.dp))
            
            // MT Manager and Clear buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MT Manager Button
                Button(
                    onClick = {
                        val context = navController.context
                        val packageNames = listOf("bin.mt.plus", "bin.mt.plus.canary")
                        var launched = false
                        
                        for (packageName in packageNames) {
                            try {
                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                if (intent != null) {
                                    context.startActivity(intent)
                                    launched = true
                                    break
                                }
                            } catch (e: Exception) {
                                // Try next package
                            }
                        }
                        
                        if (!launched) {
                            android.widget.Toast.makeText(context, "MT Manager not installed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("MT Manager")
                }
                
                // Clear Log Button
                Button(
                    onClick = { viewModel.clearLog() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Log")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Console Log
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Console Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Divider()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { log ->
                            val (icon, iconColor) = when (log.level) {
                                LogLevel.ERROR -> Icons.Default.Error to Color(0xFFFF5252)
                                LogLevel.WARNING -> Icons.Default.Warning to Color(0xFFFFB74D)
                                LogLevel.INFO -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                                else -> Icons.Default.Circle to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = when (log.level) {
                                        LogLevel.ERROR -> Color(0xFFFF5252)
                                        LogLevel.WARNING -> Color(0xFFFFB74D)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
