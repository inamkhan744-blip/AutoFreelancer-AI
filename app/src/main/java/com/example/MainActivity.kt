package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.ClientRepository
import com.example.data.repository.InboxRepository
import com.example.data.repository.OfflineSyncQueueRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AbTestingEngine
import com.example.ui.screens.ClientDeliverablesScreen
import com.example.ui.screens.ClientPortalScreen
import com.example.ui.screens.ClientsCrmScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AutoFreelancerTheme
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.viewmodel.DashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private var isAuthenticated by mutableStateOf(false)
    private var startupError by mutableStateOf(AppStartupState.errorMessage)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            lifecycleScope.launch {
                val auth = Firebase.auth
                ensureFirebaseAuthReady(auth)
                isAuthenticated = auth.currentUser != null
            }
        } catch (error: Exception) {
            showStartupError(error)
        }

        setContent {
            AutoFreelancerTheme {
                val error = startupError
                if (error != null) {
                    StartupErrorScreen(error)
                } else if (isAuthenticated) {
                    AppRootContent()
                } else {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Connecting to secure server...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    private suspend fun ensureFirebaseAuthReady(auth: FirebaseAuth) {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val isGoogleUser = currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                Log.d("Auth", "Authenticated user already available: ${currentUser.uid}, google=${isGoogleUser}")
                return
            }

            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            Log.d("Auth", "Anonymous login successful for uid: $uid")
            if (uid == null) {
                throw IllegalStateException("Firebase anonymous sign-in returned no user.")
            }
        } catch (error: Exception) {
            Log.e("Auth", "Firebase auth initialization failed before app boot", error)
            showStartupError(error)
        }
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
    val offlineSyncRepository = remember { OfflineSyncQueueRepository(context) }
    val abTestingEngine = remember { AbTestingEngine(context) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    clientRepository = clientRepository,
                    inboxRepository = inboxRepository,
                    offlineSyncRepository = offlineSyncRepository,
                    abTestingEngine = abTestingEngine
                ) as T
            }
        }
    )

    val currentScreen by dashboardViewModel.currentScreen.collectAsState()
    val inboxEmails by dashboardViewModel.inboxEmails.collectAsState()
    val selectedPortalClient by dashboardViewModel.selectedClientForPortal.collectAsState()
    val pendingInboxCount = inboxEmails.count { it.status == "Awaiting Review" }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Tasks") },
                    label = { Text("Tasks") },
                    selected = currentScreen == "DASHBOARD",
                    onClick = { dashboardViewModel.navigateTo("DASHBOARD") },
                    modifier = Modifier.testTag("nav_tab_tasks"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberPrimary, selectedTextColor = CyberPrimary, indicatorColor = CyberPrimary.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(badge = {
                            if (pendingInboxCount > 0) {
                                Badge(containerColor = CyberSecondary, contentColor = androidx.compose.ui.graphics.Color.Black) {
                                    Text("$pendingInboxCount")
                                }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.MarkEmailUnread, contentDescription = "Inbox")
                        }
                    },
                    label = { Text("Inbox") },
                    selected = currentScreen == "INBOX",
                    onClick = { dashboardViewModel.navigateTo("INBOX") },
                    modifier = Modifier.testTag("nav_tab_inbox"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberSecondary, selectedTextColor = CyberSecondary, indicatorColor = CyberSecondary.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Clients") },
                    label = { Text("Clients") },
                    selected = currentScreen == "CLIENTS" || currentScreen == "CLIENT_PORTAL",
                    onClick = { dashboardViewModel.navigateTo("CLIENTS") },
                    modifier = Modifier.testTag("nav_tab_clients"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberPrimary, selectedTextColor = CyberPrimary, indicatorColor = CyberPrimary.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = "Deliverables") },
                    label = { Text("Deliverables") },
                    selected = currentScreen == "DELIVERABLES",
                    onClick = { dashboardViewModel.navigateTo("DELIVERABLES") },
                    modifier = Modifier.testTag("nav_tab_deliverables"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberPrimary, selectedTextColor = CyberPrimary, indicatorColor = CyberPrimary.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == "SETTINGS",
                    onClick = { dashboardViewModel.navigateTo("SETTINGS") },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = CyberPrimary, selectedTextColor = CyberPrimary, indicatorColor = CyberPrimary.copy(alpha = 0.15f))
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
                "INBOX" -> InboxScreen(viewModel = dashboardViewModel, onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") })
                "CLIENTS" -> ClientsCrmScreen(viewModel = dashboardViewModel, onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") })
                "CLIENT_PORTAL" -> ClientPortalScreen(viewModel = dashboardViewModel, client = selectedPortalClient, onNavigateBack = { dashboardViewModel.closeClientPortal() })
                "DELIVERABLES" -> ClientDeliverablesScreen(viewModel = dashboardViewModel, onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") })
                "SETTINGS" -> SettingsScreen(viewModel = dashboardViewModel, onNavigateBack = { dashboardViewModel.navigateTo("DASHBOARD") })
                else -> DashboardScreen(viewModel = dashboardViewModel, onNavigateToSettings = { dashboardViewModel.navigateTo("SETTINGS") }, onSignOutClicked = { dashboardViewModel.refreshTasks() })
            }
        }
    }
}
