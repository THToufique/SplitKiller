package com.ripp3r.splitkiller.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ripp3r.splitkiller.model.AppInfo
import com.ripp3r.splitkiller.viewmodel.ExtractViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(navController: NavController) {
    val viewModel: ExtractViewModel = viewModel()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val extractionSuccess by viewModel.extractionSuccess.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isExtracting by viewModel.isExtracting.collectAsState()
    
    // Navigate back to main screen after successful extraction
    LaunchedEffect(extractionSuccess) {
        if (extractionSuccess) {
            navController.navigateUp()
            viewModel.resetExtractionSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (selectedApps.isEmpty()) "Extract Apps" else "${selectedApps.size} Selected",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "${filteredApps.size} apps found",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (selectedApps.isNotEmpty()) {
                                viewModel.clearSelection()
                            } else {
                                navController.navigateUp()
                            }
                        }) {
                            Icon(
                                if (selectedApps.isNotEmpty()) Icons.Default.Close else Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSystemApps() }) {
                            Icon(
                                imageVector = if (showSystemApps) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showSystemApps) "Hide System Apps" else "Show System Apps",
                                tint = if (showSystemApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedApps.isNotEmpty() && !isExtracting) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.extractSelectedApps() },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    text = { Text("Extract ${selectedApps.size}") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (filteredApps.isEmpty() && !isLoading) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Apps Found",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search" else "No installed apps available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha =0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredApps) { app ->
                        AppCard(
                            app = app,
                            isSelected = selectedApps.contains(app.packageName),
                            onClick = {
                                if (selectedApps.isEmpty()) {
                                    viewModel.showExtractConfirmation(app)
                                } else {
                                    viewModel.toggleAppSelection(app.packageName)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleAppSelection(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation Dialog
    showConfirmDialog?.let { appToExtract ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Extract ${appToExtract.appName}?") },
            text = { 
                Column {
                    Text("This will extract the app to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "/storage/emulated/0/SplitKiller/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${appToExtract.appName}_${appToExtract.versionName}.${if (appToExtract.isSplitApk) "xapk" else "apk"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.extractApp(appToExtract) }
                ) {
                    Text("Extract")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppCard(
    app: AppInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox (shown when in selection mode)
            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // App Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                } ?: Icon(
                   imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "SYSTEM",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (app.isSplitApk) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "SPLIT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "v${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    
                    Text(
                        text = formatFileSize(app.appSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Extract",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
