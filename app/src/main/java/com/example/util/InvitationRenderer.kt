package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.FileProvider
import com.example.data.EventEntity
import com.example.data.EventRepository
import com.example.data.GuestEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class PickedMediaInfo(
    val localPath: String,
    val aspectRatio: Float,
    val mediaType: String // IMAGE, GIF, VIDEO
)

object InvitationRenderer {

    /**
     * Generates a real, high-contrast QR Code Bitmap for `eb:<eventId>:<guestCode>`.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int,
        fgColorInt: Int,
        bgColorInt: Int = android.graphics.Color.WHITE
    ): Bitmap {
        return QrCodec.generateQrBitmap(
            content = content,
            sizePx = sizePx,
            fgColorInt = fgColorInt,
            bgColorInt = bgColorInt
        )
    }

    /**
     * Decodes a QR code string from a Bitmap (useful for verifying exported invitations or scanning from gallery).
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return QrCodec.decodeQrFromBitmap(bitmap)
    }

    /**
     * Copies a user-picked image/GIF/video into internal app storage and computes its exact original aspect ratio
     * without cropping.
     */
    suspend fun copyPickedMediaAndGetInfo(
        context: Context,
        uri: Uri,
        requestedVideo: Boolean = false
    ): PickedMediaInfo? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            val isGif = mimeType.contains("gif") || uri.toString().lowercase().endsWith(".gif")
            val isVideo = requestedVideo || mimeType.startsWith("video/")
            val ext = when {
                isGif -> "gif"
                isVideo -> "mp4"
                mimeType.contains("png") -> "png"
                else -> "jpg"
            }
            val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
            val destFile = File(dir, "bg_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            var ratio = 0.714f
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(destFile.absolutePath)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull()
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull()
                    if (w != null && h != null && h > 0f) {
                        ratio = (w / h).coerceIn(0.35f, 2.2f)
                    }
                } catch (_: Exception) {
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            } else {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(destFile.absolutePath, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    ratio = (opts.outWidth.toFloat() / opts.outHeight.toFloat()).coerceIn(0.35f, 2.2f)
                }
            }

            PickedMediaInfo(
                localPath = destFile.absolutePath,
                aspectRatio = ratio,
                mediaType = when {
                    isVideo -> "VIDEO"
                    isGif -> "GIF"
                    else -> "IMAGE"
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Renders a complete invitation card Bitmap for a guest with exact normalized positions,
     * selected font, colors, and real scannable QR code.
     *
     * Normal mode: up to 2000px longest side (fast JPG).
     * Original quality mode: up to 8192px longest side (lossless PNG).
     */
    fun renderInvitationBitmap(
        event: EventEntity,
        guest: GuestEntity,
        originalQuality: Boolean = false,
        maxDimensionOverride: Int? = null
    ): Bitmap {
        val ratio = event.backgroundAspectRatio.coerceIn(0.4f, 2.0f)
        val maxSide = maxDimensionOverride ?: if (originalQuality) 2400 else 1400

        var customBgBitmap: Bitmap? = null
        if (event.backgroundType != "PRESET" && !event.backgroundUri.isNullOrBlank()) {
            val file = File(event.backgroundUri)
            if (file.exists()) {
                if (event.backgroundType == "VIDEO") {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        customBgBitmap = retriever.getFrameAtTime(0)
                    } catch (_: Exception) {
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    val targetLimit = if (originalQuality) 4096 else 2000
                    var sampleSize = 1
                    while (bounds.outWidth / sampleSize > targetLimit || bounds.outHeight / sampleSize > targetLimit) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    customBgBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                }
            }
        }

        val width: Int
        val height: Int
        if (customBgBitmap != null) {
            val srcW = customBgBitmap.width
            val srcH = customBgBitmap.height
            val cap = maxDimensionOverride ?: if (originalQuality) 8192 else 2000
            if (srcW <= cap && srcH <= cap && originalQuality) {
                width = srcW
                height = srcH
            } else {
                val scale = min(cap.toFloat() / srcW.toFloat(), cap.toFloat() / srcH.toFloat()).coerceAtMost(1.5f)
                width = (srcW * scale).roundToInt().coerceAtLeast(600)
                height = (srcH * scale).roundToInt().coerceAtLeast(600)
            }
        } else {
            if (ratio <= 1f) {
                height = maxSide
                width = (maxSide * ratio).roundToInt().coerceAtLeast(600)
            } else {
                width = maxSide
                height = (maxSide / ratio).roundToInt().coerceAtLeast(600)
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (customBgBitmap != null) {
            val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(customBgBitmap, null, destRect, paint)
            customBgBitmap.recycle()
        } else {
            drawLuxuryPresetBackgroundOnCanvas(canvas, width.toFloat(), height.toFloat(), event.backgroundPresetId)
        }

        val fontChoice = DesignCatalog.fontChoices.getOrElse(event.fontIndex) { DesignCatalog.fontChoices.first() }
        val nameColorInt = parseColorInt(event.nameColorHex, android.graphics.Color.WHITE)
        val qrColorInt = parseColorInt(event.qrColorHex, android.graphics.Color.parseColor("#FF121826"))
        val qrBgColorInt = parseColorInt(event.qrBgColorHex, android.graphics.Color.WHITE)

        val isWeddingCard = event.groomName.isNotBlank() || event.brideName.isNotBlank() ||
            event.headerVerse.isNotBlank() || event.venueName.isNotBlank()

        if (isWeddingCard) {
            // --- FULL LUXURY WEDDING CARD RENDERING (MATCHING USER REFERENCE DESIGN) ---
            var currentY = height * 0.15f

            // 1. Top Calligraphy Title: "دعوة زفاف" (Wedding Invitation)
            val topHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nameColorInt
                typeface = fontChoice.androidTypeface
                textAlign = Paint.Align.CENTER
                textSize = width * 0.068f
                isFakeBoldText = true
                setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(120, 0, 0, 0))
            }
            val titleStr = event.name.ifBlank { "دعوة زفاف" }
            canvas.drawText(titleStr, width * 0.5f, currentY, topHeaderPaint)
            currentY += height * 0.045f

            // 2. Quranic Ayah / Opening Verse (Centered, elegant smaller size)
            if (event.headerVerse.isNotBlank()) {
                val versePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = width * 0.030f
                    alpha = 230
                }
                val verseLines = wrapTextIntoLines(event.headerVerse, versePaint, width * 0.78f)
                for (vLine in verseLines) {
                    canvas.drawText(vLine, width * 0.5f, currentY, versePaint)
                    currentY += versePaint.textSize * 1.35f
                }
                currentY += height * 0.015f
            }

            // 3. Welcoming Intro & Hosts
            val introPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nameColorInt
                typeface = fontChoice.androidTypeface
                textAlign = Paint.Align.CENTER
                textSize = width * 0.034f
                isFakeBoldText = true
            }
            canvas.drawText("بكل حُبّ ومودة يتشرف", width * 0.5f, currentY, introPaint)
            currentY += height * 0.038f

            if (event.hostsText.isNotBlank()) {
                val hostsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = width * 0.033f
                }
                val hostsLines = wrapTextIntoLines(event.hostsText, hostsPaint, width * 0.82f)
                for (hLine in hostsLines) {
                    canvas.drawText(hLine, width * 0.5f, currentY, hostsPaint)
                    currentY += hostsPaint.textSize * 1.35f
                }
                currentY += height * 0.015f
            }

            canvas.drawText("بدعوتكم لحضور حفل زفاف", width * 0.5f, currentY, introPaint)
            currentY += height * 0.052f

            // 4. Large Groom & Bride Names in Prominent Calligraphy (Centerpiece)
            val coupleText = if (event.groomName.isNotBlank() && event.brideName.isNotBlank()) {
                "${event.groomName}  ♡  ${event.brideName}"
            } else if (event.groomName.isNotBlank()) {
                event.groomName
            } else if (event.brideName.isNotBlank()) {
                event.brideName
            } else {
                event.name
            }

            val couplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nameColorInt
                typeface = fontChoice.androidTypeface
                textAlign = Paint.Align.CENTER
                textSize = width * 0.082f
                isFakeBoldText = true
                setShadowLayer(14f, 0f, 4f, android.graphics.Color.argb(160, 0, 0, 0))
            }
            canvas.drawText(coupleText, width * 0.5f, currentY, couplePaint)
            currentY += height * 0.055f

            // 5. Celebration Details: Day & Date, Venue, Time
            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nameColorInt
                typeface = fontChoice.androidTypeface
                textAlign = Paint.Align.CENTER
                textSize = width * 0.035f
            }

            val dayAndDate = buildString {
                if (event.dayName.isNotBlank()) append(event.dayName).append("  ")
                if (event.date.isNotBlank()) append(event.date)
            }.trim()
            if (dayAndDate.isNotBlank()) {
                canvas.drawText("📅 $dayAndDate", width * 0.5f, currentY, detailPaint)
                currentY += height * 0.036f
            }

            val venueStr = buildString {
                if (event.venueName.isNotBlank()) append(event.venueName)
                if (event.venueAddress.isNotBlank()) {
                    if (isNotEmpty()) append(" – ")
                    append(event.venueAddress)
                }
            }.trim()
            if (venueStr.isNotBlank()) {
                canvas.drawText("🏛️ $venueStr", width * 0.5f, currentY, detailPaint)
                currentY += height * 0.036f
            }

            val timeStr = buildString {
                if (event.timeFrom.isNotBlank()) append("الساعة ").append(event.timeFrom)
                if (event.timeTo.isNotBlank()) {
                    append(" إلى ").append(event.timeTo)
                }
            }.trim()
            if (timeStr.isNotBlank()) {
                canvas.drawText("⏰ $timeStr", width * 0.5f, currentY, detailPaint)
                currentY += height * 0.040f
            }

            // 6. Location QR Code or Guest Pass QR Code
            val qrContent = if (guest.code.isNotBlank()) {
                guest.qrPayload()
            } else if (event.showLocationQr && event.locationUrl.isNotBlank()) {
                event.locationUrl
            } else null

            if (qrContent != null) {
                val qrBoxSize = (width * 0.22f).roundToInt()
                val qrBitmap = generateQrBitmap(
                    content = qrContent,
                    sizePx = qrBoxSize,
                    fgColorInt = qrColorInt,
                    bgColorInt = qrBgColorInt
                )
                val qrCenterX = width * 0.5f
                val qrCenterY = currentY + qrBoxSize * 0.5f
                val pad = qrBoxSize * 0.08f
                val cardRect = RectF(
                    qrCenterX - qrBoxSize / 2f - pad,
                    qrCenterY - qrBoxSize / 2f - pad,
                    qrCenterX + qrBoxSize / 2f + pad,
                    qrCenterY + qrBoxSize / 2f + pad
                )
                val qrCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = qrBgColorInt
                    setShadowLayer(10f, 0f, 4f, android.graphics.Color.argb(80, 0, 0, 0))
                }
                canvas.drawRoundRect(cardRect, 14f, 14f, qrCardPaint)
                canvas.drawBitmap(
                    qrBitmap,
                    qrCenterX - qrBitmap.width / 2f,
                    qrCenterY - qrBitmap.height / 2f,
                    null
                )
                qrBitmap.recycle()

                val qrLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = width * 0.026f
                }
                val label = if (guest.code.isNotBlank()) {
                    "#${guest.code} ${if (guest.name.isNotBlank()) "• " + guest.name else ""}"
                } else {
                    "📍 مسح لوكيشن القاعة على الخريطة"
                }
                canvas.drawText(label, qrCenterX, qrCenterY + qrBoxSize / 2f + pad * 1.6f, qrLabelPaint)
                currentY = qrCenterY + qrBoxSize / 2f + pad * 2.5f
            }

            // 7. Notes & Closing Wishes
            if (event.notesText.isNotBlank()) {
                val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = width * 0.031f
                    alpha = 220
                }
                canvas.drawText(event.notesText, width * 0.5f, currentY, notesPaint)
                currentY += height * 0.035f
            }

            if (event.closingText.isNotBlank()) {
                val closingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = width * 0.033f
                    isFakeBoldText = true
                }
                canvas.drawText(event.closingText, width * 0.5f, currentY, closingPaint)
            }
        } else {
            // --- STANDARD MINIMALIST INVITATION CARD RENDERING ---
            // Draw Event Title at top if preset background
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = nameColorInt
                typeface = fontChoice.androidTypeface
                textAlign = Paint.Align.CENTER
                textSize = width * 0.052f
                setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(140, 0, 0, 0))
            }
            canvas.drawText(event.name, width * 0.5f, height * 0.16f, titlePaint)

            // 1. Draw QR Code at normalized (qrPosX, qrPosY)
            val qrBoxSize = (width * event.qrSizeRatio.coerceIn(0.15f, 0.45f)).roundToInt()
            val qrPayload = if (guest.code.isNotBlank()) {
                guest.qrPayload()
            } else if (event.showLocationQr && event.locationUrl.isNotBlank()) {
                event.locationUrl
            } else {
                "eb:${event.id}:00000000"
            }
            val qrBitmap = generateQrBitmap(
                content = qrPayload,
                sizePx = qrBoxSize,
                fgColorInt = qrColorInt,
                bgColorInt = qrBgColorInt
            )
            val qrCenterX = (event.qrPosX.coerceIn(0.1f, 0.9f) * width)
            val qrCenterY = (event.qrPosY.coerceIn(0.1f, 0.9f) * height)
            val pad = qrBoxSize * 0.08f
            val cardRect = RectF(
                qrCenterX - qrBoxSize / 2f - pad,
                qrCenterY - qrBoxSize / 2f - pad,
                qrCenterX + qrBoxSize / 2f + pad,
                qrCenterY + qrBoxSize / 2f + pad * 2.1f
            )

            val qrCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = qrBgColorInt
                setShadowLayer(16f, 0f, 6f, android.graphics.Color.argb(110, 0, 0, 0))
            }
            canvas.drawRoundRect(cardRect, pad * 0.9f, pad * 0.9f, qrCardPaint)

            val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = qrColorInt
                style = Paint.Style.STROKE
                strokeWidth = (width * 0.004f).coerceAtLeast(2f)
            }
            canvas.drawRoundRect(cardRect, pad * 0.9f, pad * 0.9f, qrBorderPaint)

            canvas.drawBitmap(
                qrBitmap,
                qrCenterX - qrBitmap.width / 2f,
                qrCenterY - qrBitmap.height / 2f,
                null
            )

            val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = qrColorInt
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                textSize = (qrBoxSize * 0.11f).coerceAtLeast(16f)
            }
            val labelStr = if (guest.code.isNotBlank()) "#${guest.code}" else "📍 لوكيشن الفرح"
            canvas.drawText(
                labelStr,
                qrCenterX,
                qrCenterY + qrBoxSize / 2f + pad * 1.35f,
                codePaint
            )
            qrBitmap.recycle()

            // 2. Draw Guest Name if enabled at (namePosX, namePosY)
            if (event.showGuestName && guest.name.isNotBlank()) {
                val scaleFactor = width / 380f
                val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = (event.nameSizeSp * scaleFactor).coerceIn(20f, width * 0.11f)
                    setShadowLayer(10f, 0f, 3f, android.graphics.Color.argb(150, 0, 0, 0))
                }
                val nameX = event.namePosX.coerceIn(0.08f, 0.92f) * width
                val nameY = event.namePosY.coerceIn(0.08f, 0.95f) * height
                canvas.drawText(guest.name, nameX, nameY, namePaint)
            }

            // 3. Draw Date & Time if enabled at (datePosX, datePosY)
            if (event.showDate || event.showTime) {
                val dateX = event.datePosX.coerceIn(0.08f, 0.92f) * width
                val dateY = event.datePosY.coerceIn(0.08f, 0.95f) * height
                val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = nameColorInt
                    typeface = fontChoice.androidTypeface
                    textAlign = Paint.Align.CENTER
                    textSize = (width * 0.036f).coerceAtLeast(18f)
                    setShadowLayer(8f, 0f, 2f, android.graphics.Color.argb(140, 0, 0, 0))
                }

                val dateLine = if (event.showDate) event.date else ""
                val timeLine = if (event.showTime) {
                    buildString {
                        if (event.timeFrom.isNotBlank()) append(event.timeFrom)
                        if (event.timeTo.isNotBlank()) {
                            if (isNotEmpty()) append(" – ")
                            append(event.timeTo)
                        }
                    }
                } else ""

                if (dateLine.isNotBlank() && timeLine.isNotBlank()) {
                    canvas.drawText(dateLine, dateX, dateY - metaPaint.textSize * 0.6f, metaPaint)
                    canvas.drawText(timeLine, dateX, dateY + metaPaint.textSize * 0.7f, metaPaint)
                } else if (dateLine.isNotBlank()) {
                    canvas.drawText(dateLine, dateX, dateY, metaPaint)
                } else if (timeLine.isNotBlank()) {
                    canvas.drawText(timeLine, dateX, dateY, metaPaint)
                }
            }
        }

        return bitmap
    }

    private fun wrapTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        for (w in words) {
            val candidate = if (currentLine.isEmpty()) w else "$currentLine $w"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.clear().append(candidate)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine.clear().append(w)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun drawLuxuryPresetBackgroundOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        presetId: Int
    ) {
        val presetStyle = DesignCatalog.presetBackgrounds.getOrElse(presetId) { DesignCatalog.presetBackgrounds.first() }
        val topInt = colorToInt(presetStyle.topColor)
        val centerInt = colorToInt(presetStyle.centerColor)
        val bottomInt = colorToInt(presetStyle.bottomColor)
        val accentInt = colorToInt(presetStyle.accentColor)
        val secondaryInt = colorToInt(presetStyle.secondaryAccentColor)

        // Base vertical gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(topInt, centerInt, bottomInt),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Subtle radial glow in center
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.5f,
                height * 0.45f,
                width * 0.65f,
                intArrayOf(
                    android.graphics.Color.argb(
                        55,
                        android.graphics.Color.red(accentInt),
                        android.graphics.Color.green(accentInt),
                        android.graphics.Color.blue(accentInt)
                    ),
                    android.graphics.Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, glowPaint)

        // Double ornamental border + Arabian arch
        val margin = width * 0.055f
        val innerMargin = width * 0.075f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentInt
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.005f).coerceAtLeast(2.5f)
        }
        val thinStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryInt
            style = Paint.Style.STROKE
            alpha = 180
            strokeWidth = (width * 0.0025f).coerceAtLeast(1.5f)
        }

        canvas.drawRoundRect(
            RectF(margin, margin, width - margin, height - margin),
            width * 0.04f,
            width * 0.04f,
            strokePaint
        )
        canvas.drawRoundRect(
            RectF(innerMargin, innerMargin, width - innerMargin, height - innerMargin),
            width * 0.03f,
            width * 0.03f,
            thinStrokePaint
        )

        // Draw celebratory 8-pointed star at top center
        drawEightPointStar(
            canvas = canvas,
            cx = width * 0.5f,
            cy = height * 0.085f,
            outerRadius = width * 0.032f,
            colorInt = accentInt
        )
    }

    private fun drawEightPointStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        outerRadius: Float,
        colorInt: Int
    ) {
        val path = Path()
        val points = 8
        val innerRadius = outerRadius * 0.45f
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    /**
     * Writes the rendered invitation image to cache/invitations and returns its FileProvider Uri.
     */
    suspend fun createInvitationFileUri(
        context: Context,
        event: EventEntity,
        guest: GuestEntity,
        originalQuality: Boolean
    ): Uri = withContext(Dispatchers.IO) {
        val bitmap = renderInvitationBitmap(event, guest, originalQuality)
        val dir = File(context.cacheDir, "invitations").apply { mkdirs() }
        val ext = if (originalQuality) "png" else "jpg"
        val safeGuestName = guest.name.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_").take(30)
        val file = File(dir, "invite_${safeGuestName}_${guest.code}.$ext")
        FileOutputStream(file).use { out ->
            if (originalQuality) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Creates and saves the complete General Wedding Invitation card (without individual guest pass)
     * as an image in cache, and returns its FileProvider Uri.
     */
    suspend fun createGeneralWeddingCardFileUri(
        context: Context,
        event: EventEntity,
        originalQuality: Boolean = true
    ): Uri = withContext(Dispatchers.IO) {
        val dummyGuest = GuestEntity(
            eventId = event.id,
            name = "",
            normalizedName = "",
            code = "",
            sequenceNumber = 0
        )
        val bitmap = renderInvitationBitmap(
            event = event.copy(showGuestName = false),
            guest = dummyGuest,
            originalQuality = originalQuality
        )
        val dir = File(context.cacheDir, "invitations").apply { mkdirs() }
        val ext = if (originalQuality) "png" else "jpg"
        val safeTitle = (if (event.groomName.isNotBlank()) "${event.groomName}_${event.brideName}" else event.name)
            .replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_").take(30)
        val file = File(dir, "wedding_card_${safeTitle}_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            if (originalQuality) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        bitmap.recycle()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Saves a rendered Bitmap directly into the Android user's device Gallery (Pictures/WeddingCards)
     * using MediaStore for instant access in Google Photos / Gallery apps.
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val safeTitle = title.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_").take(40).ifBlank { "wedding_card" }
            val filename = "${safeTitle}_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WeddingCards")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return@withContext uri
            }
            // Fallback: cache directory with FileProvider
            val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the pure card background image into the device gallery (before adding any text or barcodes).
     */
    suspend fun saveRawCardBackgroundToGallery(
        context: Context,
        event: EventEntity
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (event.backgroundType != "PRESET" && !event.backgroundUri.isNullOrBlank() && File(event.backgroundUri).exists()) {
                BitmapFactory.decodeFile(event.backgroundUri)
            } else {
                val width = 1200
                val height = (1200 / event.backgroundAspectRatio.coerceIn(0.4f, 2.0f)).roundToInt().coerceAtLeast(1600)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                drawLuxuryPresetBackgroundOnCanvas(c, width.toFloat(), height.toFloat(), event.backgroundPresetId)
                bmp
            } ?: return@withContext null
            val safeName = event.name.ifBlank { "wedding_card_background" }
            val uri = saveBitmapToGallery(context, bitmap, "bg_$safeName")
            bitmap.recycle()
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the complete rendered Wedding Invitation (with couple names, details and location barcode)
     * into the device gallery.
     */
    suspend fun saveGeneralWeddingCardToGallery(
        context: Context,
        event: EventEntity,
        originalQuality: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val dummyGuest = GuestEntity(
                eventId = event.id,
                name = "",
                normalizedName = "",
                code = "",
                sequenceNumber = 0
            )
            val bitmap = renderInvitationBitmap(
                event = event.copy(showGuestName = false),
                guest = dummyGuest,
                originalQuality = originalQuality
            )
            val safeName = if (event.groomName.isNotBlank() && event.brideName.isNotBlank()) {
                "${event.groomName}_${event.brideName}"
            } else {
                event.name.ifBlank { "wedding_invitation" }
            }
            val uri = saveBitmapToGallery(context, bitmap, safeName)
            bitmap.recycle()
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun shareGeneralWeddingCard(
        context: Context,
        event: EventEntity,
        originalQuality: Boolean = true
    ) {
        val imageUri = createGeneralWeddingCardFileUri(context, event, originalQuality)
        val titleText = if (event.groomName.isNotBlank() && event.brideName.isNotBlank()) {
            "دعوة زفاف ${event.groomName} & ${event.brideName}"
        } else {
            event.name
        }
        val details = buildString {
            append("🌹 ").append(titleText).append(" 🌹")
            if (event.dayName.isNotBlank() || event.date.isNotBlank()) {
                append("\n📅 ").append(event.dayName).append(" ").append(event.date)
            }
            if (event.timeFrom.isNotBlank()) {
                append("\n⏰ الساعة ").append(event.timeFrom)
                if (event.timeTo.isNotBlank()) append(" إلى ").append(event.timeTo)
            }
            if (event.venueName.isNotBlank()) {
                append("\n🏛️ ").append(event.venueName)
                if (event.venueAddress.isNotBlank()) append(" – ").append(event.venueAddress)
            }
            if (event.locationUrl.isNotBlank()) {
                append("\n📍 لوكيشن القاعة على الخريطة: ").append(event.locationUrl)
            }
            if (event.closingText.isNotBlank()) {
                append("\n✨ ").append(event.closingText)
            }
        }

        withContext(Dispatchers.Main) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (originalQuality) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, details)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "مشاركة دعوة الزفاف الرسمية")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Shares a guest's invitation via WhatsApp (direct to guest's number if available,
     * converting leading 0 to Egyptian +20, or opens share chooser if no number).
     */
    suspend fun dispatchGuestWhatsApp(
        context: Context,
        event: EventEntity,
        guest: GuestEntity,
        originalQuality: Boolean
    ) {
        val imageUri = createInvitationFileUri(context, event, guest, originalQuality)
        val message = EventRepository.formatWelcomeMessage(
            template = event.whatsappTemplate,
            guestName = guest.name,
            event = event
        )

        withContext(Dispatchers.Main) {
            val formattedPhone = if (guest.phone.isNotBlank()) {
                EventRepository.formatWhatsAppPhone(guest.phone)
            } else ""

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (originalQuality) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (formattedPhone.isNotEmpty()) {
                    // Direct WhatsApp conversation JID
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    setPackage("com.whatsapp")
                }
            }

            try {
                if (formattedPhone.isNotEmpty() && sendIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(sendIntent)
                } else {
                    // Fallback to system share sheet (works on emulators & devices without WhatsApp installed)
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (originalQuality) "image/png" else "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        putExtra(Intent.EXTRA_TEXT, message)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(fallbackIntent, guest.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(fallbackIntent, guest.name)
                )
            }
        }
    }

    /**
     * Shares a single invitation image via standard Share sheet (or saves/exports it).
     */
    suspend fun shareSingleInvitationImage(
        context: Context,
        event: EventEntity,
        guest: GuestEntity,
        originalQuality: Boolean
    ) {
        val uri = createInvitationFileUri(context, event, guest, originalQuality)
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (originalQuality) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${event.name} — ${guest.name} (#${guest.code})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, guest.name))
        }
    }

    /**
     * Shares selected invitations as images in batches of up to 12 per batch (Section 3.7).
     */
    suspend fun shareSelectedInvitationsBatch(
        context: Context,
        event: EventEntity,
        guests: List<GuestEntity>,
        originalQuality: Boolean
    ) {
        val batch = guests.take(12)
        if (batch.isEmpty()) return
        val uris = ArrayList<Uri>()
        withContext(Dispatchers.IO) {
            for (g in batch) {
                uris.add(createInvitationFileUri(context, event, g, originalQuality))
            }
        }
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (originalQuality) "image/png" else "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, event.name))
        }
    }

    /**
     * Generates a ZIP file (or multiple ZIP chunks if > 100 guests) containing all invitations
     * and opens the Android Share sheet to save/export the ZIP.
     */
    suspend fun exportAllAsZipAndShare(
        context: Context,
        event: EventEntity,
        guests: List<GuestEntity>,
        originalQuality: Boolean,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        if (guests.isEmpty()) return@withContext null
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeEventName = event.name.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_").take(25)
        val zipFile = File(exportsDir, "${safeEventName}_invitations.zip")
        val ext = if (originalQuality) "png" else "jpg"

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            guests.forEachIndexed { index, guest ->
                onProgress(index + 1, guests.size)
                val bmp = renderInvitationBitmap(event, guest, originalQuality)
                val safeName = guest.name.replace(Regex("[^\\p{L}\\p{N}_\\-]"), "_").take(30)
                val entryName = "${(index + 1).toString().padStart(3, '0')}_${safeName}_${guest.code}.$ext"
                zos.putNextEntry(ZipEntry(entryName))
                if (originalQuality) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, zos)
                } else {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, zos)
                }
                zos.closeEntry()
                bmp.recycle()
            }
        }

        val zipUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, zipFile.name))
        }
        zipFile
    }

    private fun parseColorInt(hex: String, fallback: Int): Int {
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }

    private fun colorToInt(color: androidx.compose.ui.graphics.Color): Int {
        return android.graphics.Color.argb(
            (color.alpha * 255).roundToInt(),
            (color.red * 255).roundToInt(),
            (color.green * 255).roundToInt(),
            (color.blue * 255).roundToInt()
        )
    }
}

/**
 * Real-time CameraX ImageAnalysis.Analyzer that decodes QR codes using ZXing MultiFormatReader.
 */
class QrFrameAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedTimestamp < 220L) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = now

        try {
            val plane = image.planes.firstOrNull()
            if (plane != null) {
                val buffer = plane.buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val decoded = QrCodec.decodeFromLuminance(bytes, image.width, image.height)
                if (!decoded.isNullOrBlank()) {
                    onQrDetected(decoded)
                }
            }
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }
}
