package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.ConduceRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val repository = remember { ConduceRepository(context) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Administrador") } // Administrador o Usuario Normal

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun normalizeEmail(input: String): String {
        val trimmed = input.trim().lowercase()
        return if (trimmed.contains("@")) {
            trimmed
        } else {
            val safe = trimmed.replace(Regex("[^a-z0-9._-]"), "")
            if (safe.isBlank()) "usuario@pedralbes.app" else "$safe@pedralbes.app"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Logo Conduces de Obra",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Conduces de Obra",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "Control de Materiales, Firmas y Auditoría de PDFs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Main Auth Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Tabs: Iniciar Sesión / Registrarse
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.White,
                            contentColor = Color(0xFFF59E0B),
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Color(0xFFF59E0B),
                                    height = 3.dp
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    selectedTabIndex = 0
                                    errorMessage = null
                                },
                                text = {
                                    Text(
                                        text = "Iniciar Sesión",
                                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == 0) Color(0xFF0F172A) else Color(0xFF6B7280)
                                    )
                                },
                                modifier = Modifier.testTag("tab_login")
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    selectedTabIndex = 1
                                    errorMessage = null
                                },
                                text = {
                                    Text(
                                        text = "Crear Cuenta",
                                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == 1) Color(0xFF0F172A) else Color(0xFF6B7280)
                                    )
                                },
                                modifier = Modifier.testTag("tab_register")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (selectedTabIndex == 1) {
                            // Register Fields: Full Name
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nombre Completo *", color = Color(0xFF374151)) },
                                placeholder = { Text("Ej: Ing. Carlos Santos", color = Color(0xFF9CA3AF)) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4B5563))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFFD1D5DB)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("display_name_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Username or Email
                        OutlinedTextField(
                            value = usernameOrEmail,
                            onValueChange = { usernameOrEmail = it },
                            label = { Text("Usuario o Correo *", color = Color(0xFF374151)) },
                            placeholder = { Text("Ej: admin o supervisor1", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4B5563))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña *", color = Color(0xFF374151)) },
                            placeholder = { Text("Mínimo 6 caracteres", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4B5563))
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Ver contraseña",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        // Role Selector if Registering
                        if (selectedTabIndex == 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nivel de Seguridad:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RoleSelectorBadge(
                                    title = "👑 Administrador",
                                    subtitle = "Acceso total y bajas",
                                    isSelected = selectedRole == "Administrador",
                                    onClick = { selectedRole = "Administrador" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_admin_radio")
                                )
                                RoleSelectorBadge(
                                    title = "👤 Usuario Normal",
                                    subtitle = "Despacho y firmas",
                                    isSelected = selectedRole == "Usuario Normal",
                                    onClick = { selectedRole = "Usuario Normal" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("role_user_radio")
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFDC2626),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (usernameOrEmail.isBlank() || password.isBlank()) {
                                    errorMessage = "Por favor ingresa usuario y contraseña"
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null

                                val email = normalizeEmail(usernameOrEmail)

                                coroutineScope.launch {
                                    try {
                                        if (selectedTabIndex == 0) {
                                            // Sign In
                                            Firebase.auth.signInWithEmailAndPassword(email, password).await()
                                            onAuthSuccess()
                                        } else {
                                            // Register
                                            val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                                            val user = authResult.user
                                            if (user != null) {
                                                val profile = UserProfile(
                                                    uid = user.uid,
                                                    username = usernameOrEmail.trim(),
                                                    displayName = displayName.ifBlank { usernameOrEmail.trim() },
                                                    email = email,
                                                    role = selectedRole,
                                                    securityLevel = if (selectedRole == "Administrador") 1 else 2,
                                                    userId = user.uid
                                                )
                                                repository.saveUserProfile(profile)
                                            }
                                            onAuthSuccess()
                                        }
                                    } catch (e: FirebaseAuthException) {
                                        Log.e("Auth", "Firebase auth failed: ${e.errorCode}", e)
                                        errorMessage = when (e.errorCode) {
                                            "ERROR_USER_NOT_FOUND" -> "El usuario no existe. Regístrate en la pestaña 'Crear Cuenta'."
                                            "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta."
                                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este usuario ya está registrado. Inicia sesión en la primera pestaña."
                                            "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil (mínimo 6 caracteres)."
                                            else -> e.localizedMessage ?: "Error de autenticación."
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Auth", "General auth failure", e)
                                        errorMessage = e.localizedMessage ?: "Ocurrió un error. Intenta de nuevo."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF0F172A),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = if (selectedTabIndex == 0) "Ingresar al Sistema" else "Crear y Entrar",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick demo access buttons (so user can test admin and normal immediately without typing)
                        Text(
                            text = "Acceso Rápido Directo (Sin Google):",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    usernameOrEmail = "admin"
                                    password = "admin123"
                                    selectedTabIndex = 0
                                    isLoading = true
                                    errorMessage = null
                                    val email = normalizeEmail("admin")
                                    coroutineScope.launch {
                                        try {
                                            Firebase.auth.signInWithEmailAndPassword(email, "admin123").await()
                                            onAuthSuccess()
                                        } catch (e: Exception) {
                                            // Create demo admin if first time
                                            try {
                                                val res = Firebase.auth.createUserWithEmailAndPassword(email, "admin123").await()
                                                res.user?.let { u ->
                                                    repository.saveUserProfile(
                                                        UserProfile(
                                                            uid = u.uid,
                                                            username = "admin",
                                                            displayName = "Administrador Principal",
                                                            email = email,
                                                            role = "Administrador",
                                                            securityLevel = 1,
                                                            userId = u.uid
                                                        )
                                                    )
                                                }
                                                onAuthSuccess()
                                            } catch (createErr: Exception) {
                                                errorMessage = "Acceso rápido admin: ${createErr.localizedMessage}"
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("quick_admin_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "👑 Demo Admin",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    usernameOrEmail = "operador"
                                    password = "operador123"
                                    selectedTabIndex = 0
                                    isLoading = true
                                    errorMessage = null
                                    val email = normalizeEmail("operador")
                                    coroutineScope.launch {
                                        try {
                                            Firebase.auth.signInWithEmailAndPassword(email, "operador123").await()
                                            onAuthSuccess()
                                        } catch (e: Exception) {
                                            // Create demo user if first time
                                            try {
                                                val res = Firebase.auth.createUserWithEmailAndPassword(email, "operador123").await()
                                                res.user?.let { u ->
                                                    repository.saveUserProfile(
                                                        UserProfile(
                                                            uid = u.uid,
                                                            username = "operador",
                                                            displayName = "Usuario Operativo",
                                                            email = email,
                                                            role = "Usuario Normal",
                                                            securityLevel = 2,
                                                            userId = u.uid
                                                        )
                                                    )
                                                }
                                                onAuthSuccess()
                                            } catch (createErr: Exception) {
                                                errorMessage = "Acceso rápido usuario: ${createErr.localizedMessage}"
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("quick_user_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "👤 Demo Usuario",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Optional Google Sign-In divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                    Text(
                        text = " o continuar con Google ",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Google Sign In button as alternative
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acceder con Cuenta de Google",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSelectorBadge(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF3C7) else Color(0xFFF3F4F6)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFD97706) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = Color(0xFF4B5563)
                )
            )
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Configuración de Google Sign-In faltante: default_web_client_id")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Tipo de credencial no esperado")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Fallo al iniciar sesión")
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
