package com.ripp3r.splitkiller.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ripp3r.splitkiller.ui.screens.*
import com.ripp3r.splitkiller.viewmodel.MergeViewModel

@Composable
fun SplitKillerApp() {
    val navController = rememberNavController()
    val mergeViewModel: MergeViewModel = viewModel()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("merge") { MergeScreen(navController, mergeViewModel) }
        composable("extract") { ExtractScreen(navController) }
        composable("keys") { KeyManagerScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
