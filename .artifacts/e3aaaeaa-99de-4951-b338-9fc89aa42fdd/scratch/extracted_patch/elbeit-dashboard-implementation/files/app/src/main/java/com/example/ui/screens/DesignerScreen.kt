package com.example.ui.screens

import com.example.ui.components.LuxCard
import com.example.ui.components.goldBottomLine
import com.example.ui.components.goldCorners
import com.example.ui.components.arabesqueLattice
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EventEntity
import com.example.ui.components.InvitationPreviewCanvas
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.EmeraldAttendedContainer
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.AiImageResult
import com.example.util.DesignCatalog
import com.example.util.GeminiImageGenerator
import com.example.util.InvitationRenderer
import com.example.util.LocalAppStrings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignerScreen(
    existingEvent: EventEntity?,
    onSaveEvent: (EventEntity) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Step state: 0 = Generate & Pick Card, 1 = Wedding Details & Location QR, 2 = Barcode, Style & Export
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    // Core Event Details
    var name by rememberSaveable { mutableStateOf(existingEvent?.name ?: "") }
    var date by rememberSaveable { mutableStateOf(existingEvent?.date ?: "") }
    var timeFrom by rememberSaveable { mutableStateOf(existingEvent?.timeFrom ?: "") }
    var timeTo by rememberSaveable { mutableStateOf(existingEvent?.timeTo ?: "") }

    // Wedding Couple & Details
    var groomName by rememberSaveable { mutableStateOf(existingEvent?.groomName ?: "") }
    var brideName by rememberSaveable { mutableStateOf(existingEvent?.brideName ?: "") }
    var headerVerse by rememberSaveable { mutableStateOf(existingEvent?.headerVerse ?: "") }
    var hostsText by rememberSaveable { mutableStateOf(existingEvent?.hostsText ?: "") }
    var dayName by rememberSaveable { mutableStateOf(existingEvent?.dayName ?: "") }
    var venueName by rememberSaveable { mutableStateOf(existingEvent?.venueName ?: "") }
    var venueAddress by rememberSaveable { mutableStateOf(existingEvent?.venueAddress ?: "") }
    var locationUrl by rememberSaveable { mutableStateOf(existingEvent?.locationUrl ?: "") }
    var showLocationQr by rememberSaveable { mutableStateOf(existingEvent?.showLocationQr ?: true) }
    var notesText by rememberSaveable { mutableStateOf(existingEvent?.notesText ?: "") }
    var closingText by rememberSaveable { mutableStateOf(existingEvent?.closingText ?: "") }

    // Background & Media
    var backgroundType by rememberSaveable { mutableStateOf(existingEvent?.backgroundType ?: "PRESET") }
    var backgroundPresetId by rememberSaveable { mutableIntStateOf(existingEvent?.backgroundPresetId ?: 0) }
    var backgroundUri by rememberSaveable { mutableStateOf(existingEvent?.backgroundUri) }
    var backgroundAspectRatio by rememberSaveable { mutableFloatStateOf(existingEvent?.backgroundAspectRatio ?: 0.714f) }

    // Colors & Typography
    var nameColorHex by rememberSaveable { mutableStateOf(existingEvent?.nameColorHex ?: "#FFFDF6E2") }
    var qrColorHex by rememberSaveable { mutableStateOf(existingEvent?.qrColorHex ?: "#FF121826") }
    var qrBgColorHex by rememberSaveable { mutableStateOf(existingEvent?.qrBgColorHex ?: "#FFFFFFFF") }
    var fontIndex by rememberSaveable { mutableIntStateOf(existingEvent?.fontIndex ?: 0) }

    // Visibility & Sizing
    var showGuestName by rememberSaveable { mutableStateOf(existingEvent?.showGuestName ?: true) }
    var showDate by rememberSaveable { mutableStateOf(existingEvent?.showDate ?: true) }
    var showTime by rememberSaveable { mutableStateOf(existingEvent?.showTime ?: true) }
    var qrSizeRatio by rememberSaveable { mutableFloatStateOf(existingEvent?.qrSizeRatio ?: 0.28f) }
    var nameSizeSp by rememberSaveable { mutableFloatStateOf(existingEvent?.nameSizeSp ?: 22f) }

    // Coordinates
    var qrPosX by rememberSaveable { mutableFloatStateOf(existingEvent?.qrPosX ?: 0.50f) }
    var qrPosY by rememberSaveable { mutableFloatStateOf(existingEvent?.qrPosY ?: 0.50f) }
    var namePosX by rememberSaveable { mutableFloatStateOf(existingEvent?.namePosX ?: 0.50f) }
    var namePosY by rememberSaveable { mutableFloatStateOf(existingEvent?.namePosY ?: 0.76f) }
    var datePosX by rememberSaveable { mutableFloatStateOf(existingEvent?.datePosX ?: 0.50f) }
    var datePosY by rememberSaveable { mutableFloatStateOf(existingEvent?.datePosY ?: 0.88f) }

    // UI Feedback & Dialogs
    var showFullScreenAdjust by remember { mutableStateOf(false) }
    var customColorTarget by remember { mutableStateOf<String?>(null) }
    var aiPrompt by rememberSaveable { mutableStateOf("") }
    var isGeneratingAiImage by remember { mutableStateOf(false) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }
    var previewBarcodeType by rememberSaveable { mutableStateOf("LOCATION") } // "LOCATION" or "GUEST"
    var downloadStatusBanner by remember { mutableStateOf<String?>(null) }

    // Sync state if existingEvent loads asynchronously
    LaunchedEffect(existingEvent?.id) {
        if (existingEvent != null) {
            name = existingEvent.name
            date = existingEvent.date
            timeFrom = existingEvent.timeFrom
            timeTo = existingEvent.timeTo
            groomName = existingEvent.groomName
            brideName = existingEvent.brideName
            headerVerse = existingEvent.headerVerse
            hostsText = existingEvent.hostsText
            dayName = existingEvent.dayName
            venueName = existingEvent.venueName
            venueAddress = existingEvent.venueAddress
            locationUrl = existingEvent.locationUrl
            showLocationQr = existingEvent.showLocationQr
            notesText = existingEvent.notesText
            closingText = existingEvent.closingText
            backgroundType = existingEvent.backgroundType
            backgroundPresetId = existingEvent.backgroundPresetId
            backgroundUri = existingEvent.backgroundUri
            backgroundAspectRatio = existingEvent.backgroundAspectRatio
            nameColorHex = existingEvent.nameColorHex
            qrColorHex = existingEvent.qrColorHex
            qrBgColorHex = existingEvent.qrBgColorHex
            fontIndex = existingEvent.fontIndex
            showGuestName = existingEvent.showGuestName
            showDate = existingEvent.showDate
            showTime = existingEvent.showTime
            qrSizeRatio = existingEvent.qrSizeRatio
            nameSizeSp = existingEvent.nameSizeSp
            qrPosX = existingEvent.qrPosX
            qrPosY = existingEvent.qrPosY
            namePosX = existingEvent.namePosX
            namePosY = existingEvent.namePosY
            datePosX = existingEvent.datePosX
            datePosY = existingEvent.datePosY
        }
    }

    // Zero-permission Android Photo Picker for Image/GIF
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val info = InvitationRenderer.copyPickedMediaAndGetInfo(context, uri, requestedVideo = false)
                if (info != null) {
                    backgroundType = info.mediaType
                    backgroundUri = info.localPath
                    backgroundAspectRatio = info.aspectRatio
                }
            }
        }
    }

    // Zero-permission Android Photo Picker for Video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val info = InvitationRenderer.copyPickedMediaAndGetInfo(context, uri, requestedVideo = true)
                if (info != null) {
                    backgroundType = "VIDEO"
                    backgroundUri = info.localPath
                    backgroundAspectRatio = info.aspectRatio
                }
            }
        }
    }

    val draftEvent = EventEntity(
        id = existingEvent?.id ?: 0L,
        name = name.ifBlank {
            if (groomName.isNotBlank() && brideName.isNotBlank()) "حفل زفاف $groomName & $brideName" else strings.appName
        },
        date = date,
        timeFrom = timeFrom,
        timeTo = timeTo,
        backgroundType = backgroundType,
        backgroundPresetId = backgroundPresetId,
        backgroundUri = backgroundUri,
        backgroundAspectRatio = backgroundAspectRatio,
        nameColorHex = nameColorHex,
        qrColorHex = qrColorHex,
        qrBgColorHex = qrBgColorHex,
        fontIndex = fontIndex,
        showGuestName = showGuestName,
        showDate = showDate,
        showTime = showTime,
        qrSizeRatio = qrSizeRatio,
        nameSizeSp = nameSizeSp,
        qrPosX = qrPosX,
        qrPosY = qrPosY,
        namePosX = namePosX,
        namePosY = namePosY,
        datePosX = datePosX,
        datePosY = datePosY,
        whatsappTemplate = existingEvent?.whatsappTemplate ?: EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR,
        createdAt = existingEvent?.createdAt ?: System.currentTimeMillis(),
        groomName = groomName,
        brideName = brideName,
        headerVerse = headerVerse,
        hostsText = hostsText,
        dayName = dayName,
        venueName = venueName,
        venueAddress = venueAddress,
        locationUrl = locationUrl,
        showLocationQr = showLocationQr,
        notesText = notesText,
        closingText = closingText
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Text(
                        text = if (existingEvent == null) {
                            if (strings.isArabic) "استوديو توليد وتصميم دعوات الأفراح" else "Wedding Studio"
                        } else {
                            strings.editDesignTitle
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGoldLight
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("designer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.cancel
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onSaveEvent(draftEvent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("designer_save_top_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .arabesqueLattice()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 3-STEP WIZARD NAVIGATION BAR (مراحل تصميم وتوليد الدعوة) ---
            LuxCard(
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StepTabItem(
                        stepNumber = "1",
                        title = if (strings.isArabic) "توليد وتصميم الدعوة" else "Generate Card",
                        isSelected = currentStep == 0,
                        isCompleted = currentStep > 0,
                        onClick = { currentStep = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    StepTabItem(
                        stepNumber = "2",
                        title = if (strings.isArabic) "بيانات الفرح واللوكيشن" else "Wedding Details",
                        isSelected = currentStep == 1,
                        isCompleted = currentStep > 1,
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    StepTabItem(
                        stepNumber = "3",
                        title = if (strings.isArabic) "الباركود والخطوط" else "Barcode & Style",
                        isSelected = currentStep == 2,
                        isCompleted = false,
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Global Download & Action Notification Banner
            if (!downloadStatusBanner.isNullOrBlank()) {
                LuxCard(
                    cornerRadius = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldAttendedContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = downloadStatusBanner!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { downloadStatusBanner = null }) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                        }
                    }
                }
            }

            // =========================================================================
            // STEP 0: توليد وتصميم الدعوة (GENERATE / SELECT INVITATION BACKGROUND)
            // =========================================================================
            if (currentStep == 0) {
                // Interactive Preview Card for Step 0 (Pure Invitation Background)
                LuxCard(
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (strings.isArabic) {
                                "معاينة تصميم الدعوة (بدون باركود) • يمكنك تحميلها فوراً"
                            } else {
                                "Invitation Design Preview (Clean Card) • Download Ready"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGoldLight,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.widthIn(max = 310.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            InvitationPreviewCanvas(
                                event = draftEvent,
                                guestName = strings.sampleGuestName,
                                guestCode = "48291037",
                                interactiveDrag = false,
                                hideQrCode = true, // Hide barcode in Step 0 for clean download
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // FEATURE REQUEST: Option to download the card image directly BEFORE barcode generation!
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = InvitationRenderer.saveRawCardBackgroundToGallery(context, draftEvent)
                                    if (uri != null) {
                                        downloadStatusBanner = if (strings.isArabic) {
                                            "✅ تم تنزيل صورة الدعوة بدون باركود إلى المعرض بنجاح! 📥"
                                        } else {
                                            "✅ Invitation card image saved to Gallery! 📥"
                                        }
                                        Toast.makeText(
                                            context,
                                            if (strings.isArabic) "تم حفظ صورة الدعوة في المعرض 📥" else "Saved to Gallery 📥",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianNight
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("download_card_before_barcode_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "📥 تنزيل صورة الدعوة (قبل إدخال الباركود)"
                                } else {
                                    "Download Card Image (Before Barcode)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Luxury Ready-Made Wedding Card Templates (Instant & Offline)
                DesignerSectionCard(
                    title = if (strings.isArabic) "قوالب دعوات أفراح فاخرة جاهزة (تعمل فوراً)" else "Luxury Ready-Made Wedding Templates"
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(GeminiImageGenerator.bundledWeddingTemplates, key = { it.id }) { template ->
                            val isSelected = backgroundType == "IMAGE" &&
                                backgroundUri?.endsWith("wedding_template_${template.id}.jpg") == true
                            Box(
                                modifier = Modifier
                                    .size(width = 120.dp, height = 152.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) RoyalGold else RoyalGold.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            val info = GeminiImageGenerator.applyBundledWeddingTemplate(
                                                context = context,
                                                template = template
                                            )
                                            if (info != null) {
                                                backgroundType = info.mediaType
                                                backgroundUri = info.localPath
                                                backgroundAspectRatio = info.aspectRatio
                                                nameColorHex = template.recommendedNameColorHex
                                                qrColorHex = template.recommendedQrColorHex
                                            }
                                        }
                                    }
                                    .testTag("wedding_template_${template.id}")
                            ) {
                                Image(
                                    painter = painterResource(id = template.drawableRes),
                                    contentDescription = if (strings.isArabic) template.titleAr else template.titleEn,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(ObsidianNight.copy(alpha = 0.85f))
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (strings.isArabic) template.titleAr else template.titleEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoyalGoldLight,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.uploadPhotoOrGifBtn, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.uploadVideoBtn, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Wedding Invitation Generator & Editor (Gemini Nano Banana 2.1)
                LuxCard(
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .border(
                            width = 1.dp,
                            color = RoyalGold.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianNight.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "توليد وتعديل دعوات الأفراح بالذكاء الاصطناعي (Nano Banana 2.1)"
                                } else {
                                    "AI Wedding Invitation Creator & Editor"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = RoyalGoldLight
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val suggestions = if (strings.isArabic) {
                            listOf(
                                "دعوة فرح ملكية بالذهبي والكحلي مع زهور وإطار زخرفي",
                                "بطاقة زفاف رخام أبيض عاجي مع ورود بيضاء وأوراق ذهبية",
                                "تصميم فرح شامبين وروز جولد مع ثريا كريستال مضيئة",
                                "أضف زهور ذهبية وإضاءة دافئة حول إطار الدعوة الحالية"
                            )
                        } else {
                            listOf(
                                "Royal navy & 24k gold wedding card with floral arch frame",
                                "Ivory marble wedding invitation with white roses & gold border",
                                "Champagne & blush rose-gold palace wedding card with sparkles",
                                "Add golden floral corners and warm chandelier bokeh to this card"
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestions.forEach { idea ->
                                FilterChip(
                                    selected = aiPrompt == idea,
                                    onClick = { aiPrompt = idea },
                                    label = {
                                        Text(
                                            text = idea,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = {
                                aiPrompt = it
                                aiStatusMessage = null
                            },
                            label = {
                                Text(
                                    if (strings.isArabic) {
                                        "اكتب وصف تصميم دعوة الفرح أو التعديل المطلوب..."
                                    } else {
                                        "Describe wedding invitation design or prompt..."
                                    }
                                )
                            },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_wedding_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isGeneratingAiImage) {
                                        isGeneratingAiImage = true
                                        aiStatusMessage = null
                                        coroutineScope.launch {
                                            when (
                                                val result = GeminiImageGenerator.generateOrEditWeddingInvitation(
                                                    context = context,
                                                    userPrompt = aiPrompt,
                                                    editExistingEvent = null
                                                )
                                            ) {
                                                is AiImageResult.Success -> {
                                                    backgroundType = "IMAGE"
                                                    backgroundUri = result.localPath
                                                    backgroundAspectRatio = result.aspectRatio
                                                    result.recommendedNameColorHex?.let { nameColorHex = it }
                                                    result.recommendedQrColorHex?.let { qrColorHex = it }
                                                    aiStatusMessage = if (strings.isArabic) {
                                                        "✅ تم توليد تصميم دعوة الفرح وتطبيقه بنجاح!"
                                                    } else {
                                                        "✅ Wedding invitation design generated!"
                                                    }
                                                }
                                                is AiImageResult.Error -> {
                                                    aiStatusMessage = if (strings.isArabic) result.messageAr else result.messageEn
                                                }
                                            }
                                            isGeneratingAiImage = false
                                        }
                                    }
                                },
                                enabled = !isGeneratingAiImage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalGold,
                                    contentColor = ObsidianNight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_generate_wedding_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (strings.isArabic) "توليد دعوة جديدة" else "Create New",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isGeneratingAiImage) {
                                        isGeneratingAiImage = true
                                        aiStatusMessage = null
                                        coroutineScope.launch {
                                            when (
                                                val result = GeminiImageGenerator.generateOrEditWeddingInvitation(
                                                    context = context,
                                                    userPrompt = aiPrompt,
                                                    editExistingEvent = draftEvent
                                                )
                                            ) {
                                                is AiImageResult.Success -> {
                                                    backgroundType = "IMAGE"
                                                    backgroundUri = result.localPath
                                                    backgroundAspectRatio = result.aspectRatio
                                                    result.recommendedNameColorHex?.let { nameColorHex = it }
                                                    result.recommendedQrColorHex?.let { qrColorHex = it }
                                                    aiStatusMessage = if (strings.isArabic) {
                                                        "✅ تم تعديل تصميم الدعوة وتطبيقه بنجاح!"
                                                    } else {
                                                        "✅ Background edited and applied!"
                                                    }
                                                }
                                                is AiImageResult.Error -> {
                                                    aiStatusMessage = if (strings.isArabic) result.messageAr else result.messageEn
                                                }
                                            }
                                            isGeneratingAiImage = false
                                        }
                                    }
                                },
                                enabled = !isGeneratingAiImage,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_edit_wedding_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (strings.isArabic) "تعديل الخلفية الحالية" else "Edit Current",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RoyalGoldLight
                                )
                            }
                        }

                        if (isGeneratingAiImage) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    color = RoyalGold,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (strings.isArabic) {
                                        "جارٍ تصميم دعوة الفرح بالذكاء الاصطناعي..."
                                    } else {
                                        "Designing wedding invitation with AI..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoyalGoldLight
                                )
                            }
                        }

                        if (!aiStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (aiStatusMessage!!.startsWith("✅")) RoyalGoldLight else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Classic Luxury Presets
                DesignerSectionCard(title = strings.presetBackgroundsTab) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DesignCatalog.presetBackgrounds, key = { it.id }) { preset ->
                            val isSelected = backgroundType == "PRESET" && backgroundPresetId == preset.id
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 74.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(preset.topColor, preset.centerColor, preset.bottomColor)
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) RoyalGold else preset.accentColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        backgroundType = "PRESET"
                                        backgroundPresetId = preset.id
                                        backgroundUri = null
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (strings.isArabic) preset.titleAr else preset.titleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = preset.accentColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Next Step Button: Move to Wedding Details
                Button(
                    onClick = { currentStep = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianNight
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(54.dp)
                        .testTag("step0_next_button")
                ) {
                    Text(
                        text = if (strings.isArabic) {
                            "التالي: إضافة بيانات الفرح والأسماء واللوكيشن ➔"
                        } else {
                            "Next: Wedding Details & Location ➔"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // =========================================================================
            // STEP 1: بيانات الفرح واللوكيشن (WEDDING DETAILS & LOCATION BARCODE)
            // =========================================================================
            if (currentStep == 1) {
                // Interactive Preview with Live Wedding Details & Location Barcode
                LuxCard(
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (strings.isArabic) {
                                "معاينة الدعوة بتفاصيل الفرح والأسماء وباركود اللوكيشن"
                            } else {
                                "Live Preview with Wedding Details & Location Barcode"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGoldLight,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.widthIn(max = 310.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            InvitationPreviewCanvas(
                                event = draftEvent,
                                guestName = strings.sampleGuestName,
                                guestCode = "", // Blank guest code triggers Location QR preview
                                interactiveDrag = false,
                                hideQrCode = !showLocationQr || locationUrl.isBlank(),
                                showLocationQrOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Download Complete Wedding Card (with Details & Location Barcode)
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = InvitationRenderer.saveGeneralWeddingCardToGallery(context, draftEvent)
                                    if (uri != null) {
                                        downloadStatusBanner = if (strings.isArabic) {
                                            "✅ تم حفظ بطاقة الزفاف باللوكيشن والتفاصيل في المعرض! 📥"
                                        } else {
                                            "✅ Wedding invitation card saved to Gallery! 📥"
                                        }
                                        Toast.makeText(
                                            context,
                                            if (strings.isArabic) "تم حفظ الدعوة في المعرض 📥" else "Saved to Gallery 📥",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_wedding_card_details_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoyalGoldLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "📥 تنزيل الدعوة ببيانات الفرح واللوكيشن (حفظ في المعرض)"
                                } else {
                                    "Download Wedding Card with Location Barcode"
                                },
                                fontWeight = FontWeight.Bold,
                                color = RoyalGoldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Couple Names & Card Title (اسم العريس والعروسة وعنوان الدعوة)
                DesignerSectionCard(
                    title = if (strings.isArabic) "أسماء العروسين وعنوان الدعوة" else "Couple Names & Title"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = groomName,
                            onValueChange = { groomName = it },
                            label = { Text(if (strings.isArabic) "اسم العريس" else "Groom's Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RoyalGold) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("groom_name_input")
                        )
                        OutlinedTextField(
                            value = brideName,
                            onValueChange = { brideName = it },
                            label = { Text(if (strings.isArabic) "اسم العروسة" else "Bride's Name") },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = RoyalGold) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bride_name_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (strings.isArabic) "عنوان الدعوة / اسم المناسبة" else "Event Title") },
                        placeholder = {
                            Text(if (strings.isArabic) "حفل زفاف المهندس كريم والأستاذة سارة" else "Royal Wedding Celebration")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("designer_event_name_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Quranic Verse / Opening Phrase & Hosts
                DesignerSectionCard(
                    title = if (strings.isArabic) "الآية الكريمة ومقدمة الدعوة" else "Opening Verse & Hosts"
                ) {
                    val quickVerses = if (strings.isArabic) {
                        listOf(
                            "﴿وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُم مِّنْ أَنفُسِكُمْ أَزْوَاجًا لِّتَسْكُنُوا إِلَيْهَا وَجَعَلَ بَيْنَكُم مَّوَدَّةً وَرَحْمَةً﴾",
                            "﴿بَارَكَ اللَّهُ لَكُمَا وَبَارَكَ عَلَيْكُمَا وَجَمَعَ بَيْنَكُمَا فِي خَيْرٍ﴾",
                            "في ليلة تملؤها البهجة والسرور، يشرفنا أن تشاركونا فرحتنا الكبرى"
                        )
                    } else {
                        listOf(
                            "Together with their families, invite you to celebrate their wedding",
                            "In the joy of love and celebration, we request the honor of your presence",
                            "Two lives, two hearts, joined together in friendship, united forever in love"
                        )
                    }

                    Text(
                        text = if (strings.isArabic) "نماذج عبارات سريعة بنقرة واحدة:" else "Quick Suggestions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickVerses.forEach { v ->
                            FilterChip(
                                selected = headerVerse == v,
                                onClick = { headerVerse = v },
                                label = {
                                    Text(
                                        text = v.take(34) + "...",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = headerVerse,
                        onValueChange = { headerVerse = it },
                        label = { Text(if (strings.isArabic) "آية قرآنية أو عبارة افتتاحية" else "Quranic Verse / Opening Verse") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("header_verse_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hostsText,
                        onValueChange = { hostsText = it },
                        label = { Text(if (strings.isArabic) "الداعون (أهل العروسين)" else "Hosts / Families") },
                        placeholder = {
                            Text(if (strings.isArabic) "عائلتا آل أحمد وآل محمود" else "The Ahmed and Mahmoud Families")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hosts_text_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Date, Day & Time (موعد وتوقيت الفرح)
                DesignerSectionCard(
                    title = if (strings.isArabic) "موعد وتوقيت الفرح" else "Date & Time"
                ) {
                    val days = if (strings.isArabic) {
                        listOf("الخميس", "الجمعة", "السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء")
                    } else {
                        listOf("Thursday", "Friday", "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday")
                    }

                    Text(
                        text = if (strings.isArabic) "يوم الحفل:" else "Day of the week:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(days) { d ->
                            FilterChip(
                                selected = dayName == d,
                                onClick = { dayName = d },
                                label = { Text(d, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(strings.eventDateLabel) },
                        placeholder = { Text("2026/10/25") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("designer_event_date_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = timeFrom,
                            onValueChange = { timeFrom = it },
                            label = { Text(strings.timeFromLabel) },
                            placeholder = { Text("7:00 م") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = timeTo,
                            onValueChange = { timeTo = it },
                            label = { Text(strings.timeToLabel) },
                            placeholder = { Text("11:00 م") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Venue Location & Google Maps Barcode / QR
                DesignerSectionCard(
                    title = if (strings.isArabic) "مكان الفرح ورابط لوكيشن الخريطة (باركود)" else "Venue & Location Barcode (QR)"
                ) {
                    OutlinedTextField(
                        value = venueName,
                        onValueChange = { venueName = it },
                        label = { Text(if (strings.isArabic) "اسم القاعة أو الفندق" else "Venue / Hall Name") },
                        placeholder = {
                            Text(if (strings.isArabic) "قاعة رويال بالاس - فندق الماسة" else "Grand Plaza Ballroom")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("venue_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = venueAddress,
                        onValueChange = { venueAddress = it },
                        label = { Text(if (strings.isArabic) "عنوان القاعة بالتفصيل" else "Venue Address") },
                        placeholder = {
                            Text(if (strings.isArabic) "التجمع الخامس، القاهرة" else "New Cairo, Egypt")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("venue_address_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // FEATURE REQUEST: Location Link & QR Code on Invitation
                    OutlinedTextField(
                        value = locationUrl,
                        onValueChange = { locationUrl = it },
                        label = {
                            Text(
                                if (strings.isArabic) {
                                    "رابط لوكيشن الفرح على الخريطة (Google Maps)"
                                } else {
                                    "Wedding Location Link (Google Maps)"
                                }
                            )
                        },
                        placeholder = { Text("https://maps.app.goo.gl/...") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalGold) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_url_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) {
                                    "إظهار باركود (QR) لوكيشن القاعة على الدعوة"
                                } else {
                                    "Show Location Barcode (QR) on Card"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (strings.isArabic) {
                                    "يقوم الضيف بمسحه لفتح موقع القاعة مباشرة على Google Maps"
                                } else {
                                    "Guests scan this QR to open the venue in Google Maps"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showLocationQr,
                            onCheckedChange = { showLocationQr = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LuxCard(
                        cornerRadius = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianNight.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "📍 ميزة ذكية: يتحول رابط خرائط جوجل تلقائيًا إلى باركود أنيق يظهر على بطاقة الدعوة مع عبارة 'مسح لوكيشن القاعة'!"
                                } else {
                                    "📍 Smart Feature: Google Maps URL automatically renders as an elegant QR code on the wedding invitation card!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = RoyalGoldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 5: Notes & Closing Wishes (ملاحظات وتمنيات ختامية)
                DesignerSectionCard(
                    title = if (strings.isArabic) "ملاحظات إضافية وتمنيات ختامية" else "Notes & Closing Wishes"
                ) {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(if (strings.isArabic) "ملاحظة خاصة (مثال: جنة الأطفال منازلهم)" else "Special Notes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_text_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = closingText,
                        onValueChange = { closingText = it },
                        label = { Text(if (strings.isArabic) "كلمة ختامية (مثال: دامت دياركم عامرة بالأفراح)" else "Closing Message") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("closing_text_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 1 Bottom Navigation: Back & Next Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(if (strings.isArabic) "⬅ السابق: توليد وتصميم الدعوة" else "⬅ Back: Card Design")
                    }

                    Button(
                        onClick = { currentStep = 2 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("step1_next_button")
                    ) {
                        Text(
                            text = if (strings.isArabic) "التالي: ضبط الباركود والخطوط ➔" else "Next: Barcode & Style ➔",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =========================================================================
            // STEP 2: ضبط الباركود والخطوط والألوان (BARCODE, STYLE & FINAL SAVE)
            // =========================================================================
            if (currentStep == 2) {
                // Interactive Preview with Live Drag & Drop
                LuxCard(
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.dragHintText,
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGoldLight,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showFullScreenAdjust = true },
                                modifier = Modifier.testTag("fullscreen_position_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = strings.fullScreenPositionBtn,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.fullScreenPositionBtn, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.widthIn(max = 310.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            InvitationPreviewCanvas(
                                event = draftEvent,
                                guestName = strings.sampleGuestName,
                                guestCode = if (previewBarcodeType == "LOCATION") "" else "48291037",
                                interactiveDrag = true,
                                hideQrCode = false,
                                showLocationQrOnly = previewBarcodeType == "LOCATION",
                                onQrPosChanged = { x, y ->
                                    qrPosX = x
                                    qrPosY = y
                                },
                                onNamePosChanged = { x, y ->
                                    namePosX = x
                                    namePosY = y
                                },
                                onDatePosChanged = { x, y ->
                                    datePosX = x
                                    datePosY = y
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch Preview Barcode: Location QR vs Guest QR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = previewBarcodeType == "LOCATION",
                                onClick = { previewBarcodeType = "LOCATION" },
                                label = {
                                    Text(
                                        text = if (strings.isArabic) "📍 باركود لوكيشن القاعة" else "Venue Location QR",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = previewBarcodeType == "GUEST",
                                onClick = { previewBarcodeType = "GUEST" },
                                label = {
                                    Text(
                                        text = if (strings.isArabic) "🎟️ باركود دخول الضيوف" else "Guest Pass QR",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Preset Colors + Color Picker
                DesignerSectionCard(title = strings.colorsSection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.nameColorLabel, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { customColorTarget = "NAME" }) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.customHexColorLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    ColorPaletteRow(
                        selectedHex = nameColorHex,
                        onSelectHex = { nameColorHex = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.qrColorLabel, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { customColorTarget = "QR" }) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.customHexColorLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    ColorPaletteRow(
                        selectedHex = qrColorHex,
                        onSelectHex = { qrColorHex = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: 8 Arabic & Latin Fonts
                DesignerSectionCard(title = strings.fontsSection) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DesignCatalog.fontChoices.forEach { font ->
                            FilterChip(
                                selected = fontIndex == font.index,
                                onClick = { fontIndex = font.index },
                                label = {
                                    Text(
                                        text = if (strings.isArabic) font.nameAr else font.nameEn,
                                        fontFamily = font.composeFontFamily,
                                        fontWeight = font.fontWeight,
                                        fontStyle = font.fontStyle
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Sizing Sliders
                DesignerSectionCard(title = strings.sizeAndPositionSection) {
                    Text(
                        text = "${strings.qrSizeLabel}: ${(qrSizeRatio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = qrSizeRatio,
                        onValueChange = { qrSizeRatio = it },
                        valueRange = 0.16f..0.44f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${strings.nameSizeLabel}: ${nameSizeSp.roundToInt()} sp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = nameSizeSp,
                        onValueChange = { nameSizeSp = it },
                        valueRange = 14f..40f
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Visibility Switches
                DesignerSectionCard(title = strings.visibilitySection) {
                    VisibilitySwitchRow(
                        label = strings.showGuestNameLabel,
                        checked = showGuestName,
                        onCheckedChange = { showGuestName = it }
                    )
                    VisibilitySwitchRow(
                        label = strings.showDateLabel,
                        checked = showDate,
                        onCheckedChange = { showDate = it }
                    )
                    VisibilitySwitchRow(
                        label = strings.showTimeLabel,
                        checked = showTime,
                        onCheckedChange = { showTime = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High Quality Download & Share Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val uri = InvitationRenderer.saveGeneralWeddingCardToGallery(context, draftEvent, originalQuality = true)
                            if (uri != null) {
                                downloadStatusBanner = if (strings.isArabic) {
                                    "✅ تم حفظ وتصدير الدعوة بجودة فائقة في المعرض! 📥"
                                } else {
                                    "✅ High-resolution wedding card exported to Gallery! 📥"
                                }
                                Toast.makeText(
                                    context,
                                    if (strings.isArabic) "تم الحفظ بجودة فائقة في المعرض 📥" else "Exported to Gallery 📥",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = RoyalGoldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (strings.isArabic) "📥 تنزيل الدعوة النهائية بجودة أصلية فائقة" else "Export High-Resolution Card",
                        fontWeight = FontWeight.Bold,
                        color = RoyalGoldLight
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step 2 Bottom Navigation & Save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(if (strings.isArabic) "⬅ السابق: بيانات الفرح" else "⬅ Back")
                    }

                    Button(
                        onClick = { onSaveEvent(draftEvent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(54.dp)
                            .testTag("save_event_design_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.saveEventAndDesignBtn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Full-Screen Position Adjust Modal (ضبط المكان بملء الشاشة مع السحب)
    if (showFullScreenAdjust) {
        Dialog(
            onDismissRequest = { showFullScreenAdjust = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianNight.copy(alpha = 0.96f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.fullScreenPositionBtn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RoyalGoldLight
                        )
                        IconButton(onClick = { showFullScreenAdjust = false }) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                        }
                    }

                    Text(
                        text = strings.dragHintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        InvitationPreviewCanvas(
                            event = draftEvent,
                            guestName = strings.sampleGuestName,
                            guestCode = if (previewBarcodeType == "LOCATION") "" else "48291037",
                            interactiveDrag = true,
                            hideQrCode = false,
                            showLocationQrOnly = previewBarcodeType == "LOCATION",
                            onQrPosChanged = { x, y ->
                                qrPosX = x
                                qrPosY = y
                            },
                            onNamePosChanged = { x, y ->
                                namePosX = x
                                namePosY = y
                            },
                            onDatePosChanged = { x, y ->
                                datePosX = x
                                datePosY = y
                            },
                            modifier = Modifier.fillMaxWidth(0.94f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showFullScreenAdjust = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianNight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(strings.close, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Custom Hex Color Picker Dialog
    customColorTarget?.let { target ->
        var hexInput by remember {
            mutableStateOf(if (target == "NAME") nameColorHex else qrColorHex)
        }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            modifier = Modifier.goldCorners(28.dp),
            onDismissRequest = { customColorTarget = null },
            title = {
                Text(
                    text = if (target == "NAME") strings.nameColorLabel else strings.qrColorLabel,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = strings.customHexColorLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = {
                            hexInput = it
                            errorMsg = null
                        },
                        label = { Text("Hex Code (#RRGGBB)") },
                        singleLine = true,
                        isError = errorMsg != null
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = hexInput.trim()
                        if (trimmed.matches(Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"))) {
                            val fullHex = if (trimmed.length == 7) {
                                "#FF" + trimmed.removePrefix("#")
                            } else {
                                trimmed
                            }
                            if (target == "NAME") {
                                nameColorHex = fullHex
                            } else {
                                qrColorHex = fullHex
                            }
                            customColorTarget = null
                        } else {
                            errorMsg = if (strings.isArabic) "صيغة غير صحيحة، اكتب مثلاً #FFD700" else "Invalid Hex, e.g. #FFD700"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianNight
                    )
                ) {
                    Text(strings.save, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customColorTarget = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

/**
 * Step tab indicator item with step number and title.
 */
@Composable
private fun StepTabItem(
    stepNumber: String,
    title: String,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) RoyalGold.copy(alpha = 0.22f) else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) RoyalGold else if (isCompleted) EmeraldAttended else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) RoyalGold else if (isCompleted) EmeraldAttended else Color.White.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted && !isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ObsidianNight,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        color = if (isSelected) ObsidianNight else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) RoyalGoldLight else Color.White.copy(alpha = 0.85f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DesignerSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    LuxCard(
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RoyalGoldLight
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ColorPaletteRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(DesignCatalog.presetColorsHex) { hex ->
            val color = DesignCatalog.parseColor(hex, Color.White)
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) RoyalGold else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .clickable { onSelectHex(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)

@Composable
private fun VisibilitySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
