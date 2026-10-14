package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.EventEntity
import com.example.data.GuestEntity
import com.example.data.ScanOutcome
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonErrorContainer
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.EmeraldAttendedContainer
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.InvitationRenderer
import com.example.util.LocalAppStrings
import com.example.util.QrFrameAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    event: EventEntity,
    guests: List<GuestEntity>,
    continuousScan: Boolean,
    onToggleContinuousScan: (Boolean) -> Unit,
    onVerifyBarcode: (String, (ScanOutcome) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentOutcome by remember { mutableStateOf<ScanOutcome?>(null) }
    var scannerActive by remember { mutableStateOf(true) }
    var manualBarcodeInput by rememberSaveable { mutableStateOf("") }

    // Automatically re-arm scanner after 2.4 seconds if continuousScan is enabled
    LaunchedEffect(currentOutcome, continuousScan) {
        if (currentOutcome != null && continuousScan) {
            delay(2400L)
            scannerActive = true
        }
    }

    fun handleScannedPayload(payload: String) {
        if (payload.isBlank()) return
        scannerActive = false
        onVerifyBarcode(payload) { outcome ->
            currentOutcome = outcome
            triggerHapticFeedback(context, outcome is ScanOutcome.CheckedInSuccess)
        }
    }

    // Also allow picking an invitation image from gallery to decode its QR code directly
    val imageQrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                val text = InvitationRenderer.decodeQrFromBitmap(bmp)
                                bmp.recycle()
                                text
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                handleScannedPayload(decoded ?: "UNKNOWN_IMAGE_QR")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.scannerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RoyalGoldLight
                        )
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("scanner_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Continuous Scan Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.continuousScanLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = continuousScan,
                        onCheckedChange = onToggleContinuousScan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CameraX Live Viewfinder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .height(260.dp)
                    .border(2.dp, RoyalGold.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianNight)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && scannerActive) {
                        CameraQrPreview(
                            onQrDetected = { rawQr ->
                                if (scannerActive) {
                                    handleScannedPayload(rawQr)
                                }
                            }
                        )
                        // Target reticle overlay
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .border(2.dp, RoyalGoldLight, RoundedCornerShape(18.dp))
                        )
                    } else if (!hasCameraPermission) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalGold,
                                    contentColor = ObsidianNight
                                )
                            ) {
                                Text(if (strings.isArabic) "تفعيل الكاميرا للمسح" else "Grant Camera Permission")
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = RoyalGoldLight,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    currentOutcome = null
                                    scannerActive = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalGold,
                                    contentColor = ObsidianNight
                                )
                            ) {
                                Text(strings.scanNextBtn, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    imageQrLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Icon(Icons.Default.ImageSearch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isArabic) {
                        "مسح باركود من صورة دعوة محفوظة"
                    } else {
                        "Scan QR from Saved Invitation Image"
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scan Result Banner (Section 3.8: 4 distinct states)
            AnimatedVisibility(visible = currentOutcome != null) {
                currentOutcome?.let { outcome ->
                    ScanOutcomeBanner(
                        outcome = outcome,
                        onScanNext = {
                            currentOutcome = null
                            scannerActive = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual / Test Scan Section for instant verification on Emulator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.manualOrTestScanSection,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGoldLight
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            label = { Text(strings.enterCodeManuallyLabel) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_barcode_input")
                        )
                        Button(
                            onClick = { handleScannedPayload(manualBarcodeInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianNight
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("verify_barcode_button")
                        ) {
                            Text(strings.verifyCodeBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanOutcomeBanner(
    outcome: ScanOutcome,
    onScanNext: () -> Unit
) {
    val strings = LocalAppStrings.current
    val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    val (bgColor, borderColor, icon, title, details) = when (outcome) {
        is ScanOutcome.CheckedInSuccess -> {
            val timeStr = timeFormatter.format(Date(outcome.checkInTimeMillis))
            OutcomeVisuals(
                bgColor = EmeraldAttendedContainer,
                borderColor = EmeraldAttended,
                icon = Icons.Default.CheckCircle,
                title = strings.scanSuccessTitle,
                details = "${outcome.guest.name}\n#${outcome.guest.code}  •  $timeStr"
            )
        }

        is ScanOutcome.AlreadyCheckedIn -> {
            val firstTimeStr = timeFormatter.format(Date(outcome.firstCheckInTimeMillis))
            OutcomeVisuals(
                bgColor = CrimsonErrorContainer,
                borderColor = CrimsonError,
                icon = Icons.Default.Block,
                title = strings.scanDuplicateTitle,
                details = "${outcome.guest.name} (#${outcome.guest.code})\n${strings.firstEntryTimeLabel} $firstTimeStr"
            )
        }

        is ScanOutcome.WrongEvent -> {
            val otherLabel = outcome.otherEventName ?: "#${outcome.otherEventId}"
            OutcomeVisuals(
                bgColor = AmberWarningContainer,
                borderColor = AmberWarning,
                icon = Icons.Default.WarningAmber,
                title = strings.scanWrongEventTitle,
                details = if (strings.isArabic) {
                    "هذه الدعوة (#${outcome.guestCode}) تخص مناسبة أخرى: $otherLabel"
                } else {
                    "Invitation (#${outcome.guestCode}) belongs to another event: $otherLabel"
                }
            )
        }

        is ScanOutcome.UnknownInvitation -> {
            OutcomeVisuals(
                bgColor = CrimsonErrorContainer,
                borderColor = CrimsonError,
                icon = Icons.Default.ErrorOutline,
                title = strings.scanUnknownTitle,
                details = outcome.rawBarcode
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .testTag("scan_outcome_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = details,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                fontFamily = FontFamily.SansSerif,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScanNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = ObsidianNight
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.scanNextBtn, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class OutcomeVisuals(
    val bgColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val details: String
)

@Composable
private fun CameraQrPreview(
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp)),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, QrFrameAnalyzer(onQrDetected))
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun triggerHapticFeedback(context: Context, success: Boolean) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (success) {
                vibrator.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 140), -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (success) 90L else 250L)
        }
    } catch (_: Exception) {
    }
}
