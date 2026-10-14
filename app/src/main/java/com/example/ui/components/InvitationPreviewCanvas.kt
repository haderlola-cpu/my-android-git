package com.example.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventEntity
import com.example.util.DesignCatalog
import com.example.util.InvitationRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Invitation Preview Canvas:
 * - Renders the 6 luxury preset backgrounds OR the uploaded image/GIF/video at its exact native aspect ratio
 *   without cropping.
 * - Generates a real scannable QR code (`eb:<eventId>:<code >`) with custom color.
 * - Supports finger drag-and-drop (`interactiveDrag = true`) for:
 *   1) QR Barcode (`onQrPosChanged`)
 *   2) Guest Name (`onNamePosChanged`)
 *   3) Date & Time (`onDatePosChanged`)
 */
@Composable
fun InvitationPreviewCanvas(
    event: EventEntity,
    guestName: String,
    guestCode: String,
    interactiveDrag: Boolean = false,
    hideQrCode: Boolean = false,
    showLocationQrOnly: Boolean = false,
    onQrPosChanged: ((Float, Float) -> Unit)? = null,
    onNamePosChanged: ((Float, Float) -> Unit)? = null,
    onDatePosChanged: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ratio = event.backgroundAspectRatio.coerceIn(0.42f, 1.9f)
    val fontChoice = DesignCatalog.fontChoices.getOrElse(event.fontIndex) { DesignCatalog.fontChoices.first() }
    val nameColor = DesignCatalog.parseColor(event.nameColorHex, Color(0xFFFDF6E2))
    val qrColor = DesignCatalog.parseColor(event.qrColorHex, Color(0xFF121826))
    val qrBgColor = DesignCatalog.parseColor(event.qrBgColorHex, Color.White)

    // Load custom image/video preview asynchronously if backgroundType != PRESET
    val customBgBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = event.backgroundType,
        key2 = event.backgroundUri
    ) {
        value = withContext(Dispatchers.IO) {
            val uriPath = event.backgroundUri
            if (event.backgroundType == "PRESET" || uriPath.isNullOrBlank()) {
                null
            } else {
                try {
                    val file = File(uriPath)
                    if (!file.exists()) {
                        null
                    } else if (event.backgroundType == "VIDEO") {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(file.absolutePath)
                            retriever.getFrameAtTime(0)?.asImageBitmap()
                        } finally {
                            retriever.release()
                        }
                    } else {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    // Determine whether this preview shows the Location QR code or the Guest Pass QR code
    val isLocationQrMode = showLocationQrOnly || (guestCode.isBlank() && event.showLocationQr && event.locationUrl.isNotBlank())
    val qrPayload = if (isLocationQrMode && event.locationUrl.isNotBlank()) {
        event.locationUrl
    } else {
        "eb:${if (event.id == 0L) 1L else event.id}:${guestCode.ifBlank { "48291037" }}"
    }

    val qrImageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        qrPayload,
        event.qrColorHex,
        event.qrBgColorHex,
        hideQrCode
    ) {
        if (hideQrCode) {
            value = null
        } else {
            value = withContext(Dispatchers.Default) {
                val fgInt = try {
                    android.graphics.Color.parseColor(event.qrColorHex)
                } catch (_: Exception) {
                    android.graphics.Color.BLACK
                }
                val bgInt = try {
                    android.graphics.Color.parseColor(event.qrBgColorHex)
                } catch (_: Exception) {
                    android.graphics.Color.WHITE
                }
                InvitationRenderer.generateQrBitmap(
                    content = qrPayload,
                    sizePx = 320,
                    fgColorInt = fgInt,
                    bgColorInt = bgInt
                ).asImageBitmap()
            }
        }
    }

    var activeDraggedElement by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(18.dp))
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
    ) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val boxHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val scaleFactor = (maxWidth.value / 340f).coerceIn(0.65f, 1.8f)

        // 1. Background Layer (Supports live Video playback, animated GIF, custom Image, or 6 Luxury Presets)
        val hasCustomFile = event.backgroundType != "PRESET" && !event.backgroundUri.isNullOrBlank() &&
            remember(event.backgroundUri) { File(event.backgroundUri).exists() }

        if (hasCustomFile && event.backgroundType == "VIDEO") {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoPath(event.backgroundUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            start()
                        }
                    }
                },
                update = { videoView ->
                    if (!videoView.isPlaying) {
                        videoView.start()
                    }
                }
            )
        } else if (hasCustomFile && event.backgroundType == "GIF" && android.os.Build.VERSION.SDK_INT >= 28) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_XY
                        try {
                            val source = android.graphics.ImageDecoder.createSource(File(event.backgroundUri!!))
                            val drawable = android.graphics.ImageDecoder.decodeDrawable(source)
                            setImageDrawable(drawable)
                            if (drawable is android.graphics.drawable.AnimatedImageDrawable) {
                                drawable.start()
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            )
        } else if (customBgBitmap != null) {
            Image(
                bitmap = customBgBitmap!!,
                contentDescription = event.name,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val preset = DesignCatalog.presetBackgrounds.getOrElse(event.backgroundPresetId) {
                DesignCatalog.presetBackgrounds.first()
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(preset.topColor, preset.centerColor, preset.bottomColor)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(preset.accentColor.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = (w * 0.65f).coerceAtLeast(1f)
                    )
                )
                val m1 = w * 0.055f
                val m2 = w * 0.075f
                drawRoundRect(
                    color = preset.accentColor,
                    topLeft = Offset(m1, m1),
                    size = Size(w - m1 * 2, h - m1 * 2),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = Stroke(width = (w * 0.005f).coerceAtLeast(2f))
                )
                drawRoundRect(
                    color = preset.secondaryAccentColor.copy(alpha = 0.65f),
                    topLeft = Offset(m2, m2),
                    size = Size(w - m2 * 2, h - m2 * 2),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
                    style = Stroke(width = (w * 0.0025f).coerceAtLeast(1f))
                )

                // Top 8-point star ornament
                val starPath = Path()
                val cx = w * 0.5f
                val cy = h * 0.085f
                val rOut = w * 0.032f
                val rIn = rOut * 0.45f
                for (i in 0 until 16) {
                    val r = if (i % 2 == 0) rOut else rIn
                    val angle = Math.toRadians((i * 360.0 / 16.0) - 90.0)
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                drawPath(starPath, color = preset.accentColor)
            }
        }

        // Animated badge indicator if GIF or VIDEO
        if (event.backgroundType == "GIF" || event.backgroundType == "VIDEO") {
            Text(
                text = if (event.backgroundType == "GIF") "GIF 🎞️" else "VIDEO 🎬",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        val isWedding = event.groomName.isNotBlank() || event.brideName.isNotBlank() || event.venueName.isNotBlank()

        // 1. Event Title Header
        Text(
            text = event.name.ifBlank { if (isWedding) "دعوة زفاف" else "elbeit elaraby" },
            color = nameColor,
            fontFamily = fontChoice.composeFontFamily,
            fontWeight = fontChoice.fontWeight,
            fontStyle = fontChoice.fontStyle,
            fontSize = (17f * scaleFactor).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (maxHeight * 0.08f), start = 18.dp, end = 18.dp)
        )

        // Quranic Verse or Opening Intro if Wedding Card
        if (isWedding && event.headerVerse.isNotBlank()) {
            Text(
                text = event.headerVerse,
                color = nameColor.copy(alpha = 0.88f),
                fontFamily = fontChoice.composeFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (11f * scaleFactor).sp,
                textAlign = TextAlign.Center,
                lineHeight = (15f * scaleFactor).sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (maxHeight * 0.16f), start = 20.dp, end = 20.dp)
            )
        }

        // Centerpiece Couple Names (Groom & Bride) if Wedding Card
        if (isWedding && (event.groomName.isNotBlank() || event.brideName.isNotBlank())) {
            val coupleText = if (event.groomName.isNotBlank() && event.brideName.isNotBlank()) {
                "${event.groomName}  ♡  ${event.brideName}"
            } else {
                event.groomName.ifBlank { event.brideName }
            }
            Text(
                text = coupleText,
                color = nameColor,
                fontFamily = fontChoice.composeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (20f * scaleFactor).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (maxHeight * 0.25f), start = 16.dp, end = 16.dp)
            )
        }

        // Venue Name if Wedding Card
        if (isWedding && event.venueName.isNotBlank()) {
            Text(
                text = "🏛️ ${event.venueName}${if (event.venueAddress.isNotBlank()) " – " + event.venueAddress else ""}",
                color = nameColor.copy(alpha = 0.92f),
                fontFamily = fontChoice.composeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (12f * scaleFactor).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (maxHeight * 0.33f), start = 16.dp, end = 16.dp)
            )
        }

        // 2. Draggable QR Code Element (Hidden if hideQrCode is true)
        if (!hideQrCode) {
            val qrSizeDp = maxWidth * event.qrSizeRatio.coerceIn(0.15f, 0.45f)
            val qrSizePx = with(density) { qrSizeDp.toPx() }
            val currentQrPos by rememberUpdatedState(event.qrPosX to event.qrPosY)
            val currentOnQrPosChanged by rememberUpdatedState(onQrPosChanged)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((event.qrPosX * boxWidthPx) - (qrSizePx * 0.58f)).roundToInt(),
                            y = ((event.qrPosY * boxHeightPx) - (qrSizePx * 0.62f)).roundToInt()
                        )
                    }
                    .then(
                        if (interactiveDrag && onQrPosChanged != null) {
                            Modifier.pointerInput(boxWidthPx, boxHeightPx) {
                                detectDragGestures(
                                    onDragStart = { activeDraggedElement = "QR" },
                                    onDragEnd = { activeDraggedElement = null },
                                    onDragCancel = { activeDraggedElement = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val (curX, curY) = currentQrPos
                                        val nextX = (curX + dragAmount.x / boxWidthPx).coerceIn(0.12f, 0.88f)
                                        val nextY = (curY + dragAmount.y / boxHeightPx).coerceIn(0.12f, 0.88f)
                                        currentOnQrPosChanged?.invoke(nextX, nextY)
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(qrBgColor)
                    .border(
                        width = if (activeDraggedElement == "QR") 2.5.dp else 1.5.dp,
                        color = if (activeDraggedElement == "QR") Color(0xFF10B981) else qrColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val currentQrBitmap = qrImageBitmap
                    if (currentQrBitmap != null) {
                        Image(
                            bitmap = currentQrBitmap,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(qrSizeDp)
                        )
                    }
                    val qrBottomLabel = if (isLocationQrMode) {
                        "📍 لوكيشن القاعة"
                    } else {
                        "#${guestCode.ifBlank { "48291037" }}"
                    }
                    Text(
                        text = qrBottomLabel,
                        color = qrColor,
                        fontFamily = if (isLocationQrMode) fontChoice.composeFontFamily else FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10f * scaleFactor).coerceAtLeast(8.5f).sp
                    )
                }
            }
        }

        // 3. Draggable Guest Name Element (in standard mode or if showGuestName enabled)
        if (event.showGuestName && !isWedding) {
            val currentNamePos by rememberUpdatedState(event.namePosX to event.namePosY)
            val currentOnNamePosChanged by rememberUpdatedState(onNamePosChanged)
            val nameWidthDp = maxWidth * 0.84f
            val nameWidthPx = with(density) { nameWidthDp.toPx() }

            Box(
                modifier = Modifier
                    .size(width = nameWidthDp, height = (46 * scaleFactor).dp)
                    .offset {
                        IntOffset(
                            x = ((event.namePosX * boxWidthPx) - (nameWidthPx / 2f)).roundToInt(),
                            y = ((event.namePosY * boxHeightPx) - (22f * scaleFactor)).roundToInt()
                        )
                    }
                    .then(
                        if (interactiveDrag && onNamePosChanged != null) {
                            Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (activeDraggedElement == "NAME") Color(0xFF10B981) else nameColor.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .pointerInput(boxWidthPx, boxHeightPx) {
                                    detectDragGestures(
                                        onDragStart = { activeDraggedElement = "NAME" },
                                        onDragEnd = { activeDraggedElement = null },
                                        onDragCancel = { activeDraggedElement = null },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val (curX, curY) = currentNamePos
                                            val nextX = (curX + dragAmount.x / boxWidthPx).coerceIn(0.15f, 0.85f)
                                            val nextY = (curY + dragAmount.y / boxHeightPx).coerceIn(0.08f, 0.94f)
                                            currentOnNamePosChanged?.invoke(nextX, nextY)
                                        }
                                    )
                                }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = guestName,
                    color = nameColor,
                    fontFamily = fontChoice.composeFontFamily,
                    fontWeight = fontChoice.fontWeight,
                    fontStyle = fontChoice.fontStyle,
                    fontSize = (event.nameSizeSp * scaleFactor * 0.82f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        // 4. Draggable Date & Time Element
        if (event.showDate || event.showTime) {
            val currentDatePos by rememberUpdatedState(event.datePosX to event.datePosY)
            val currentOnDatePosChanged by rememberUpdatedState(onDatePosChanged)
            val dateWidthDp = maxWidth * 0.84f
            val dateWidthPx = with(density) { dateWidthDp.toPx() }

            val dateTimeText = buildString {
                if (event.showDate && (event.date.isNotBlank() || (isWedding && event.dayName.isNotBlank()))) {
                    if (isWedding && event.dayName.isNotBlank()) append(event.dayName).append(" ")
                    if (event.date.isNotBlank()) append(event.date)
                }
                if (event.showTime && (event.timeFrom.isNotBlank() || event.timeTo.isNotBlank())) {
                    if (isNotEmpty()) append("\n")
                    append(event.timeFrom)
                    if (event.timeTo.isNotBlank()) {
                        append(" – ")
                        append(event.timeTo)
                    }
                }
            }

            if (dateTimeText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(width = dateWidthDp, height = (48 * scaleFactor).dp)
                        .offset {
                            IntOffset(
                                x = ((event.datePosX * boxWidthPx) - (dateWidthPx / 2f)).roundToInt(),
                                y = ((event.datePosY * boxHeightPx) - (24f * scaleFactor)).roundToInt()
                            )
                        }
                        .then(
                            if (interactiveDrag && onDatePosChanged != null) {
                                Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (activeDraggedElement == "DATE") Color(0xFF10B981) else nameColor.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .pointerInput(boxWidthPx, boxHeightPx) {
                                        detectDragGestures(
                                            onDragStart = { activeDraggedElement = "DATE" },
                                            onDragEnd = { activeDraggedElement = null },
                                            onDragCancel = { activeDraggedElement = null },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val (curX, curY) = currentDatePos
                                                val nextX = (curX + dragAmount.x / boxWidthPx).coerceIn(0.15f, 0.85f)
                                                val nextY = (curY + dragAmount.y / boxHeightPx).coerceIn(0.08f, 0.94f)
                                                currentOnDatePosChanged?.invoke(nextX, nextY)
                                            }
                                        )
                                    }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dateTimeText,
                        color = nameColor.copy(alpha = 0.92f),
                        fontFamily = fontChoice.composeFontFamily,
                        fontWeight = fontChoice.fontWeight,
                        fontSize = (12.5f * scaleFactor).sp,
                        lineHeight = (16f * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
