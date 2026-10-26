package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AutoFreelancerTheme
import com.example.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            AutoFreelancerTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository, settingsRepository) as T
            }
        }
    )

    val currentScreen by dashboardViewModel.currentScreen.collectAsState()

    if (currentScreen == "SETTINGS") {
        SettingsScreen(
            viewModel = dashboardViewModel,
            onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") }
        )
    } else {
        DashboardScreen(
            viewModel = dashboardViewModel,
            onNavigateToSettings = { dashboardViewModel.navigateTo("SETTINGS") },
            onSignOutClicked = {
                dashboardViewModel.refreshTasks()
            }
        )
    }
}

