package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.util.PdfGenerator

data class PathPoint(val offset: Offset, val isStart: Boolean)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureCaptured: (String) -> Unit, // returns Base64 string
    onClear: () -> Unit = {}
) {
    val points = remember { mutableStateListOf<PathPoint>() }
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            points.add(PathPoint(offset, isStart = true))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            points.add(PathPoint(change.position, isStart = false))
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()

                // Baseline guideline for signing
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(20f, size.height * 0.75f),
                    end = Offset(size.width - 20f, size.height * 0.75f),
                    strokeWidth = 1.5f
                )

                if (points.isEmpty()) {
                    return@Canvas
                }

                for (i in 1 until points.size) {
                    val p1 = points[i - 1]
                    val p2 = points[i]
                    if (!p2.isStart) {
                        drawLine(
                            color = Color(0xFF0F172A),
                            start = p1.offset,
                            end = p2.offset,
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            if (points.isEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Firme aquí con el dedo",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    points.clear()
                    onClear()
                },
                modifier = Modifier.testTag("clear_signature_button")
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpiar")
            }

            Button(
                onClick = {
                    if (points.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(AndroidColor.WHITE)

                        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.rgb(15, 23, 42)
                            strokeWidth = 4f
                            style = AndroidPaint.Style.STROKE
                            strokeCap = AndroidPaint.Cap.ROUND
                            strokeJoin = AndroidPaint.Join.ROUND
                        }

                        for (i in 1 until points.size) {
                            val p1 = points[i - 1]
                            val p2 = points[i]
                            if (!p2.isStart) {
                                canvas.drawLine(p1.offset.x, p1.offset.y, p2.offset.x, p2.offset.y, paint)
                            }
                        }

                        val base64 = PdfGenerator.bitmapToBase64(bitmap)
                        onSignatureCaptured(base64)
                    }
                },
                enabled = points.isNotEmpty(),
                modifier = Modifier.testTag("save_signature_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirmar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirmar Firma")
            }
        }
    }
}

@Composable
fun SignatureDialog(
    title: String,
    signerRole: String,
    onDismiss: () -> Unit,
    onSignatureConfirmed: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = signerRole,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                SignaturePad(
                    onSignatureCaptured = { base64 ->
                        onSignatureConfirmed(base64)
                        onDismiss()
                    }
                )
            }
        }
    }
}
