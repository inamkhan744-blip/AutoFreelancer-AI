package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AutoFreelancerTheme
import com.example.ui.theme.CyberPrimary
import com.example.ui.viewmodel.DashboardViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

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
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            user = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (user == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    user = Firebase.auth.currentUser
                },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                user = Firebase.auth.currentUser
            }
        )
    } else {
        val repository = remember(user?.uid) {
            TaskRepository(context)
        }
        val settingsRepository = remember {
            SettingsRepository(context)
        }
        val dashboardViewModel: DashboardViewModel = viewModel(
            key = user?.uid,
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
                    AuthManager.signOut(
                        context = context,
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                        onSignOutComplete = {
                            user = null
                        }
                    )
                }
            )
        }
    }
}
