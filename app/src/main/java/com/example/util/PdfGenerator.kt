package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

        // Clean white background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Top decorative border line
        paint.color = Color.BLACK
        paint.strokeWidth = 3f
        canvas.drawLine(MARGIN, 18f, PAGE_WIDTH - MARGIN, 18f, paint)

        // Load company logo (custom base64 if provided, else default Pedralbes logo)
        val logoBitmap = loadCompanyLogo(context, company)
        val textStartX = if (logoBitmap != null) {
            val maxW = 100f
            val maxH = 65f
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat().coerceAtLeast(1f)
            val (w, h) = if (aspect > maxW / maxH) {
                maxW to (maxW / aspect)
            } else {
                (maxH * aspect) to maxH
            }
            val topY = 26f + ((maxH - h) / 2f)
            val destRect = RectF(MARGIN, topY, MARGIN + w, topY + h)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
            MARGIN + w + 14f
        } else {
            MARGIN
        }

        // Company Information (strictly in BLACK text)
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        val displayName = company.companyName.ifBlank { "Pedralbes" }
        canvas.drawText(displayName, textStartX, 40f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        val taxText = if (company.taxId.isNotBlank()) "RNC / ID Fiscal: ${company.taxId}" else "Control de Materiales y Equipos"
        canvas.drawText(taxText, textStartX, 54f, paint)

        val contactText = listOfNotNull(
            company.phone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            company.email.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        canvas.drawText(contactText.ifBlank { "Logística y Traslados entre Proyectos" }, textStartX, 67f, paint)

        if (company.address.isNotBlank()) {
            canvas.drawText(company.address.take(52), textStartX, 80f, paint)
        }

        // Right side badge of header: Folio & Date in crisp black
        val folioBoxRect = RectF(PAGE_WIDTH - MARGIN - 135f, 24f, PAGE_WIDTH - MARGIN, 50f)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(folioBoxRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("FOLIO: ${conduce.conduceNumber}", folioBoxRect.centerX(), 41f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = conduce.createdAt?.toDate()?.let { sdf.format(it) } ?: sdf.format(Date())
        canvas.drawText(dateStr, folioBoxRect.centerX(), 64f, paint)

        // Status badge with black text and border
        val statusRect = RectF(PAGE_WIDTH - MARGIN - 110f, 70f, PAGE_WIDTH - MARGIN, 88f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawRoundRect(statusRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("ESTADO: ${conduce.status.uppercase(Locale.getDefault())}", statusRect.centerX(), 82f, paint)

        // Reset alignment
        paint.textAlign = Paint.Align.LEFT
        var currentY = 102f

        // Document Title Bar
        paint.color = Color.rgb(241, 245, 249)
        val titleBarRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f)
        canvas.drawRoundRect(titleBarRect, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRoundRect(titleBarRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("COMPROBANTE OFICIAL DE CONDUCE DE TRASLADO / ENTREGA  •  TIPO: ${conduce.type.uppercase(Locale.getDefault())}", MARGIN + 10f, currentY + 15f, paint)

        currentY += 30f

        // Two Cards: Origen and Destino
        val halfWidth = (PAGE_WIDTH - (MARGIN * 2) - 10f) / 2f
        drawProjectCard(
            canvas, paint,
            title = "1. ORIGEN (PUNTO DE SALIDA)",
            project = conduce.originProject,
            responsible = conduce.originResponsible,
            left = MARGIN, top = currentY, width = halfWidth, height = 54f
        )

        drawProjectCard(
            canvas, paint,
            title = "2. DESTINO (OBRA / PROYECTO)",
            project = conduce.destinationProject,
            responsible = conduce.destinationResponsible,
            left = MARGIN + halfWidth + 10f, top = currentY, width = halfWidth, height = 54f
        )

        currentY += 62f

        // Transport & Driver Card
        drawCarrierCard(
            canvas, paint,
            conduce = conduce,
            left = MARGIN, top = currentY, width = PAGE_WIDTH - (MARGIN * 2), height = 48f
        )

        currentY += 56f

        // Items Section Table Header
        paint.color = Color.rgb(241, 245, 249) // light neutral background
        val tableTop = currentY
        canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 20f, paint)

        // Header border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + 20f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
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

        canvas.drawText("#", colX[0], tableTop + 13f, paint)
        canvas.drawText("DESCRIPCIÓN DE MATERIAL / HERRAMIENTA", colX[1], tableTop + 13f, paint)
        canvas.drawText("CATEGORÍA", colX[2], tableTop + 13f, paint)
        canvas.drawText("CANT.", colX[3], tableTop + 13f, paint)
        canvas.drawText("UNIDAD", colX[4], tableTop + 13f, paint)
        canvas.drawText("SERIE / CÓD.", colX[5], tableTop + 13f, paint)
        canvas.drawText("CONDICIÓN", colX[6], tableTop + 13f, paint)

        currentY = tableTop + 20f

        // Items rows in strictly BLACK text
        val parsedItems = conduce.getParsedItems()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f

        if (parsedItems.isEmpty()) {
            paint.color = Color.WHITE
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)
            paint.color = Color.BLACK
            canvas.drawText("Sin ítems registrados en este conduce.", MARGIN + 10f, currentY + 15f, paint)
            currentY += 22f
        } else {
            for ((index, item) in parsedItems.take(8).withIndex()) {
                val rowHeight = 20f
                paint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                // Row borders
                paint.color = Color.rgb(203, 213, 225)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, currentY + rowHeight, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                // Black text
                paint.color = Color.BLACK
                canvas.drawText("${index + 1}", colX[0], currentY + 13f, paint)
                val desc = if (item.description.length > 36) item.description.take(34) + ".." else item.description
                canvas.drawText(desc, colX[1], currentY + 13f, paint)
                canvas.drawText(item.category, colX[2], currentY + 13f, paint)
                canvas.drawText("${item.quantity}", colX[3], currentY + 13f, paint)
                canvas.drawText(item.unit, colX[4], currentY + 13f, paint)
                val code = item.serialOrCode.ifBlank { "-" }.let { if (it.length > 14) it.take(12) + ".." else it }
                canvas.drawText(code, colX[5], currentY + 13f, paint)
                canvas.drawText(item.condition, colX[6], currentY + 13f, paint)

                currentY += rowHeight
            }
        }

        currentY += 8f

        // Observaciones Generales Box (with BLACK text)
        val obsRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 38f)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(obsRect, 4f, 4f, paint)
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(obsRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText("OBSERVACIONES / CONDICIONES DE TRASLADO:", MARGIN + 8f, currentY + 12f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val noteContent = conduce.notes.ifBlank {
            "Materiales y herramientas entregados para uso exclusivo en el proyecto indicado. El receptor verifica cantidades y buen estado."
        }
        val noteLine1 = if (noteContent.length > 95) noteContent.take(95) else noteContent
        val noteLine2 = if (noteContent.length > 95) noteContent.substring(95).take(95) else ""
        canvas.drawText(noteLine1, MARGIN + 8f, currentY + 23f, paint)
        if (noteLine2.isNotBlank()) {
            canvas.drawText(noteLine2, MARGIN + 8f, currentY + 33f, paint)
        }

        currentY += 48f

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

        // Footer in BLACK text
        paint.color = Color.BLACK
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Comprobante oficial de control y traslado de materiales y herramientas • Conduces de Obra • Pág. 1",
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
        height: Float
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        // Border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Title in BLACK
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText(title, left + 10f, top + 14f, paint)

        // Project Name in BLACK bold
        paint.color = Color.BLACK
        paint.textSize = 9.5f
        canvas.drawText(project.ifBlank { "No especificado" }.take(32), left + 10f, top + 29f, paint)

        // Responsible in BLACK
        paint.color = Color.BLACK
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val respText = if (responsible.isNotBlank()) "Resp: $responsible" else "Resp: Encargado de obra"
        canvas.drawText(respText.take(34), left + 10f, top + 44f, paint)
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

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Section Title in BLACK bold
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText("3. DATOS DEL TRANSPORTE Y CHOFER", left + 10f, top + 14f, paint)

        // Row 1: Conductor in BLACK
        paint.color = Color.BLACK
        paint.textSize = 9f
        val driverName = conduce.carrierName.ifBlank { "Transportista propio / Asignado" }
        canvas.drawText("Chofer: $driverName", left + 10f, top + 28f, paint)

        // Row 2: Transport note in BLACK
        paint.color = Color.BLACK
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Traslado y entrega de materiales y herramientas entre obras", left + 10f, top + 41f, paint)
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

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Header inside signature box
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(left + 1f, top + 1f, left + width - 1f, top + 16f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
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

        // Print signer name in BLACK
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText(name.take(24), left + (width / 2f), top + 72f, paint)

        // Verification subtitle in BLACK
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6f
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

        // White background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Top line
        paint.color = Color.BLACK
        paint.strokeWidth = 2f
        canvas.drawLine(MARGIN, 18f, PAGE_WIDTH - MARGIN, 18f, paint)

        // Header in BLACK
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("ANEXO DE EVIDENCIAS FOTOGRÁFICAS", MARGIN, 36f, paint)

        paint.textSize = 9f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FOLIO: ${conduce.conduceNumber}", PAGE_WIDTH - MARGIN, 36f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Subtitle bar in BLACK
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(MARGIN, 46f, PAGE_WIDTH - MARGIN, 66f), 4f, 4f, paint)
        paint.color = Color.BLACK
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Fotografías de respaldo capturadas durante la carga, transporte o recepción en obra:", MARGIN + 10f, 59f, paint)

        val photoWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 2f
        val photoHeight = 220f
        val startY = 80f

        for ((index, bitmap) in evidenceBitmaps.take(4).withIndex()) {
            val col = index % 2
            val row = index / 2
            val left = MARGIN + (col * (photoWidth + 16f))
            val top = startY + (row * (photoHeight + 40f))

            // Photo frame
            paint.color = Color.rgb(248, 250, 252)
            val frameRect = RectF(left, top, left + photoWidth, top + photoHeight)
            canvas.drawRoundRect(frameRect, 6f, 6f, paint)

            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(frameRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Draw image inside frame
            val innerRect = RectF(left + 4f, top + 4f, left + photoWidth - 4f, top + photoHeight - 4f)
            canvas.drawBitmap(bitmap, null, innerRect, paint)

            // Caption under photo in BLACK
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("Evidencia Fotográfica #${index + 1}", left + 4f, top + photoHeight + 14f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            canvas.drawText("Verificación visual de estado y entrega de carga", left + 4f, top + photoHeight + 25f, paint)
        }

        // Footer in BLACK
        paint.color = Color.BLACK
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Anexo de Evidencias • Conduces de Obra • Pág. 2",
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
