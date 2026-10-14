package com.example.ui.screens.reception

import com.example.ui.components.goldBottomLine
import com.example.ui.components.LuxCard
import com.example.ui.components.arabesqueLattice
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.EmeraldMedium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.BackupImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.EventEntity
import com.example.data.EventWithCounts
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionEventSelectionScreen(
    userEmail: String,
    events: List<EventWithCounts>,
    isDownloadingGuests: Boolean,
    downloadStatusMessage: String?,
    onSelectEvent: (EventEntity) -> Unit,
    onLogout: () -> Unit,
    onImportSecureEvent: (String, String, (BackupImportResult) -> Unit) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPasswordInput by remember { mutableStateOf("") }
    var importResultMessage by remember { mutableStateOf<String?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            showImportPasswordDialog = true
            importPasswordInput = ""
            importResultMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Column {
                        Text(
                            text = if (strings.isArabic) "اختيار المناسبة النشطة" else "Select Active Event",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RoyalGoldLight
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { importFileLauncher.launch("*/*") },
                        modifier = Modifier.testTag("import_offline_event_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = if (strings.isArabic) "استيراد مناسبة أوفلاين" else "Import Offline Event",
                            tint = RoyalGoldLight
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("reception_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = strings.logoutBtn,
                            tint = RoyalGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianNight)
                .arabesqueLattice()
        ) {
            if (events.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = RoyalGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (strings.isArabic) "لا توجد مناسبات نشطة حالياً" else "No active events available",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (strings.isArabic) {
                            "يرجى التواصل مع مسؤول النظام لإنشاء المناسبات وتكليف الاستقبال بها."
                        } else {
                            "Please contact system admin to setup events for gate check-in."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Surface(
                            color = MidnightCard,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = RoyalGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (strings.isArabic) {
                                        "اختر المناسبة لتحميل قائمة ضيوفها وتفعيل الفحص السريع والعمل أوفلاين عند البوابة."
                                    } else {
                                        "Select an event to download guests for fast offline gate scanning."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(events) { item ->
                        ReceptionEventCard(
                            eventWithCounts = item,
                            onSelect = { onSelectEvent(item.event) }
                        )
                    }
                }
            }

            // Downloading Guests Modal Dialog
            if (isDownloadingGuests) {
                Dialog(onDismissRequest = {}) {
                    LuxCard(
                        cornerRadius = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(1.dp, RoyalGold, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = RoyalGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (strings.isArabic) "تحميل بيانات المناسبة والضيوف" else "Downloading Event Guests",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalGoldLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = downloadStatusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Import Secure Event Dialog
            if (showImportPasswordDialog) {
                Dialog(onDismissRequest = { showImportPasswordDialog = false }) {
                    LuxCard(
                        cornerRadius = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightCard),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(1.dp, RoyalGold, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (strings.isArabic) "استيراد مناسبة أوفلاين (مشفرة)" else "Import Offline Event (PIN)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalGoldLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "أدخل كلمة المرور (PIN) لفك تشفير واستيراد ملف المناسبة:"
                                } else {
                                    "Enter password (PIN) to decrypt and import event file:"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = importPasswordInput,
                                onValueChange = { importPasswordInput = it },
                                label = { Text(if (strings.isArabic) "كلمة المرور / PIN" else "Password / PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (importResultMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = importResultMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showImportPasswordDialog = false
                                        selectedImportUri = null
                                        importPasswordInput = ""
                                        importResultMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.cancel)
                                }
                                Button(
                                    onClick = {
                                        val uri = selectedImportUri ?: return@Button
                                        if (importPasswordInput.isBlank()) {
                                            importResultMessage = if (strings.isArabic) "يرجى إدخال كلمة المرور" else "Please enter password"
                                            return@Button
                                        }
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val jsonString = try {
                                                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                                            } catch (e: Exception) {
                                                null
                                            }
                                            if (jsonString.isNullOrBlank()) {
                                                withContext(Dispatchers.Main) {
                                                    importResultMessage = if (strings.isArabic) "تعذر قراءة الملف" else "Could not read file"
                                                }
                                                return@launch
                                            }
                                            onImportSecureEvent(jsonString, importPasswordInput) { result ->
                                                when (result) {
                                                    is BackupImportResult.Success -> {
                                                        importResultMessage = if (strings.isArabic) {
                                                            "تم استيراد المناسبة بنجاح ✨ (${result.eventsAdded} مناسبة، ${result.guestsAdded} ضيف)"
                                                        } else {
                                                            "Event imported successfully ✨ (${result.eventsAdded} event, ${result.guestsAdded} guests)"
                                                        }
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1500)
                                                            showImportPasswordDialog = false
                                                            selectedImportUri = null
                                                            importPasswordInput = ""
                                                            importResultMessage = null
                                                        }
                                                    }
                                                    is BackupImportResult.Conflict -> {
                                                        importResultMessage = if (strings.isArabic) {
                                                            "تعذر الدمج: ${result.reasonAr}"
                                                        } else {
                                                            "Import conflict: ${result.reasonEn}"
                                                        }
                                                    }
                                                    is BackupImportResult.Invalid -> {
                                                        importResultMessage = if (strings.isArabic) {
                                                            "كلمة المرور خاطئة أو الملف غير صالح"
                                                        } else {
                                                            "Incorrect password or invalid file format"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianNight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (strings.isArabic) "استيراد" else "Import", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceptionEventCard(
    eventWithCounts: EventWithCounts,
    onSelect: () -> Unit
) {
    val strings = LocalAppStrings.current
    val event = eventWithCounts.event
    val total = eventWithCounts.totalGuests
    val attended = eventWithCounts.attendedGuests
    val progress = if (total > 0) attended.toFloat() / total else 0f

    LuxCard(
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable { onSelect() }
            .border(1.dp, RoyalGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .testTag("reception_event_card_${event.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = RoyalGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$attended / $total",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = event.date.ifBlank { if (strings.isArabic) "بدون تاريخ" else "No Date" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (event.timeFrom.isNotBlank()) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.timeFrom + (if (event.timeTo.isNotBlank()) " - ${event.timeTo}" else ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = EmeraldAttended,
                trackColor = ObsidianNight
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldMedium,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, RoyalGold, RoundedCornerShape(14.dp))
                    .testTag("select_event_btn_${event.id}")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (strings.isArabic) "مسح QR" else "Scan QR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
