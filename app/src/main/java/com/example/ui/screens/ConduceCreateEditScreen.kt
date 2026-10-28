package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conduce
import com.example.data.model.ConduceItem
import com.example.data.model.Project
import com.example.ui.components.SignatureDialog
import com.example.ui.viewmodel.ConduceViewModel
import com.example.util.PdfGenerator
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConduceCreateEditScreen(
    existingConduce: Conduce?,
    viewModel: ConduceViewModel,
    onBack: () -> Unit,
    onConduceSaved: (Conduce) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val projects by viewModel.projectsState.collectAsStateWithLifecycle()

    val initialNumber = remember {
        existingConduce?.conduceNumber ?: "CND-2026-${(1000..9999).random()}"
    }

    var conduceNumber by remember { mutableStateOf(initialNumber) }
    var selectedType by remember { mutableStateOf(existingConduce?.type ?: "Traslado") }
    var status by remember { mutableStateOf(existingConduce?.status ?: "Borrador") }

    var originProject by remember { mutableStateOf(existingConduce?.originProject ?: "") }
    var originResponsible by remember { mutableStateOf(existingConduce?.originResponsible ?: "") }

    var destinationProject by remember { mutableStateOf(existingConduce?.destinationProject ?: "") }
    var destinationResponsible by remember { mutableStateOf(existingConduce?.destinationResponsible ?: "") }

    // Chofer: SOLO pedir el nombre del chofer
    var carrierName by remember { mutableStateOf(existingConduce?.carrierName ?: "") }

    var dispatcherName by remember { mutableStateOf(existingConduce?.dispatcherName ?: "") }
    var dispatcherSignature by remember { mutableStateOf(existingConduce?.dispatcherSignature ?: "") }

    var carrierSignature by remember { mutableStateOf(existingConduce?.carrierSignature ?: "") }

    var receiverName by remember { mutableStateOf(existingConduce?.receiverName ?: "") }
    var receiverSignature by remember { mutableStateOf(existingConduce?.receiverSignature ?: "") }

    var notes by remember { mutableStateOf(existingConduce?.notes ?: "") }

    val itemsList = remember {
        mutableStateListOf<ConduceItem>().apply {
            if (existingConduce != null) {
                addAll(existingConduce.getParsedItems())
            }
        }
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var activeSignatureType by remember { mutableStateOf<String?>(null) } // "dispatcher", "carrier", "receiver"

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onItemAdded = { newItem ->
                itemsList.add(newItem)
                showAddItemDialog = false
            }
        )
    }

    if (activeSignatureType != null) {
        val (title, role) = when (activeSignatureType) {
            "dispatcher" -> "Firma de Despacho" to "Quien entrega los materiales"
            "carrier" -> "Firma del Transportista" to "Chofer responsable de la carga"
            else -> "Firma de Recepción" to "Responsable en obra receptora"
        }
        SignatureDialog(
            title = title,
            signerRole = role,
            onDismiss = { activeSignatureType = null },
            onSignatureConfirmed = { base64 ->
                when (activeSignatureType) {
                    "dispatcher" -> dispatcherSignature = base64
                    "carrier" -> carrierSignature = base64
                    else -> receiverSignature = base64
                }
                activeSignatureType = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingConduce == null) "Nuevo Conduce" else "Editar Conduce",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // General Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Información General",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = conduceNumber,
                        onValueChange = { conduceNumber = it },
                        label = { Text("Número de Folio *", color = Color.Black) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conduce_number_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tipo de Conduce",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.Black
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Traslado", "Entrega", "Devolución").forEach { typeOption ->
                            FilterChip(
                                selected = selectedType == typeOption,
                                onClick = { selectedType = typeOption },
                                label = { Text(typeOption, color = if (selectedType == typeOption) Color.White else Color.Black) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.Black,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Projects Card (Origen & Destino with Autocomplete and Miniatures)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ubicaciones y Responsables",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Text(
                        text = "Selecciona de las obras configuradas o escribe con autocompletar",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Origin Project Autocomplete Field
                    ProjectAutocompleteField(
                        label = "Obra / Proyecto de Origen *",
                        testTag = "origin_project_input",
                        selectedName = originProject,
                        onNameChange = { originProject = it },
                        onResponsibleAutoFill = { if (originResponsible.isBlank()) originResponsible = it },
                        projects = projects
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = originResponsible,
                        onValueChange = { originResponsible = it },
                        label = { Text("Responsable en Origen", color = Color.Black) },
                        placeholder = { Text("Ej. Ing. Carlos Gómez") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Destination Project Autocomplete Field
                    ProjectAutocompleteField(
                        label = "Obra / Proyecto de Destino *",
                        testTag = "destination_project_input",
                        selectedName = destinationProject,
                        onNameChange = { destinationProject = it },
                        onResponsibleAutoFill = { if (destinationResponsible.isBlank()) destinationResponsible = it },
                        projects = projects
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = destinationResponsible,
                        onValueChange = { destinationResponsible = it },
                        label = { Text("Responsable en Destino", color = Color.Black) },
                        placeholder = { Text("Ej. Arq. Laura Martínez") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transport details Card (SOLO PEDIR EL NOMBRE DEL CHOFER)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Datos del Chofer",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = carrierName,
                        onValueChange = { carrierName = it },
                        label = { Text("Nombre del Chofer / Transportista *", color = Color.Black) },
                        placeholder = { Text("Ej. Juan Pérez") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carrier_name_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Materials and Tools List Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Materiales y Herramientas (${itemsList.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )

                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("add_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (itemsList.isEmpty()) {
                        Text(
                            text = "No hay materiales ni herramientas en la lista. Toca 'Agregar' para añadir ítems al conduce.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        itemsList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${item.description}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    )
                                    Text(
                                        text = "${item.quantity} ${item.unit} • ${item.category} • Estado: ${item.condition}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
                                    )
                                }

                                IconButton(onClick = { itemsList.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFDC2626))
                                }
                            }
                            if (index < itemsList.lastIndex) {
                                Divider(color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Digital Signatures Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Firmas Digitales",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Text(
                        text = "Captura digital de firmas directamente en la pantalla",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Dispatcher Signature
                    OutlinedTextField(
                        value = dispatcherName,
                        onValueChange = { dispatcherName = it },
                        label = { Text("Nombre de quien Despacha", color = Color.Black) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SignaturePreviewOrButton(
                        label = "Firma de Despacho",
                        signatureBase64 = dispatcherSignature,
                        onSignClick = { activeSignatureType = "dispatcher" },
                        onClearClick = { dispatcherSignature = "" }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Carrier Signature
                    SignaturePreviewOrButton(
                        label = "Firma del Transportista / Chofer",
                        signatureBase64 = carrierSignature,
                        onSignClick = { activeSignatureType = "carrier" },
                        onClearClick = { carrierSignature = "" }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Receiver Signature
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("Nombre de quien Recibe en Obra", color = Color.Black) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SignaturePreviewOrButton(
                        label = "Firma de Recepción en Obra",
                        signatureBase64 = receiverSignature,
                        onSignClick = { activeSignatureType = "receiver" },
                        onClearClick = { receiverSignature = "" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notes Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Observaciones Generales",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Condiciones de entrega, precintos, aseguramiento de carga, etc.") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Buttons
            Button(
                onClick = {
                    val conduceToSave = (existingConduce ?: Conduce()).copy(
                        conduceNumber = conduceNumber.trim(),
                        type = selectedType,
                        status = status,
                        originProject = originProject.trim(),
                        originResponsible = originResponsible.trim(),
                        destinationProject = destinationProject.trim(),
                        destinationResponsible = destinationResponsible.trim(),
                        carrierName = carrierName.trim(),
                        dispatcherName = dispatcherName.trim(),
                        dispatcherSignature = dispatcherSignature,
                        carrierSignature = carrierSignature,
                        receiverName = receiverName.trim(),
                        receiverSignature = receiverSignature,
                        items = itemsList.map { it.toMap() },
                        notes = notes.trim()
                    )

                    viewModel.saveConduce(conduceToSave) { docId ->
                        onConduceSaved(conduceToSave.copy(id = docId))
                    }
                },
                enabled = !isSaving && originProject.isNotBlank() && destinationProject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_conduce_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar y Sincronizar en la Nube", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectAutocompleteField(
    label: String,
    testTag: String,
    selectedName: String,
    onNameChange: (String) -> Unit,
    onResponsibleAutoFill: (String) -> Unit,
    projects: List<Project>
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredProjects = remember(selectedName, projects) {
        if (selectedName.isBlank()) {
            projects
        } else {
            projects.filter {
                it.name.contains(selectedName, ignoreCase = true) ||
                it.location.contains(selectedName, ignoreCase = true)
            }
        }
    }

    Column {
        // Quick thumbnail chips carousel if projects exist
        if (projects.isNotEmpty()) {
            Text(
                text = "Seleccionar de catálogo:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                items(projects) { project ->
                    val isSelected = selectedName.equals(project.name, ignoreCase = true)
                    val thumbnailBitmap = remember(project.thumbnailBase64) {
                        if (project.thumbnailBase64.isNotBlank()) {
                            PdfGenerator.decodeBase64Bitmap(project.thumbnailBase64)
                        } else null
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color.Black else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color.Black else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.clickable {
                            onNameChange(project.name)
                            if (project.defaultResponsible.isNotBlank()) {
                                onResponsibleAutoFill(project.defaultResponsible)
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbnailBitmap != null) {
                                    Image(
                                        bitmap = thumbnailBitmap.asImageBitmap(),
                                        contentDescription = project.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.LocationCity,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Searchable Autocomplete Field with Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded && filteredProjects.isNotEmpty(),
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {
                    onNameChange(it)
                    expanded = true
                },
                label = { Text(label, color = Color.Black) },
                placeholder = { Text("Escribe o selecciona...", color = Color(0xFF64748B)) },
                trailingIcon = {
                    if (projects.isNotEmpty()) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color(0xFF64748B)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag(testTag)
            )

            ExposedDropdownMenu(
                expanded = expanded && filteredProjects.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                filteredProjects.forEach { project ->
                    val thumbnailBitmap = remember(project.thumbnailBase64) {
                        if (project.thumbnailBase64.isNotBlank()) {
                            PdfGenerator.decodeBase64Bitmap(project.thumbnailBase64)
                        } else null
                    }

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbnailBitmap != null) {
                                        Image(
                                            bitmap = thumbnailBitmap.asImageBitmap(),
                                            contentDescription = project.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.LocationCity,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    )
                                    val subtitle = listOfNotNull(
                                        project.location.takeIf { it.isNotBlank() },
                                        project.defaultResponsible.takeIf { it.isNotBlank() }?.let { "Resp: $it" }
                                    ).joinToString(" • ")
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.Black,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            onNameChange(project.name)
                            if (project.defaultResponsible.isNotBlank()) {
                                onResponsibleAutoFill(project.defaultResponsible)
                            }
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SignaturePreviewOrButton(
    label: String,
    signatureBase64: String,
    onSignClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val bitmap = remember(signatureBase64) {
        if (signatureBase64.isNotBlank()) PdfGenerator.decodeBase64Bitmap(signatureBase64) else null
    }

    if (bitmap != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = onClearClick) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar firma", tint = Color.Black)
            }
        }
    } else {
        OutlinedButton(
            onClick = onSignClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Registrar $label", color = Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onItemAdded: (ConduceItem) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Material") }
    var quantityText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Uds") }
    var serialOrCode by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Bueno") }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Añadir Material o Herramienta",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del Ítem *", color = Color.Black) },
                    placeholder = { Text("Ej. Cemento Gris, Taladro Percutor 20V") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_description_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría", color = Color.Black) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("Material", "Herramienta", "Equipo", "Otro").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.Black) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Cantidad *", color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_quantity_input")
                    )

                    // Unit dropdown
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad", color = Color.Black) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            listOf("Uds", "Sacos", "m³", "kg", "Piezas", "Cajas", "Metros", "Galones").forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u, color = Color.Black) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = serialOrCode,
                        onValueChange = { serialOrCode = it },
                        label = { Text("Serie / Referencia", color = Color.Black) },
                        placeholder = { Text("SN-9843") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    // Condition dropdown
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = condition,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado", color = Color.Black) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            listOf("Nuevo", "Bueno", "Usado", "Regular").forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond, color = Color.Black) },
                                    onClick = {
                                        condition = cond
                                        conditionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (description.isNotBlank()) {
                                val qty = quantityText.toDoubleOrNull() ?: 1.0
                                onItemAdded(
                                    ConduceItem(
                                        id = UUID.randomUUID().toString(),
                                        description = description.trim(),
                                        category = category,
                                        quantity = qty,
                                        unit = unit,
                                        serialOrCode = serialOrCode.trim(),
                                        condition = condition,
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        enabled = description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_item_button")
                    ) {
                        Text("Añadir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
