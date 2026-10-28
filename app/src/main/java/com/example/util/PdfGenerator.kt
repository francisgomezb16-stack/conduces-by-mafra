package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import com.example.data.model.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 30f

    fun generateConducePdf(
        context: Context,
        conduce: Conduce,
        company: CompanyProfile,
        evidenceBitmaps: List<Bitmap> = emptyList(),
        projects: List<Project> = emptyList()
    ): File {
        val pdfDocument = PdfDocument()
        val totalPages = if (evidenceBitmaps.size > 4) 2 else 1

        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)

        drawPageOne(
            context = context,
            canvas = page1.canvas,
            conduce = conduce,
            company = company,
            projects = projects,
            evidenceBitmaps = evidenceBitmaps,
            totalPages = totalPages
        )
        pdfDocument.finishPage(page1)

        // Only create a second page when photographic evidences exceed 4
        if (evidenceBitmaps.size > 4) {
            val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            drawEvidencePage(
                context = context,
                canvas = page2.canvas,
                conduce = conduce,
                extraEvidences = evidenceBitmaps.drop(4),
                totalPages = totalPages
            )
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
        company: CompanyProfile,
        projects: List<Project>,
        evidenceBitmaps: List<Bitmap>,
        totalPages: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Clean white page background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Top Header Accent Banner (Modern Slate & Industrial Amber)
        val topBannerHeight = 84f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), topBannerHeight, paint)

        // Thin Gold Accent Line below header
        paint.color = Color.rgb(245, 158, 11) // Amber 500
        paint.strokeWidth = 3f
        canvas.drawLine(0f, topBannerHeight, PAGE_WIDTH.toFloat(), topBannerHeight, paint)

        // Border line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(0f, topBannerHeight + 1f, PAGE_WIDTH.toFloat(), topBannerHeight + 1f, paint)

        // Load company logo
        val logoBitmap = loadCompanyLogo(context, company)
        val textStartX = if (logoBitmap != null) {
            val maxW = 100f
            val maxH = 64f
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat().coerceAtLeast(1f)
            val (w, h) = if (aspect > maxW / maxH) {
                maxW to (maxW / aspect)
            } else {
                (maxH * aspect) to maxH
            }
            val topY = 10f + ((maxH - h) / 2f)
            val destRect = RectF(MARGIN, topY, MARGIN + w, topY + h)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
            MARGIN + w + 12f
        } else {
            MARGIN
        }

        // Company Information (strictly in BLACK text)
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        val displayName = company.companyName.ifBlank { "Pedralbes" }
        canvas.drawText(displayName, textStartX, 25f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        val taxText = if (company.taxId.isNotBlank()) "RNC / ID Fiscal: ${company.taxId}" else "Constructora & Desarrollo de Proyectos"
        canvas.drawText(taxText, textStartX, 38f, paint)

        val contactText = listOfNotNull(
            company.phone.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            company.email.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        canvas.drawText(contactText.ifBlank { "Logística y Traslados entre Proyectos de Obra" }, textStartX, 50f, paint)

        if (company.address.isNotBlank()) {
            canvas.drawText(company.address.take(54), textStartX, 62f, paint)
        }

        // Right side badge of header: Folio & Date Box
        val folioBoxRect = RectF(PAGE_WIDTH - MARGIN - 132f, 10f, PAGE_WIDTH - MARGIN, 48f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(folioBoxRect, 5f, 5f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(245, 158, 11)
        paint.strokeWidth = 2f
        canvas.drawRoundRect(folioBoxRect, 5f, 5f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("FOLIO: ${conduce.conduceNumber}", folioBoxRect.centerX(), 27f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = conduce.createdAt?.toDate()?.let { sdf.format(it) } ?: sdf.format(Date())
        canvas.drawText(dateStr, folioBoxRect.centerX(), 40f, paint)

        // Status badge under folio
        val statusRect = RectF(PAGE_WIDTH - MARGIN - 110f, 54f, PAGE_WIDTH - MARGIN, 73f)
        val statusBgColor = when (conduce.status) {
            "Entregado" -> Color.rgb(220, 252, 231)
            "En Tránsito" -> Color.rgb(254, 243, 199)
            "Anulado" -> Color.rgb(254, 226, 226)
            else -> Color.rgb(241, 245, 249)
        }
        paint.color = statusBgColor
        canvas.drawRoundRect(statusRect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawRoundRect(statusRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText("ESTADO: ${conduce.status.uppercase(Locale.getDefault())}", statusRect.centerX(), 66f, paint)

        // Reset alignment
        paint.textAlign = Paint.Align.LEFT
        var currentY = 94f

        // Document Title Bar with type indicator
        val titleBarRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 18f)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(titleBarRect, 3f, 3f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawRoundRect(titleBarRect, 3f, 3f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText(
            "COMPROBANTE OFICIAL DE CONDUCE • TIPO: ${conduce.type.uppercase(Locale.getDefault())} • CONTROL DE TRASLADO Y FIRMAS",
            MARGIN + 8f,
            currentY + 12f,
            paint
        )

        currentY += 24f

        // Route Projects Cards with PHOTO THUMBNAILS (Miniaturas de obras involucradas)
        val originProj = projects.find { it.name.trim().equals(conduce.originProject.trim(), ignoreCase = true) }
        val destProj = projects.find { it.name.trim().equals(conduce.destinationProject.trim(), ignoreCase = true) }

        val originBitmap = originProj?.thumbnailBase64?.let { decodeBase64Bitmap(it) }
        val destBitmap = destProj?.thumbnailBase64?.let { decodeBase64Bitmap(it) }

        val cardWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 2f
        val cardHeight = 60f

        drawProjectCardWithThumbnail(
            canvas = canvas,
            paint = paint,
            tag = "PUNTO DE ORIGEN",
            projectName = conduce.originProject,
            responsible = conduce.originResponsible,
            location = originProj?.location ?: "",
            thumbnail = originBitmap,
            left = MARGIN,
            top = currentY,
            width = cardWidth,
            height = cardHeight,
            accentColor = Color.rgb(245, 158, 11)
        )

        // Route Connector Arrow between cards
        val arrowCenterX = MARGIN + cardWidth + 8f
        val arrowCenterY = currentY + (cardHeight / 2f)
        paint.color = Color.BLACK
        paint.strokeWidth = 1.5f
        canvas.drawLine(arrowCenterX - 5f, arrowCenterY, arrowCenterX + 5f, arrowCenterY, paint)
        canvas.drawLine(arrowCenterX + 2f, arrowCenterY - 3f, arrowCenterX + 5f, arrowCenterY, paint)
        canvas.drawLine(arrowCenterX + 2f, arrowCenterY + 3f, arrowCenterX + 5f, arrowCenterY, paint)

        drawProjectCardWithThumbnail(
            canvas = canvas,
            paint = paint,
            tag = "PUNTO DE DESTINO",
            projectName = conduce.destinationProject,
            responsible = conduce.destinationResponsible,
            location = destProj?.location ?: "",
            thumbnail = destBitmap,
            left = MARGIN + cardWidth + 16f,
            top = currentY,
            width = cardWidth,
            height = cardHeight,
            accentColor = Color.rgb(16, 185, 129)
        )

        currentY += cardHeight + 6f

        // Transport & Driver Card (Only driver name)
        drawCarrierCard(
            canvas = canvas,
            paint = paint,
            conduce = conduce,
            left = MARGIN,
            top = currentY,
            width = PAGE_WIDTH - (MARGIN * 2),
            height = 30f
        )

        currentY += 36f

        // Table Header: Materials and Tools
        val tableTop = currentY
        val tableHeaderHeight = 18f

        paint.color = Color.rgb(15, 23, 42) // Dark Slate
        canvas.drawRect(MARGIN, tableTop, PAGE_WIDTH - MARGIN, tableTop + tableHeaderHeight, paint)

        paint.color = Color.rgb(245, 158, 11)
        canvas.drawRect(MARGIN, tableTop, MARGIN + 4f, tableTop + tableHeaderHeight, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f

        val colX = floatArrayOf(
            MARGIN + 8f,        // #
            MARGIN + 26f,       // Descripción
            MARGIN + 215f,      // Categoría
            MARGIN + 280f,      // Cantidad
            MARGIN + 335f,      // Unidad
            MARGIN + 385f,      // Código / Serie
            MARGIN + 465f       // Condición
        )

        canvas.drawText("#", colX[0], tableTop + 12f, paint)
        canvas.drawText("DESCRIPCIÓN DEL MATERIAL / EQUIPO", colX[1], tableTop + 12f, paint)
        canvas.drawText("CATEGORÍA", colX[2], tableTop + 12f, paint)
        canvas.drawText("CANT.", colX[3], tableTop + 12f, paint)
        canvas.drawText("UNIDAD", colX[4], tableTop + 12f, paint)
        canvas.drawText("CÓDIGO / SERIE", colX[5], tableTop + 12f, paint)
        canvas.drawText("CONDICIÓN", colX[6], tableTop + 12f, paint)

        currentY = tableTop + tableHeaderHeight

        // Items rows (All text strictly in BLACK)
        val parsedItems = conduce.getParsedItems()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f

        val maxItemsToShow = if (evidenceBitmaps.isNotEmpty()) 5 else 8
        val itemsToDraw = parsedItems.take(maxItemsToShow)

        if (itemsToDraw.isEmpty()) {
            paint.color = Color.WHITE
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 18f, paint)
            paint.color = Color.BLACK
            canvas.drawText("Sin ítems registrados en este conduce.", MARGIN + 10f, currentY + 12f, paint)
            currentY += 18f
        } else {
            for ((index, item) in itemsToDraw.withIndex()) {
                val rowHeight = 17f
                paint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                paint.color = Color.rgb(226, 232, 240)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, currentY + rowHeight, PAGE_WIDTH - MARGIN, currentY + rowHeight, paint)

                paint.color = Color.BLACK
                canvas.drawText("${index + 1}", colX[0], currentY + 11.5f, paint)

                val desc = if (item.description.length > 36) item.description.take(34) + ".." else item.description
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(desc, colX[1], currentY + 11.5f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText(item.category, colX[2], currentY + 11.5f, paint)
                canvas.drawText("${item.quantity}", colX[3], currentY + 11.5f, paint)
                canvas.drawText(item.unit, colX[4], currentY + 11.5f, paint)

                val code = item.serialOrCode.ifBlank { "-" }.let { if (it.length > 14) it.take(12) + ".." else it }
                canvas.drawText(code, colX[5], currentY + 11.5f, paint)
                canvas.drawText(item.condition, colX[6], currentY + 11.5f, paint)

                currentY += rowHeight
            }

            // Summary row under items
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 14f, paint)
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 6.5f
            val totalQty = parsedItems.sumOf { it.quantity }
            canvas.drawText(
                "TOTAL: ${parsedItems.size} ÍTEMS  •  SUMA CANTIDADES: $totalQty  •  CONDUCE VERIFICADO EN DESTINO",
                MARGIN + 10f,
                currentY + 10f,
                paint
            )
            currentY += 14f
        }

        currentY += 6f

        // Observaciones Generales Box
        val obsRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 28f)
        paint.color = Color.rgb(255, 251, 235)
        canvas.drawRoundRect(obsRect, 3f, 3f, paint)

        paint.color = Color.rgb(245, 158, 11)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(obsRect, 3f, 3f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText("OBSERVACIONES / CONDICIONES DE TRASLADO:", MARGIN + 6f, currentY + 10f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.5f
        val noteContent = conduce.notes.ifBlank {
            "Materiales y herramientas entregados para uso exclusivo en el proyecto indicado. El receptor verifica cantidades y buen estado."
        }
        val noteLine1 = if (noteContent.length > 105) noteContent.take(105) else noteContent
        val noteLine2 = if (noteContent.length > 105) noteContent.substring(105).take(105) else ""
        canvas.drawText(noteLine1, MARGIN + 6f, currentY + 18.5f, paint)
        if (noteLine2.isNotBlank()) {
            canvas.drawText(noteLine2, MARGIN + 6f, currentY + 25.5f, paint)
        }

        currentY += 34f

        // EVIDENCIAS FOTOGRÁFICAS DIRECTO EN LA MISMA HOJA (HASTA 4 FOTOS)
        val pageOneEvidences = evidenceBitmaps.take(4)
        if (pageOneEvidences.isNotEmpty()) {
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f
            canvas.drawText("EVIDENCIAS FOTOGRÁFICAS EN SITIO (${pageOneEvidences.size} FOTOS EN ESTA HOJA):", MARGIN, currentY + 8f, paint)

            currentY += 14f

            val totalPhotoWidth = PAGE_WIDTH - (MARGIN * 2)
            val gap = 8f
            val count = pageOneEvidences.size
            val pWidth = (totalPhotoWidth - (gap * (count - 1))) / count
            val pHeight = 64f

            for ((pIdx, photo) in pageOneEvidences.withIndex()) {
                val pLeft = MARGIN + (pIdx * (pWidth + gap))
                val pRect = RectF(pLeft, currentY, pLeft + pWidth, currentY + pHeight)

                // Background
                paint.color = Color.rgb(241, 245, 249)
                canvas.drawRoundRect(pRect, 4f, 4f, paint)

                // Clip photo
                canvas.save()
                val photoPath = Path().apply { addRoundRect(pRect, 4f, 4f, Path.Direction.CW) }
                canvas.clipPath(photoPath)
                canvas.drawBitmap(photo, null, pRect, paint)
                canvas.restore()

                // Border
                paint.style = Paint.Style.STROKE
                paint.color = Color.BLACK
                paint.strokeWidth = 1f
                canvas.drawRoundRect(pRect, 4f, 4f, paint)
                paint.style = Paint.Style.FILL

                // Caption badge at bottom of photo
                val badgeRect = RectF(pLeft, currentY + pHeight - 12f, pLeft + pWidth, currentY + pHeight)
                paint.color = Color.argb(200, 0, 0, 0)
                canvas.drawRect(badgeRect, paint)

                paint.color = Color.WHITE
                paint.textSize = 6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Evidencia ${pIdx + 1}", pRect.centerX(), currentY + pHeight - 3.5f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            currentY += pHeight + 8f
        }

        // Digital Signatures Section (3 signature columns)
        val sigBoxWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 3f
        val sigBoxHeight = 70f

        drawSignatureBox(
            canvas = canvas,
            paint = paint,
            title = "1. DESPACHADO / ENTREGADO",
            name = conduce.dispatcherName.ifBlank { "Encargado Despacho" },
            signatureBase64 = conduce.dispatcherSignature,
            left = MARGIN,
            top = currentY,
            width = sigBoxWidth,
            height = sigBoxHeight
        )

        drawSignatureBox(
            canvas = canvas,
            paint = paint,
            title = "2. TRANSPORTISTA / CHOFER",
            name = conduce.carrierName.ifBlank { "Transporte Asignado" },
            signatureBase64 = conduce.carrierSignature,
            left = MARGIN + sigBoxWidth + 8f,
            top = currentY,
            width = sigBoxWidth,
            height = sigBoxHeight
        )

        drawSignatureBox(
            canvas = canvas,
            paint = paint,
            title = "3. RECIBIDO EN OBRA",
            name = conduce.receiverName.ifBlank { "Responsable en Obra" },
            signatureBase64 = conduce.receiverSignature,
            left = MARGIN + (sigBoxWidth * 2) + 16f,
            top = currentY,
            width = sigBoxWidth,
            height = sigBoxHeight
        )

        // Security authenticity badge
        val badgeY = currentY + sigBoxHeight + 6f
        paint.color = Color.rgb(241, 245, 249)
        val badgeRect = RectF(MARGIN, badgeY, PAGE_WIDTH - MARGIN, badgeY + 12f)
        canvas.drawRoundRect(badgeRect, 3f, 3f, paint)

        paint.color = Color.BLACK
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "✓ DOCUMENTO ELECTRÓNICO AUDITABLE • FIRMAS DIGITALIZADAS • CONDUCES DE OBRA PEDRALBES",
            PAGE_WIDTH / 2f,
            badgeY + 9f,
            paint
        )

        // Footer
        paint.color = Color.BLACK
        paint.textSize = 7f
        canvas.drawText(
            "Comprobante oficial de control y traslado de materiales • Pág. 1 de $totalPages",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 12f,
            paint
        )
    }

    private fun drawProjectCardWithThumbnail(
        canvas: Canvas,
        paint: Paint,
        tag: String,
        projectName: String,
        responsible: String,
        location: String,
        thumbnail: Bitmap?,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        accentColor: Int
    ) {
        val rect = RectF(left, top, left + width, top + height)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 5f, 5f, paint)

        // Border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 5f, 5f, paint)
        paint.style = Paint.Style.FILL

        // Accent top ribbon
        val ribbonRect = RectF(left, top, left + width, top + 13f)
        paint.color = accentColor
        canvas.drawRoundRect(ribbonRect, 5f, 5f, paint)
        canvas.drawRect(left, top + 5f, left + width, top + 13f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("• $tag", left + 6f, top + 9.5f, paint)

        // Thumbnail on the left side
        val thumbSize = 40f
        val thumbLeft = left + 6f
        val thumbTop = top + 16f
        val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize)

        if (thumbnail != null) {
            canvas.save()
            val clipPath = Path().apply { addRoundRect(thumbRect, 4f, 4f, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(thumbnail, null, thumbRect, paint)
            canvas.restore()

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawRoundRect(thumbRect, 4f, 4f, paint)
            paint.style = Paint.Style.FILL
        } else {
            // Stylized construction thumbnail placeholder
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRoundRect(thumbRect, 4f, 4f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 0.8f
            canvas.drawRoundRect(thumbRect, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            // Draw miniature building silhouette
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawRect(thumbLeft + 8f, thumbTop + 12f, thumbLeft + 20f, thumbTop + 32f, paint)
            canvas.drawRect(thumbLeft + 18f, thumbTop + 6f, thumbLeft + 32f, thumbTop + 32f, paint)
            paint.color = Color.WHITE
            canvas.drawRect(thumbLeft + 11f, thumbTop + 15f, thumbLeft + 17f, thumbTop + 21f, paint)
            canvas.drawRect(thumbLeft + 22f, thumbTop + 10f, thumbLeft + 28f, thumbTop + 16f, paint)
        }

        // Project details to the right of thumbnail
        val infoLeft = thumbLeft + thumbSize + 7f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        val displayProj = projectName.ifBlank { "No especificado" }.let {
            if (it.length > 24) it.take(22) + ".." else it
        }
        canvas.drawText(displayProj, infoLeft, top + 26f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.5f
        val locText = location.ifBlank { "Ubicación en obra" }.let {
            if (it.length > 26) it.take(24) + ".." else it
        }
        canvas.drawText("📍 $locText", infoLeft, top + 37f, paint)

        val respText = responsible.ifBlank { "Encargado de obra" }.let {
            if (it.length > 26) it.take(24) + ".." else it
        }
        canvas.drawText("👤 Resp: $respText", infoLeft, top + 48f, paint)
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
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        canvas.drawText("• DATOS DEL TRANSPORTE Y CHOFER ASIGNADO:", left + 8f, top + 11f, paint)

        // Driver Name only
        paint.color = Color.BLACK
        paint.textSize = 8.5f
        val driverName = conduce.carrierName.ifBlank { "Transporte Propio / Asignado" }
        canvas.drawText("Chofer: $driverName", left + 8f, top + 23f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        val vehicleDetails = listOfNotNull(
            conduce.vehiclePlate.takeIf { it.isNotBlank() }?.let { "Placa: $it" },
            conduce.vehicleDescription.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        if (vehicleDetails.isNotBlank()) {
            canvas.drawText(vehicleDetails, left + 280f, top + 23f, paint)
        }
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
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, rect.centerX(), top + 10f, paint)

        val sigAreaTop = top + 13f
        val sigAreaBottom = top + height - 16f

        if (signatureBase64.isNotBlank()) {
            val sigBitmap = decodeBase64Bitmap(signatureBase64)
            if (sigBitmap != null) {
                val sigRect = RectF(left + 6f, sigAreaTop, left + width - 6f, sigAreaBottom)
                canvas.drawBitmap(sigBitmap, null, sigRect, paint)
            } else {
                paint.color = Color.rgb(71, 85, 105)
                paint.textSize = 6.5f
                canvas.drawText("[Firma Digitalizada]", rect.centerX(), top + 34f, paint)
            }
        } else {
            paint.color = Color.rgb(148, 163, 184)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawLine(left + 12f, top + 36f, left + width - 12f, top + 36f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.BLACK
            paint.textSize = 6f
            canvas.drawText("Pendiente de firma", rect.centerX(), top + 32f, paint)
        }

        // Line and Name
        paint.color = Color.BLACK
        paint.strokeWidth = 0.8f
        canvas.drawLine(left + 8f, top + height - 14f, left + width - 8f, top + height - 14f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 6.5f
        canvas.drawText(name.take(24), rect.centerX(), top + height - 5f, paint)

        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawEvidencePage(
        context: Context,
        canvas: Canvas,
        conduce: Conduce,
        extraEvidences: List<Bitmap>,
        totalPages: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Header Accent
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 48f, paint)

        paint.color = Color.rgb(245, 158, 11)
        paint.strokeWidth = 2f
        canvas.drawLine(0f, 48f, PAGE_WIDTH.toFloat(), 48f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ANEXO DE EVIDENCIAS FOTOGRÁFICAS ADICIONALES", MARGIN, 28f, paint)

        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Folio: ${conduce.conduceNumber}", PAGE_WIDTH - MARGIN, 28f, paint)
        paint.textAlign = Paint.Align.LEFT

        val photoWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 2f
        val photoHeight = 150f

        for ((index, bitmap) in extraEvidences.take(4).withIndex()) {
            val col = index % 2
            val row = index / 2

            val left = MARGIN + (col * (photoWidth + 16f))
            val top = 65f + (row * (photoHeight + 46f))

            val photoRect = RectF(left, top, left + photoWidth, top + photoHeight)

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(photoRect, 6f, 6f, paint)

            canvas.save()
            val clipPath = Path().apply { addRoundRect(photoRect, 6f, 6f, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(bitmap, null, photoRect, paint)
            canvas.restore()

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawRoundRect(photoRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Caption under photo
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("Evidencia Fotográfica Adicional #${index + 5}", left + 4f, top + photoHeight + 14f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7f
            canvas.drawText("Verificación visual de estado y entrega de carga en obra", left + 4f, top + photoHeight + 25f, paint)
        }

        // Footer
        paint.color = Color.BLACK
        paint.textSize = 7f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Anexo de Evidencias Fotográficas • Conduces de Obra • Pág. 2 de $totalPages",
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
