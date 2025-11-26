package com.ripp3r.splitkiller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ripp3r.splitkiller.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel()
    val selectedTheme by viewModel.theme.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val autoMerge by viewModel.autoMerge.collectAsState()
    
    var showThemePicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader("Appearance")
            
            SettingsCard {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = selectedTheme,
                        onClick = { showThemePicker = true }
                    )
                }
            }
            
            // APK Operations Section
            SettingsSectionHeader("APK Operations")
            
            SettingsCard {
                Column {
                    SettingsSwitchItem(
                        icon = Icons.Default.Android,
                        title = "Show System Apps",
                        subtitle = "Display system apps in extraction",
                        checked = showSystemApps,
                        onCheckedChange = { viewModel.setShowSystemApps(it) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 56.dp))
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.Merge,
                        title = "Auto-Merge Splits",
                        subtitle = "Automatically merge split APKs on extraction",
                        checked = autoMerge,
                        onCheckedChange = { viewModel.setAutoMerge(it) }
                    )
                }
            }
            
            // About Section
            SettingsSectionHeader("About")
            
            SettingsCard {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "1.0.0",
                        onClick = { }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 56.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = "Developer",
                        subtitle = "Built with Kotlin & Jetpack Compose",
                        onClick = { }
                    )
                }
            }
        }
    }
    
    // Theme Picker Dialog
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
            title = { Text("Choose Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption("Light", selectedTheme) {
                        viewModel.setTheme("Light")
                        showThemePicker = false
                    }
                    ThemeOption("Dark", selectedTheme) {
                        viewModel.setTheme("Dark")
                        showThemePicker = false
                    }
                    ThemeOption("System", selectedTheme) {
                        viewModel.setTheme("System")
                        showThemePicker = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    theme: String,
    selectedTheme: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (theme == selectedTheme) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = theme == selectedTheme,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = theme,
            style = MaterialTheme.typography.bodyLarge,
            color = if (theme == selectedTheme) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
