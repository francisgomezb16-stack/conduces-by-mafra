package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conduce
import com.example.ui.components.SignatureDialog
import com.example.ui.viewmodel.ConduceViewModel
import com.example.util.PdfGenerator
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConduceDetailScreen(
    conduce: Conduce,
    viewModel: ConduceViewModel,
    onBack: () -> Unit,
    onNavigateToEdit: (Conduce) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var currentConduce by remember { mutableStateOf(conduce) }
    var activeSignatureType by remember { mutableStateOf<String?>(null) } // "dispatcher", "carrier", "receiver"
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val evidenceBitmaps = remember { mutableStateListOf<Bitmap>() }

    // Zero-permission Android Photo Picker for attaching evidence
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    evidenceBitmaps.add(bitmap)
                    val updated = currentConduce.copy(evidenceCount = evidenceBitmaps.size.toLong())
                    currentConduce = updated
                    viewModel.saveConduce(updated)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    if (activeSignatureType != null) {
        val (title, role) = when (activeSignatureType) {
            "dispatcher" -> "Firma de Despacho" to "Quien entrega / Despacha los materiales"
            "carrier" -> "Firma del Transportista" to "Chofer o transportista a cargo"
            else -> "Firma de Recepción" to "Responsable que recibe en obra"
        }
        SignatureDialog(
            title = title,
            signerRole = role,
            onDismiss = { activeSignatureType = null },
            onSignatureConfirmed = { base64Signature ->
                val updated = when (activeSignatureType) {
                    "dispatcher" -> currentConduce.copy(dispatcherSignature = base64Signature)
                    "carrier" -> currentConduce.copy(carrierSignature = base64Signature)
                    else -> currentConduce.copy(
                        receiverSignature = base64Signature,
                        status = if (currentConduce.status == "En Tránsito") "Entregado" else currentConduce.status
                    )
                }
                currentConduce = updated
                viewModel.saveConduce(updated)
                activeSignatureType = null
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Conduce") },
            text = { Text("¿Estás seguro de que deseas eliminar el conduce ${currentConduce.conduceNumber}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConduce(currentConduce.id, onDone = onBack)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentConduce.conduceNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Detalle del Conduce",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
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
                actions = {
                    IconButton(
                        onClick = { viewModel.generateAndViewPdf(context, currentConduce, evidenceBitmaps) },
                        modifier = Modifier.testTag("action_view_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Ver PDF",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.generateAndSharePdf(context, currentConduce, evidenceBitmaps) },
                        modifier = Modifier.testTag("action_share_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir PDF",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToEdit(currentConduce) },
                        modifier = Modifier.testTag("action_edit_conduce")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Conduce",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("action_delete_conduce")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFEF4444)
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
            // Status & Type Header Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FOLIO OFICIAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = currentConduce.conduceNumber,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            )
                        }

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (currentConduce.status) {
                                    "Entregado" -> Color(0xFF16A34A)
                                    "En Tránsito" -> Color(0xFF2563EB)
                                    "Anulado" -> Color(0xFFDC2626)
                                    else -> Color(0xFFD97706)
                                },
                                modifier = Modifier
                                    .clickable { showStatusMenu = true }
                                    .testTag("change_status_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = currentConduce.status.uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false }
                            ) {
                                listOf("Borrador", "En Tránsito", "Entregado", "Anulado").forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = {
                                            viewModel.updateStatus(currentConduce.id, status)
                                            currentConduce = currentConduce.copy(status = status)
                                            showStatusMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val dateFormatted = currentConduce.createdAt?.toDate()?.let { sdf.format(it) } ?: "Fecha de emisión reciente"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tipo: ${currentConduce.type}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary PDF Action Bar
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Comprobante Oficial PDF con Logo",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Incluye membrete institucional, firmas digitales y fotos de evidencias adjuntas.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateAndViewPdf(context, currentConduce, evidenceBitmaps) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_view_pdf_official")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver PDF")
                        }

                        Button(
                            onClick = { viewModel.generateAndSharePdf(context, currentConduce, evidenceBitmaps) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_share_pdf_official")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir", color = Color(0xFF0F172A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Route Card: Origin & Destination
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Ruta y Ubicaciones de Proyecto",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ORIGEN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = currentConduce.originProject.ifBlank { "No especificado" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (currentConduce.originResponsible.isNotBlank()) {
                                Text(
                                    text = "Resp: ${currentConduce.originResponsible}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DESTINO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = currentConduce.destinationProject.ifBlank { "No especificado" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (currentConduce.destinationResponsible.isNotBlank()) {
                                Text(
                                    text = "Resp: ${currentConduce.destinationResponsible}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transport details
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transporte y Conductor",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val carrier = currentConduce.carrierName.ifBlank { "Chofer propio" }
                    val idDoc = if (currentConduce.carrierIdNumber.isNotBlank()) " • Cédula/ID: ${currentConduce.carrierIdNumber}" else ""
                    Text(
                        text = "Chofer: $carrier$idDoc",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val vehInfo = listOfNotNull(
                        currentConduce.vehicleDescription.takeIf { it.isNotBlank() },
                        currentConduce.vehiclePlate.takeIf { it.isNotBlank() }?.let { "Placa: $it" },
                        currentConduce.carrierPhone.takeIf { it.isNotBlank() }?.let { "Tel: $it" }
                    ).joinToString(" • ")

                    if (vehInfo.isNotBlank()) {
                        Text(
                            text = vehInfo,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Items Table Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val parsedItems = currentConduce.getParsedItems()
                    Text(
                        text = "Materiales y Herramientas (${parsedItems.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (parsedItems.isEmpty()) {
                        Text(
                            text = "No se han agregado ítems a este conduce.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    } else {
                        parsedItems.forEachIndexed { index, item ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${item.description}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Row(modifier = Modifier.padding(top = 2.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (item.category == "Material") Color(0xFFE0F2FE) else Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (item.category == "Material") Color(0xFF0369A1) else Color(0xFFB45309),
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (item.serialOrCode.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Cód: ${item.serialOrCode}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color(0xFF64748B),
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.quantity} ${item.unit}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        )
                                        Text(
                                            text = item.condition,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF16A34A),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                if (index < parsedItems.lastIndex) {
                                    Divider(color = Color(0xFFE2E8F0))
                                }
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Firmas Digitales de Control",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Toca cada recuadro para registrar o actualizar la firma digital",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SignatureSlot(
                            title = "Despachado",
                            name = currentConduce.dispatcherName.ifBlank { "Despacho" },
                            signatureBase64 = currentConduce.dispatcherSignature,
                            onClick = { activeSignatureType = "dispatcher" },
                            modifier = Modifier.weight(1f)
                        )
                        SignatureSlot(
                            title = "Transportista",
                            name = currentConduce.carrierName.ifBlank { "Chofer" },
                            signatureBase64 = currentConduce.carrierSignature,
                            onClick = { activeSignatureType = "carrier" },
                            modifier = Modifier.weight(1f)
                        )
                        SignatureSlot(
                            title = "Recepción",
                            name = currentConduce.receiverName.ifBlank { "Receptor" },
                            signatureBase64 = currentConduce.receiverSignature,
                            onClick = { activeSignatureType = "receiver" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Evidences Gallery Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evidencias Fotográficas (${evidenceBitmaps.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("add_evidence_photo_button")
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adjuntar Foto")
                        }
                    }

                    if (evidenceBitmaps.isEmpty()) {
                        Text(
                            text = "No hay fotografías adjuntas aún. Adjunta fotos de la carga para el PDF.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(evidenceBitmaps) { bitmap ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Evidencia",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Observations
            if (currentConduce.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Observaciones",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentConduce.notes,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignatureSlot(
    title: String,
    name: String,
    signatureBase64: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSigned = signatureBase64.isNotBlank()
    val bitmap = remember(signatureBase64) {
        if (isSigned) PdfGenerator.decodeBase64Bitmap(signatureBase64) else null
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSigned) Color(0xFFF8FAFC) else Color(0xFFFFFBEB),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSigned) Color(0xFFCBD5E1) else Color(0xFFFCD34D)
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .height(105.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    fontSize = 10.sp
                )
            )

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Firma",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Firmar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}
