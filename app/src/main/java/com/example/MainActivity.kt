package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import com.example.data.repository.ClientRepository
import com.example.data.repository.InboxRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.ui.screens.ClientDeliverablesScreen
import com.example.ui.screens.ClientsCrmScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AutoFreelancerTheme
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTertiary
import com.example.ui.viewmodel.DashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private var isAuthenticated by mutableStateOf(false)
    private var startupError by mutableStateOf(AppStartupState.errorMessage)
    private var firebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.example.util.configureAppCheck(this, intent)
            enableEdgeToEdge()
            firebaseAuth = runCatching { Firebase.auth }
                .onFailure { Log.w("MainActivity", "Firebase Auth unavailable; showing sign-in screen.", it) }
                .getOrNull()
            isAuthenticated = firebaseAuth?.currentUser != null
            authStateListener = firebaseAuth?.let { FirebaseAuth.AuthStateListener { auth ->
                try {
                    isAuthenticated = auth.currentUser != null
                } catch (error: Exception) {
                    showStartupError(error)
                }
            } }
            if (isAuthenticated && SettingsRepository(this).settingsFlow.value.autoPilotEnabled) {
                com.example.worker.AutoPilotScheduler.schedule(this, true)
            }
        } catch (error: Exception) {
            showStartupError(error)
        }

        setContent {
            AutoFreelancerTheme {
                val error = startupError ?: AppStartupState.errorMessage
                if (error != null) {
                    StartupErrorScreen(error)
                } else if (isAuthenticated || firebaseAuth == null) {
                    AppRootContent()
                } else {
                    AuthScreen(onAuthSuccess = { isAuthenticated = true })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val auth = firebaseAuth ?: return
            authStateListener?.let(auth::addAuthStateListener)
        } catch (error: Exception) {
            showStartupError(error)
        }
    }

    override fun onStop() {
        try {
            val auth = firebaseAuth
            val listener = authStateListener
            if (auth != null && listener != null) auth.removeAuthStateListener(listener)
        } catch (error: Exception) {
            Log.w("MainActivity", "Unable to remove Firebase auth listener", error)
        }
        super.onStop()
    }

    private fun showStartupError(error: Throwable) {
        startupError = error.localizedMessage ?: "App initialization failed."
        AppStartupState.recordFailure(error)
    }
}

@Composable
private fun StartupErrorScreen(message: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("App initialization failed", style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val repository = remember { TaskRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val clientRepository = remember { ClientRepository(context) }
    val inboxRepository = remember { InboxRepository(context) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    clientRepository = clientRepository,
                    inboxRepository = inboxRepository
                ) as T
            }
        }
    )

    val currentScreen by dashboardViewModel.currentScreen.collectAsState()
    val inboxEmails by dashboardViewModel.inboxEmails.collectAsState()
    val pendingInboxCount = inboxEmails.count { it.status == "Awaiting Review" }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Tasks"
                        )
                    },
                    label = { Text("Tasks") },
                    selected = currentScreen == "DASHBOARD",
                    onClick = { dashboardViewModel.navigateTo("DASHBOARD") },
                    modifier = Modifier.testTag("nav_tab_tasks"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingInboxCount > 0) {
                                    Badge(
                                        containerColor = CyberSecondary,
                                        contentColor = androidx.compose.ui.graphics.Color.Black
                                    ) {
                                        Text("$pendingInboxCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailUnread,
                                contentDescription = "Inbox"
                            )
                        }
                    },
                    label = { Text("Inbox") },
                    selected = currentScreen == "INBOX",
                    onClick = { dashboardViewModel.navigateTo("INBOX") },
                    modifier = Modifier.testTag("nav_tab_inbox"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberSecondary,
                        selectedTextColor = CyberSecondary,
                        indicatorColor = CyberSecondary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Clients"
                        )
                    },
                    label = { Text("Clients") },
                    selected = currentScreen == "CLIENTS",
                    onClick = { dashboardViewModel.navigateTo("CLIENTS") },
                    modifier = Modifier.testTag("nav_tab_clients"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Deliverables"
                        )
                    },
                    label = { Text("Deliverables") },
                    selected = currentScreen == "DELIVERABLES",
                    onClick = { dashboardViewModel.navigateTo("DELIVERABLES") },
                    modifier = Modifier.testTag("nav_tab_deliverables"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    selected = currentScreen == "SETTINGS",
                    onClick = { dashboardViewModel.navigateTo("SETTINGS") },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberPrimary,
                        selectedTextColor = CyberPrimary,
                        indicatorColor = CyberPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                "INBOX" -> {
                    InboxScreen(
                        viewModel = dashboardViewModel,
                        onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") }
                    )
                }
                "CLIENTS" -> {
                    ClientsCrmScreen(
                        viewModel = dashboardViewModel,
                        onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") }
                    )
                }
                "DELIVERABLES" -> {
                    ClientDeliverablesScreen(
                        viewModel = dashboardViewModel,
                        onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") }
                    )
                }
                "SETTINGS" -> {
                    SettingsScreen(
                        viewModel = dashboardViewModel,
                        onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") }
                    )
                }
                else -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToSettings = { dashboardViewModel.navigateTo("SETTINGS") },
                        onSignOutClicked = {
                            dashboardViewModel.refreshTasks()
                        }
                    )
                }
            }
        }
    }
}

