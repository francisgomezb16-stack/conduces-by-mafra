package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.model.Conduce
import com.example.data.repository.ConduceRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConduceCreateEditScreen
import com.example.ui.screens.ConduceDetailScreen
import com.example.ui.screens.ConduceListScreen
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ConduceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class Screen {
    LIST,
    CREATE_EDIT,
    DETAIL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(currentUser == null) }

    // Silently restore session on cold launch so the user is never asked to log in again
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isCheckingAuth = false
                },
                onUnauthenticated = {
                    isCheckingAuth = false
                },
                scope = coroutineScope
            )
        } else {
            isCheckingAuth = false
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isCheckingAuth = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFF59E0B))
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val repository = remember { ConduceRepository(context) }
        val viewModel = remember { ConduceViewModel(repository) }

        AppNavigation(
            viewModel = viewModel,
            onSignOut = {
                currentUser = null
            }
        )
    }
}

@Composable
fun AppNavigation(
    viewModel: ConduceViewModel,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.LIST) }
    var selectedConduce by remember { mutableStateOf<Conduce?>(null) }

    when (currentScreen) {
        Screen.LIST -> {
            ConduceListScreen(
                viewModel = viewModel,
                onNavigateToCreate = {
                    selectedConduce = null
                    currentScreen = Screen.CREATE_EDIT
                },
                onNavigateToDetail = { conduce ->
                    selectedConduce = conduce
                    currentScreen = Screen.DETAIL
                },
                onSignOut = onSignOut
            )
        }
        Screen.DETAIL -> {
            selectedConduce?.let { conduce ->
                ConduceDetailScreen(
                    conduce = conduce,
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.LIST },
                    onNavigateToEdit = { editConduce ->
                        selectedConduce = editConduce
                        currentScreen = Screen.CREATE_EDIT
                    }
                )
            } ?: run {
                currentScreen = Screen.LIST
            }
        }
        Screen.CREATE_EDIT -> {
            ConduceCreateEditScreen(
                existingConduce = selectedConduce,
                viewModel = viewModel,
                onBack = {
                    currentScreen = if (selectedConduce != null) Screen.DETAIL else Screen.LIST
                },
                onConduceSaved = { savedConduce ->
                    selectedConduce = savedConduce
                    currentScreen = Screen.DETAIL
                }
            )
        }
    }
}
