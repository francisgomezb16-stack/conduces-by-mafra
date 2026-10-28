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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
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

    var carrierName by remember { mutableStateOf(existingConduce?.carrierName ?: "") }
    var carrierIdNumber by remember { mutableStateOf(existingConduce?.carrierIdNumber ?: "") }
    var carrierPhone by remember { mutableStateOf(existingConduce?.carrierPhone ?: "") }
    var vehiclePlate by remember { mutableStateOf(existingConduce?.vehiclePlate ?: "") }
    var vehicleDescription by remember { mutableStateOf(existingConduce?.vehicleDescription ?: "") }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
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
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = conduceNumber,
                        onValueChange = { conduceNumber = it },
                        label = { Text("Número de Folio *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conduce_number_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tipo de Conduce",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF475569)
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
                                label = { Text(typeOption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F172A),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Projects Card (Origen & Destino)
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
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = originProject,
                        onValueChange = { originProject = it },
                        label = { Text("Obra / Proyecto de Origen *") },
                        placeholder = { Text("Ej. Torre Bella Vista, Almacén Central") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("origin_project_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = originResponsible,
                        onValueChange = { originResponsible = it },
                        label = { Text("Responsable en Origen") },
                        placeholder = { Text("Ej. Ing. Carlos Gómez") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = destinationProject,
                        onValueChange = { destinationProject = it },
                        label = { Text("Obra / Proyecto de Destino *") },
                        placeholder = { Text("Ej. Residencial Las Palmas, Obra B") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("destination_project_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = destinationResponsible,
                        onValueChange = { destinationResponsible = it },
                        label = { Text("Responsable en Destino") },
                        placeholder = { Text("Ej. Arq. Laura Martínez") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transport details Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Datos de Transporte",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = carrierName,
                        onValueChange = { carrierName = it },
                        label = { Text("Nombre del Chofer / Transportista") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carrier_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = carrierIdNumber,
                            onValueChange = { carrierIdNumber = it },
                            label = { Text("Cédula / Licencia") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carrierPhone,
                            onValueChange = { carrierPhone = it },
                            label = { Text("Teléfono Chofer") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vehicleDescription,
                            onValueChange = { vehicleDescription = it },
                            label = { Text("Vehículo (Marca/Modelo)") },
                            placeholder = { Text("Camión Daihatsu") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = vehiclePlate,
                            onValueChange = { vehiclePlate = it },
                            label = { Text("Placa") },
                            placeholder = { Text("L-123456") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
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
                            color = Color(0xFF0F172A)
                        )

                        Button(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.testTag("add_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar", color = Color(0xFF0F172A))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (itemsList.isEmpty()) {
                        Text(
                            text = "No hay materiales ni herramientas en la lista. Toca 'Agregar' para añadir ítems al conduce.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
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
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${item.quantity} ${item.unit} • ${item.category} • Estado: ${item.condition}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                    )
                                }

                                IconButton(onClick = { itemsList.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
                                }
                            }
                            if (index < itemsList.lastIndex) {
                                Divider(color = Color(0xFFF1F5F9))
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
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Captura digital de firmas directamente en la pantalla",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Dispatcher Signature
                    OutlinedTextField(
                        value = dispatcherName,
                        onValueChange = { dispatcherName = it },
                        label = { Text("Nombre de quien Despacha") },
                        singleLine = true,
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
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Carrier Signature
                    SignaturePreviewOrButton(
                        label = "Firma del Transportista / Chofer",
                        signatureBase64 = carrierSignature,
                        onSignClick = { activeSignatureType = "carrier" },
                        onClearClick = { carrierSignature = "" }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Receiver Signature
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("Nombre de quien Recibe en Obra") },
                        singleLine = true,
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
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Condiciones de entrega, precintos, aseguramiento de carga, etc.") },
                        maxLines = 3,
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
                        carrierIdNumber = carrierIdNumber.trim(),
                        carrierPhone = carrierPhone.trim(),
                        vehiclePlate = vehiclePlate.trim(),
                        vehicleDescription = vehicleDescription.trim(),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_conduce_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar y Sincronizar en la Nube", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
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
                Icon(Icons.Default.Close, contentDescription = "Eliminar firma", tint = Color(0xFF64748B))
            }
        }
    } else {
        OutlinedButton(
            onClick = onSignClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Registrar $label")
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
            color = MaterialTheme.colorScheme.surface,
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del Ítem *") },
                    placeholder = { Text("Ej. Cemento Gris, Taladro Percutor 20V") },
                    singleLine = true,
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
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        listOf("Material", "Herramienta", "Equipo", "Otro").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
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
                        label = { Text("Cantidad *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
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
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            listOf("Uds", "Sacos", "m³", "kg", "Piezas", "Cajas", "Metros", "Galones").forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
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
                        label = { Text("Serie / Referencia") },
                        placeholder = { Text("SN-9843") },
                        singleLine = true,
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
                            label = { Text("Estado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false }
                        ) {
                            listOf("Nuevo", "Bueno", "Usado", "Regular").forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond) },
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_item_button")
                    ) {
                        Text("Añadir")
                    }
                }
            }
        }
    }
}
