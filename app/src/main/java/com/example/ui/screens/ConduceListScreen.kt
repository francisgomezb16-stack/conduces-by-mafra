package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import com.example.ui.components.AuditLogsDialog
import com.example.ui.components.CompanySettingsDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.viewmodel.ConduceViewModel
import com.example.ui.viewmodel.ConducesUiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConduceListScreen(
    viewModel: ConduceViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Conduce) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val conducesUiState by viewModel.conducesState.collectAsStateWithLifecycle()
    val companyProfile by viewModel.companyProfileState.collectAsStateWithLifecycle()
    val projects by viewModel.projectsState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfileState.collectAsStateWithLifecycle()
    val deletionLogs by viewModel.deletionLogsState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var showCompanyDialog by remember { mutableStateOf(false) }
    var showAuditDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    if (showCompanyDialog) {
        CompanySettingsDialog(
            initialProfile = companyProfile ?: CompanyProfile(),
            projects = projects,
            onDismiss = { showCompanyDialog = false },
            onSaveProfile = { updatedProfile ->
                viewModel.saveCompanyProfile(updatedProfile)
            },
            onSaveProject = { newProject ->
                viewModel.saveProject(newProject)
            },
            onDeleteProject = { projectId ->
                viewModel.deleteProject(projectId)
            }
        )
    }

    if (showAuditDialog) {
        AuditLogsDialog(
            logs = deletionLogs,
            onDismiss = { showAuditDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = userProfile,
            onDismiss = { showProfileDialog = false },
            onUpdateProfile = { updated ->
                viewModel.saveUserProfile(updated)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conduces de Obra",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Clickable Role Badge (Admin / Usuario)
                            val isAdmin = userProfile.role == "Administrador" || userProfile.securityLevel == 1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAdmin) Color(0xFFF59E0B) else Color(0xFF334155))
                                    .clickable { showProfileDialog = true }
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("user_role_badge")
                            ) {
                                Text(
                                    text = if (isAdmin) "👑 Admin" else "👤 Usuario",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isAdmin) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "${companyProfile?.companyName?.take(20) ?: "Pedralbes"} • ${userProfile.displayName.ifBlank { userProfile.username.ifBlank { "Usuario" } }}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Audit Logs of deleted conduces and PDFs
                    IconButton(
                        onClick = { showAuditDialog = true },
                        modifier = Modifier.testTag("audit_logs_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = "Bitácora de Auditoría",
                                tint = if (deletionLogs.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF94A3B8)
                            )
                            if (deletionLogs.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Company & Project settings
                    IconButton(
                        onClick = { showCompanyDialog = true },
                        modifier = Modifier.testTag("company_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Configurar Empresa y Proyectos",
                            tint = Color.White
                        )
                    }

                    // Sign Out
                    IconButton(
                        onClick = {
                            signOutUser(context, credentialManager, onSignOut, coroutineScope)
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF0F172A),
                modifier = Modifier.testTag("create_conduce_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Conduce")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por folio, obra, chofer o ítem...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_conduce_input")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Todos", "En Tránsito", "Entregado", "Borrador")
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (val state = conducesUiState) {
                is ConducesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B))
                    }
                }
                is ConducesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is ConducesUiState.Success -> {
                    val allConduces = state.conduces

                    // Metrics Strip
                    MetricsHeader(conduces = allConduces)

                    val filtered = allConduces.filter { c ->
                        val matchesFilter = when (selectedFilter) {
                            "Todos" -> true
                            else -> c.status.equals(selectedFilter, ignoreCase = true)
                        }
                        val query = searchQuery.trim().lowercase(Locale.getDefault())
                        val matchesSearch = query.isEmpty() ||
                                c.conduceNumber.lowercase(Locale.getDefault()).contains(query) ||
                                c.originProject.lowercase(Locale.getDefault()).contains(query) ||
                                c.destinationProject.lowercase(Locale.getDefault()).contains(query) ||
                                c.carrierName.lowercase(Locale.getDefault()).contains(query) ||
                                c.getParsedItems().any { it.description.lowercase(Locale.getDefault()).contains(query) }

                        matchesFilter && matchesSearch
                    }

                    if (filtered.isEmpty()) {
                        EmptyConducesView(
                            hasSearch = searchQuery.isNotBlank() || selectedFilter != "Todos",
                            onCreateClick = onNavigateToCreate
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered, key = { it.id }) { conduce ->
                                ConduceItemCard(
                                    conduce = conduce,
                                    onClick = { onNavigateToDetail(conduce) },
                                    onViewPdf = {
                                        viewModel.generateAndViewPdf(context, conduce)
                                    },
                                    onSharePdf = {
                                        viewModel.generateAndSharePdf(context, conduce)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsHeader(conduces: List<Conduce>) {
    val inTransit = conduces.count { it.status == "En Tránsito" }
    val delivered = conduces.count { it.status == "Entregado" }
    val total = conduces.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = "Total",
            value = total.toString(),
            color = Color(0xFF0F172A),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "En Tránsito",
            value = inTransit.toString(),
            color = Color(0xFF2563EB),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Entregados",
            value = delivered.toString(),
            color = Color(0xFF16A34A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
fun ConduceItemCard(
    conduce: Conduce,
    onClick: () -> Unit,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    val statusBg = when (conduce.status) {
        "Entregado" -> Color(0xFFDCFCE7)
        "En Tránsito" -> Color(0xFFDBEAFE)
        "Anulado" -> Color(0xFFFEE2E2)
        else -> Color(0xFFFEF3C7)
    }
    val statusTextColor = when (conduce.status) {
        "Entregado" -> Color(0xFF15803D)
        "En Tránsito" -> Color(0xFF1D4ED8)
        "Anulado" -> Color(0xFFB91C1C)
        else -> Color(0xFFB45309)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conduce_card_${conduce.conduceNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Folio + Type + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = conduce.conduceNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = conduce.type,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = conduce.status,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ORIGEN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = conduce.originProject.ifBlank { "No especificado" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DESTINO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = conduce.destinationProject.ifBlank { "No especificado" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items summary and Carrier
            val items = conduce.getParsedItems()
            val itemsCountText = "${items.size} ítems (${items.count { it.category == "Material" }} mat., ${items.count { it.category == "Herramienta" }} herr.)"
            Text(
                text = itemsCountText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Normal
                )
            )

            // Date and signatures summary
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = conduce.createdAt?.toDate()?.let { sdf.format(it) } ?: "Reciente"

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action: Ver PDF
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .clickable(onClick = onViewPdf)
                            .testTag("view_pdf_${conduce.conduceNumber}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Ver PDF",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Action: Compartir PDF
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier
                            .clickable(onClick = onSharePdf)
                            .testTag("share_pdf_${conduce.conduceNumber}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConducesView(
    hasSearch: Boolean,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasSearch) "No se encontraron conduces" else "No hay conduces registrados aún",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B)
        )

        Text(
            text = if (hasSearch) "Intenta cambiar el término de búsqueda o filtro" else "Comienza creando el primer conduce de traslado o entrega de materiales.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        if (!hasSearch) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = onCreateClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear Primer Conduce")
            }
        }
    }
}
