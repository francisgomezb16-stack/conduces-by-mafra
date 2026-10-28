package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateConducePdf(
        context: Context,
        conduce: Conduce,
        company: CompanyProfile,
        evidenceBitmaps: List<Bitmap> = emptyList()
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawPageOne(context, canvas, conduce, company)
        pdfDocument.finishPage(page)

        // If there are photo evidences, draw page 2 for evidence gallery
        if (evidenceBitmaps.isNotEmpty()) {
            val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            drawEvidencePage(context, page2.canvas, conduce, evidenceBitmaps)
            pdfDocument.finishPage(page2)
        }

        // Save PDF to cache/pdfs directory
        val outputDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val sanitizeNumber = conduce.conduceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(outputDir, "Conduce_${sanitizeNumber}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawPageOne(
        context: Context,
        canvas: Canvas,
        conduce: Conduce,
        company: CompanyProfile
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var currentY = MARGIN

        // Background card accent header
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Company Logo
        val logoBitmap = loadCompanyLogo(context, company)
        if (logoBitmap != null) {
            val destRect = RectF(MARGIN, 15f, MARGIN + 65f, 80f)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
        }

        // Company Information (White text on dark header)
        val textStartX = if (logoBitmap != null) MARGIN + 75f else MARGIN
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText(company.companyName.ifBlank { "EMPRESA CONSTRUCTORA & LOGÍSTICA" }, textStartX, 35f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        val taxText = if (company.taxId.isNotBlank()) "RNC / ID Fiscal: ${company.taxId}" else "Control de Materiales y Equipos"
        val contactText = listOfNotNull(
            company.phone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            company.email.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        canvas.drawText(taxText, textStartX, 50f, paint)
        canvas.drawText(contactText.ifBlank { "Logística y Traslados entre Proyectos" }, textStartX, 63f, paint)
        if (company.address.isNotBlank()) {
            canvas.drawText(company.address.take(55), textStartX, 76f, paint)
        }

        // Right side badge of header: Folio & Date
        paint.color = Color.rgb(245, 158, 11) // Amber 500
        val folioBoxRect = RectF(PAGE_WIDTH - MARGIN - 130f, 15f, PAGE_WIDTH - MARGIN, 42f)
        canvas.drawRoundRect(folioBoxRect, 6f, 6f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("FOLIO: ${conduce.conduceNumber}", folioBoxRect.centerX(), 32f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = conduce.createdAt?.toDate()?.let { sdf.format(it) } ?: sdf.format(Date())
        canvas.drawText(dateStr, folioBoxRect.centerX(), 56f, paint)

        // Status badge
        paint.color = when (conduce.status) {
            "Entregado" -> Color.rgb(34, 197, 94) // Green
            "En Tránsito" -> Color.rgb(59, 130, 246) // Blue
            "Anulado" -> Color.rgb(239, 68, 68) // Red
            else -> Color.rgb(234, 179, 8) // Yellow
        }
        val statusRect = RectF(PAGE_WIDTH - MARGIN - 110f, 64f, PAGE_WIDTH - MARGIN, 80f)
        canvas.drawRoundRect(statusRect, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText(conduce.status.uppercase(Locale.getDefault()), statusRect.centerX(), 75f, paint)

        // Reset text alignment
        paint.textAlign = Paint.Align.LEFT
        currentY = 112f

        // Document Subtitle Bar
        paint.color = Color.rgb(241, 245, 249) // Slate 100
        canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f), 4f, 4f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("COMPROBANTE OFICIAL DE CONDUCE DE TRASLADO / ENTREGA  •  TIPO: ${conduce.type.uppercase(Locale.getDefault())}", MARGIN + 10f, currentY + 15f, paint)

        currentY += 30f

        // Two Cards: Origen and Destino
        val halfWidth = (PAGE_WIDTH - (MARGIN * 2) - 10f) / 2f
        drawProjectCard(
            canvas, paint,
            title = "1. ORIGEN (PUNTO DE SALIDA)",
            project = conduce.originProject,
            responsible = conduce.originResponsible,
            left = MARGIN, top = currentY, width = halfWidth, height = 52f,
            accentColor = Color.rgb(2, 132, 199) // Light blue
        )

        drawProjectCard(
            canvas, paint,
            title = "2. DESTINO (OBRA / PROYECTO)",
            project = conduce.destinationProject,
            responsible = conduce.destinationResponsible,
            left = MARGIN + halfWidth + 10f, top = currentY, width = halfWidth, height = 52f,
            accentColor = Color.rgb(16, 185, 129) // Emerald green
        )

        currentY += 60f

        // Transport & Driver Card
        drawCarrierCard(
            canvas, paint,
            conduce = conduce,
            left = MARGIN, top = currentY, width = PAGE_WIDTH - (MARGIN * 2), height = 48f
        )

        currentY += 56f

        // Items Section Table Header
        paint.color = Color.rgb(30, 41, 59) // Slate 800
        val tableTop = currentY
        canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 18f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f

        val colX = floatArrayOf(
            MARGIN + 6f,        // #
            MARGIN + 28f,       // Descripción
            MARGIN + 215f,      // Categoría
            MARGIN + 280f,      // Cantidad
            MARGIN + 335f,      // Unidad
            MARGIN + 385f,      // Código/Serie
            MARGIN + 465f       // Estado
        )

        canvas.drawText("#", colX[0], tableTop + 12f, paint)
        canvas.drawText("DESCRIPCIÓN DE MATERIAL / HERRAMIENTA", colX[1], tableTop + 12f, paint)
        canvas.drawText("CATEGORÍA", colX[2], tableTop + 12f, paint)
        canvas.drawText("CANT.", colX[3], tableTop + 12f, paint)
        canvas.drawText("UNIDAD", colX[4], tableTop + 12f, paint)
        canvas.drawText("SERIE / CÓD.", colX[5], tableTop + 12f, paint)
        canvas.drawText("CONDICIÓN", colX[6], tableTop + 12f, paint)

        currentY = tableTop + 18f

        // Items rows (up to 8 items on page 1)
        val parsedItems = conduce.getParsedItems()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f

        if (parsedItems.isEmpty()) {
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)
            paint.color = Color.GRAY
            canvas.drawText("Sin ítems registrados en este conduce.", MARGIN + 10f, currentY + 15f, paint)
            currentY += 22f
        } else {
            for ((index, item) in parsedItems.take(8).withIndex()) {
                val rowHeight = 18f
                // Zebra stripe
                paint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                // Bottom border
                paint.color = Color.rgb(226, 232, 240)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, currentY + rowHeight, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText("${index + 1}", colX[0], currentY + 12f, paint)
                val desc = if (item.description.length > 36) item.description.take(34) + ".." else item.description
                canvas.drawText(desc, colX[1], currentY + 12f, paint)
                canvas.drawText(item.category, colX[2], currentY + 12f, paint)
                canvas.drawText("${item.quantity}", colX[3], currentY + 12f, paint)
                canvas.drawText(item.unit, colX[4], currentY + 12f, paint)
                val code = item.serialOrCode.ifBlank { "-" }.let { if (it.length > 14) it.take(12) + ".." else it }
                canvas.drawText(code, colX[5], currentY + 12f, paint)
                canvas.drawText(item.condition, colX[6], currentY + 12f, paint)

                currentY += rowHeight
            }
        }

        currentY += 8f

        // Observaciones Generales Box
        paint.color = Color.rgb(248, 250, 252)
        val obsRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 36f)
        canvas.drawRoundRect(obsRect, 4f, 4f, paint)
        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(obsRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText("OBSERVACIONES / CONDICIONES DE TRASLADO:", MARGIN + 8f, currentY + 12f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val noteContent = conduce.notes.ifBlank {
            "Materiales y herramientas entregados para uso exclusivo en el proyecto indicado. El receptor verifica cantidades y buen estado."
        }
        val noteLine1 = if (noteContent.length > 95) noteContent.take(95) else noteContent
        val noteLine2 = if (noteContent.length > 95) noteContent.substring(95).take(95) else ""
        canvas.drawText(noteLine1, MARGIN + 8f, currentY + 22f, paint)
        if (noteLine2.isNotBlank()) {
            canvas.drawText(noteLine2, MARGIN + 8f, currentY + 31f, paint)
        }

        currentY += 46f

        // Signatures Section (3 signature columns)
        val sigBoxWidth = (PAGE_WIDTH - (MARGIN * 2) - 20f) / 3f
        val sigBoxHeight = 88f

        drawSignatureBox(
            canvas, paint,
            title = "DESPACHADO / ENTREGADO",
            name = conduce.dispatcherName.ifBlank { "Encargado Despacho" },
            signatureBase64 = conduce.dispatcherSignature,
            left = MARGIN, top = currentY, width = sigBoxWidth, height = sigBoxHeight
        )

        drawSignatureBox(
            canvas, paint,
            title = "TRANSPORTISTA / CHOFER",
            name = conduce.carrierName.ifBlank { "Transporte Asignado" },
            signatureBase64 = conduce.carrierSignature,
            left = MARGIN + sigBoxWidth + 10f, top = currentY, width = sigBoxWidth, height = sigBoxHeight
        )

        drawSignatureBox(
            canvas, paint,
            title = "RECIBIDO EN OBRA",
            name = conduce.receiverName.ifBlank { "Responsable en Obra" },
            signatureBase64 = conduce.receiverSignature,
            left = MARGIN + (sigBoxWidth * 2) + 20f, top = currentY, width = sigBoxWidth, height = sigBoxHeight
        )

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Este documento constituye un comprobante formal de traslado y custodia de materiales y herramientas. Conduces de Obra Pro • Pág. 1",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 16f,
            paint
        )
    }

    private fun drawProjectCard(
        canvas: Canvas,
        paint: Paint,
        title: String,
        project: String,
        responsible: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        accentColor: Int
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        // Border
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Left accent indicator strip
        paint.color = accentColor
        canvas.drawRoundRect(RectF(left, top, left + 4f, top + height), 2f, 2f, paint)

        // Title
        paint.color = accentColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText(title, left + 10f, top + 13f, paint)

        // Project Name
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9f
        canvas.drawText(project.ifBlank { "No especificado" }.take(32), left + 10f, top + 27f, paint)

        // Responsible
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val respText = if (responsible.isNotBlank()) "Resp: $responsible" else "Resp: Encargado de obra"
        canvas.drawText(respText.take(34), left + 10f, top + 41f, paint)
    }

    private fun drawCarrierCard(
        canvas: Canvas,
        paint: Paint,
        conduce: Conduce,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Left accent strip
        paint.color = Color.rgb(245, 158, 11)
        canvas.drawRoundRect(RectF(left, top, left + 4f, top + height), 2f, 2f, paint)

        // Section Title
        paint.color = Color.rgb(217, 119, 6)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText("3. DATOS DEL TRANSPORTE Y CONDUCTOR", left + 10f, top + 13f, paint)

        // Row 1: Conductor + ID
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 8.5f
        val driverName = conduce.carrierName.ifBlank { "Transportista propio / Asignado" }
        val idText = if (conduce.carrierIdNumber.isNotBlank()) " (ID/Lic: ${conduce.carrierIdNumber})" else ""
        canvas.drawText("Chofer: $driverName$idText", left + 10f, top + 27f, paint)

        // Row 2: Vehiculo + Placa + Tel
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val vehicleInfo = listOfNotNull(
            conduce.vehicleDescription.takeIf { it.isNotBlank() },
            conduce.vehiclePlate.takeIf { it.isNotBlank() }?.let { "Placa: $it" },
            conduce.carrierPhone.takeIf { it.isNotBlank() }?.let { "Tel: $it" }
        ).joinToString("  •  ")
        canvas.drawText(vehicleInfo.ifBlank { "Vehículo institucional de la obra" }, left + 10f, top + 40f, paint)
    }

    private fun drawSignatureBox(
        canvas: Canvas,
        paint: Paint,
        title: String,
        name: String,
        signatureBase64: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.WHITE
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Header inside signature box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(left + 1f, top + 1f, left + width - 1f, top + 16f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, left + (width / 2f), top + 11f, paint)

        // Draw signature bitmap if present
        if (signatureBase64.isNotBlank()) {
            val sigBitmap = decodeBase64Bitmap(signatureBase64)
            if (sigBitmap != null) {
                val sigRect = RectF(left + 10f, top + 20f, left + width - 10f, top + 60f)
                canvas.drawBitmap(sigBitmap, null, sigRect, paint)
            }
        } else {
            // Draw placeholder signature line
            paint.color = Color.rgb(203, 213, 225)
            paint.strokeWidth = 0.8f
            canvas.drawLine(left + 12f, top + 56f, left + width - 12f, top + 56f, paint)
        }

        // Print signer name
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText(name.take(24), left + (width / 2f), top + 72f, paint)

        // Verification subtitle
        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 5.5f
        canvas.drawText("Firma y Conformidad", left + (width / 2f), top + 81f, paint)

        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawEvidencePage(
        context: Context,
        canvas: Canvas,
        conduce: Conduce,
        evidenceBitmaps: List<Bitmap>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header for page 2
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 55f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ANEXO DE EVIDENCIAS FOTOGRÁFICAS", MARGIN, 32f, paint)

        paint.color = Color.rgb(245, 158, 11)
        paint.textSize = 9f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FOLIO: ${conduce.conduceNumber}", PAGE_WIDTH - MARGIN, 32f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Subtitle bar
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, 65f, PAGE_WIDTH - MARGIN, 85f), 4f, 4f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Fotografías de respaldo capturadas durante la carga, transporte o recepción en obra:", MARGIN + 10f, 78f, paint)

        // 2x2 grid of photos (up to 4 images on page 2)
        val photoWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 2f
        val photoHeight = 220f
        var startY = 100f

        for ((index, bitmap) in evidenceBitmaps.take(4).withIndex()) {
            val col = index % 2
            val row = index / 2
            val left = MARGIN + (col * (photoWidth + 16f))
            val top = startY + (row * (photoHeight + 40f))

            // Photo frame
            paint.color = Color.rgb(241, 245, 249)
            val frameRect = RectF(left, top, left + photoWidth, top + photoHeight)
            canvas.drawRoundRect(frameRect, 6f, 6f, paint)

            paint.color = Color.rgb(203, 213, 225)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(frameRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Draw image inside frame
            val innerRect = RectF(left + 4f, top + 4f, left + photoWidth - 4f, top + photoHeight - 4f)
            canvas.drawBitmap(bitmap, null, innerRect, paint)

            // Caption under photo
            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            canvas.drawText("Evidencia Fotográfica #${index + 1}", left + 4f, top + photoHeight + 14f, paint)

            paint.color = Color.rgb(100, 116, 139)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 6.5f
            canvas.drawText("Verificación visual de estado y entrega de carga", left + 4f, top + photoHeight + 24f, paint)
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Anexo de Evidencias • Conduces de Obra Pro • Pág. 2",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 16f,
            paint
        )
    }

    private fun loadCompanyLogo(context: Context, company: CompanyProfile): Bitmap? {
        if (company.logoBase64.isNotBlank()) {
            val custom = decodeBase64Bitmap(company.logoBase64)
            if (custom != null) return custom
        }
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_company_logo)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64Bitmap(base64Str: String): Bitmap? {
        return try {
            val clean = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
            val decodedBytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir Conduce PDF"))
    }

    fun viewPdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(viewIntent, "Abrir Conduce PDF"))
    }
}
