package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.EventEntity
import com.example.data.GuestEntity
import com.example.data.SkippedGuestItem
import com.example.ui.components.InvitationPreviewCanvas
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightOutline
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.ui.theme.WhatsAppGreen
import com.example.util.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddGuestsDialog(
    onDismiss: () -> Unit,
    onAddByNames: (String) -> Unit,
    onAddBySequence: (Int, Int, String) -> Unit,
    onAddByPhones: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var namesInput by rememberSaveable { mutableStateOf("") }
    var seqFrom by rememberSaveable { mutableStateOf("1") }
    var seqTo by rememberSaveable { mutableStateOf("25") }
    var seqPrefix by rememberSaveable {
        mutableStateOf(if (strings.isArabic) "طاولة" else "Table")
    }
    var phonesInput by rememberSaveable { mutableStateOf("") }

    // File picker for uploading a TXT/CSV file of names + optional phone numbers and adding them all at once
    val namesFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                }
            } catch (_: Exception) {
                null
            }
            if (!content.isNullOrBlank()) {
                // Support both TXT lines ("أحمد محمد 01012345678") and CSV/Excel lines ("أحمد محمد,01012345678")
                val normalizedLines = content
                    .replace("\uFEFF", "")
                    .lines()
                    .map { line -> line.replace(',', ' ').replace(';', ' ').replace('\t', ' ').trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                if (normalizedLines.isNotBlank()) {
                    namesInput = normalizedLines
                    onAddByNames(normalizedLines)
                }
            }
        }
    }

    // File picker for uploading a TXT/CSV file of phone numbers and adding them all at once
    val phonesFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                }
            } catch (_: Exception) {
                null
            }
            if (!content.isNullOrBlank()) {
                val normalizedLines = content
                    .replace("\uFEFF", "")
                    .lines()
                    .flatMap { line -> line.split(',', ';', '\t') }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                if (normalizedLines.isNotBlank()) {
                    phonesInput = normalizedLines
                    onAddByPhones(normalizedLines)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.addGuestsDialogTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RoyalGoldLight
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ObsidianNight,
                    contentColor = RoyalGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.tabByNames, style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.tabBySequence, style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(strings.tabByPhones, style = MaterialTheme.typography.labelMedium) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        Text(
                            text = strings.byNamesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                namesFileLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_names_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "رفع ملف الأسماء والأرقام دفعة واحدة (TXT / CSV)"
                                } else {
                                    "Upload Names & Numbers File at Once (TXT / CSV)"
                                },
                                color = RoyalGoldLight,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = namesInput,
                            onValueChange = { namesInput = it },
                            minLines = 5,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_guests_names_input")
                        )
                    }

                    1 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = seqFrom,
                                onValueChange = { seqFrom = it.filter { ch -> ch.isDigit() } },
                                label = { Text(strings.seqFromLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = seqTo,
                                onValueChange = { seqTo = it.filter { ch -> ch.isDigit() } },
                                label = { Text(strings.seqToLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = seqPrefix,
                            onValueChange = { seqPrefix = it },
                            label = { Text(strings.seqPrefixLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        Text(
                            text = strings.byPhonesHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                phonesFileLauncher.launch(arrayOf("text/*", "application/csv", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_phones_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) {
                                    "رفع ملف أرقام الموبايل دفعة واحدة (TXT / CSV)"
                                } else {
                                    "Upload Phone Numbers File at Once (TXT / CSV)"
                                },
                                color = RoyalGoldLight,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phonesInput,
                            onValueChange = { phonesInput = it },
                            minLines = 5,
                            maxLines = 8,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_guests_phones_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> if (namesInput.isNotBlank()) onAddByNames(namesInput)
                        1 -> {
                            val from = seqFrom.toIntOrNull() ?: 1
                            val to = seqTo.toIntOrNull() ?: 10
                            onAddBySequence(from, to, seqPrefix)
                        }
                        2 -> if (phonesInput.isNotBlank()) onAddByPhones(phonesInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = ObsidianNight
                ),
                modifier = Modifier.testTag("confirm_add_guests_button")
            ) {
                Text(strings.addNowBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun SkippedGuestsDialog(
    addedCount: Int,
    skippedItems: List<SkippedGuestItem>,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.skippedGuestsTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AmberWarning
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = String.format(strings.addedSuccessFormat, addedCount),
                    color = EmeraldAttended,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(skippedItems) { item ->
                        Surface(
                            color = ObsidianNight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.inputLine,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalGoldLight
                                )
                                Text(
                                    text = if (strings.isArabic) item.reasonAr else item.reasonEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

/**
 * Section 3.4: «مين سجل ومين لا»: الضغط على «الحاضرون» أو «لم يصلوا» بيفتح قايمة بالأسماء، والحاضرين بوقت دخولهم.
 */
@Composable
fun AttendanceListModal(
    title: String,
    guests: List<GuestEntity>,
    showCheckInTime: Boolean,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$title (${guests.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RoyalGoldLight
            )
        },
        text = {
            if (guests.isEmpty()) {
                Text(
                    text = strings.noAttendanceYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(guests, key = { it.id }) { guest ->
                        Surface(
                            color = ObsidianNight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${guest.sequenceNumber}. ${guest.name}",
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalGoldLight
                                    )
                                    Text(
                                        text = "#${guest.code}" + if (guest.phone.isNotBlank()) " · ${guest.phone}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (showCheckInTime && guest.attendedAt != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldAttended,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = timeFormatter.format(Date(guest.attendedAt)),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldAttended
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

/**
 * Section 3.6: Sequential WhatsApp Dispatch (الإرسال المتتابع)
 * Shows next unsent guest, progress (e.g. 12/50), single "Send to Next" button, and "Skip" button.
 */
@Composable
fun SequentialWhatsAppDialog(
    queue: List<GuestEntity>,
    currentIndex: Int,
    totalCount: Int,
    onSendCurrentAndAdvance: (GuestEntity) -> Unit,
    onSkipCurrent: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentGuest = queue.getOrNull(currentIndex)
    val completedCount = (totalCount - queue.size + currentIndex).coerceIn(0, totalCount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.sequentialDispatchTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight
                )
                Text(
                    text = "$completedCount / $totalCount",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = WhatsAppGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = {
                        if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 1f
                    },
                    color = WhatsAppGreen,
                    trackColor = ObsidianNight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (currentGuest == null) {
                    Text(
                        text = strings.allGuestsSentMsg,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldAttended,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Surface(
                        color = ObsidianNight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${currentGuest.sequenceNumber}. ${currentGuest.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = RoyalGoldLight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentGuest.phone.isNotBlank()) {
                                    "📱 ${currentGuest.phone}"
                                } else {
                                    if (strings.isArabic) "بدون رقم موبايل (مشاركة عامة)" else "No phone (General Share)"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "#${currentGuest.code}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = RoyalGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSendCurrentAndAdvance(currentGuest) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = ObsidianNight
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sequential_send_next_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.sendToNextBtn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onSkipCurrent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.skipGuestBtn)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
fun SelectRangeDialog(
    maxSequence: Int,
    onSelectRange: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var fromText by rememberSaveable { mutableStateOf("1") }
    var toText by rememberSaveable { mutableStateOf(maxSequence.coerceAtLeast(1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.selectRangeBtn) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fromText,
                    onValueChange = { fromText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.seqFromLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = toText,
                    onValueChange = { toText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(strings.timeToLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f = fromText.toIntOrNull() ?: 1
                    val t = toText.toIntOrNull() ?: maxSequence
                    onSelectRange(minOf(f, t), maxOf(f, t))
                    onDismiss()
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun WhatsAppTemplateDialog(
    initialTemplate: String,
    onSaveTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var template by rememberSaveable { mutableStateOf(initialTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.whatsappTemplateDialogTitle) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.whatsappPlaceholdersHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = RoyalGoldLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        template = if (strings.isArabic) {
                            EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR
                        } else {
                            EventEntity.DEFAULT_WHATSAPP_TEMPLATE_EN
                        }
                    }
                ) {
                    Text(
                        text = if (strings.isArabic) "استعادة الرسالة الافتراضية" else "Reset to Default Template",
                        color = RoyalGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveTemplate(template)
                    onDismiss()
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun EditEventMetadataDialog(
    event: EventEntity,
    onSave: (EventEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var name by rememberSaveable { mutableStateOf(event.name) }
    var date by rememberSaveable { mutableStateOf(event.date) }
    var timeFrom by rememberSaveable { mutableStateOf(event.timeFrom) }
    var timeTo by rememberSaveable { mutableStateOf(event.timeTo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editEventInfoMenu) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.eventNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(strings.eventDateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeFrom,
                        onValueChange = { timeFrom = it },
                        label = { Text(strings.timeFromLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeTo,
                        onValueChange = { timeTo = it },
                        label = { Text(strings.timeToLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        event.copy(
                            name = name.ifBlank { event.name },
                            date = date,
                            timeFrom = timeFrom,
                            timeTo = timeTo
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun GuestInvitationPreviewDialog(
    event: EventEntity,
    guest: GuestEntity,
    onDownloadImage: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MidnightCard,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guest.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RoyalGoldLight
                        )
                        Text(
                            text = "QR: ${guest.qrPayload()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth(0.92f)) {
                    InvitationPreviewCanvas(
                        event = event,
                        guestName = guest.name,
                        guestCode = guest.code,
                        interactiveDrag = false
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MidnightOutline)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (strings.isArabic) "تحميل ⬇️" else "Download ⬇️")
                    }
                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = ObsidianNight
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isArabic) "واتساب 💬" else "WhatsApp 💬",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
