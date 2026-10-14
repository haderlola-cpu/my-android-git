package com.example.ui.screens

import com.example.ui.components.LuxIconButton
import com.example.ui.components.goldBottomLine
import com.example.ui.components.LuxCard
import com.example.ui.components.arabesqueLattice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Lock
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AddGuestsResult
import com.example.data.EventEntity
import com.example.data.GuestEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.EmeraldAttendedContainer
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightOutline
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import androidx.compose.material.icons.filled.Share
import com.example.ui.theme.WhatsAppGreen
import com.example.util.InvitationRenderer
import com.example.util.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    event: EventEntity,
    guests: List<GuestEntity>,
    zipProgress: Pair<Int, Int>?,
    viewModel: MainViewModel,
    onOpenScanner: () -> Unit,
    onEditDesign: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var filterMode by rememberSaveable { mutableIntStateOf(0) } // 0 = All, 1 = Attended, 2 = Not Arrived
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var visibleCount by rememberSaveable { mutableIntStateOf(30) } // Section 3.4: 30 per page

    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddGuestsDialog by remember { mutableStateOf(false) }
    var lastAddResult by remember { mutableStateOf<AddGuestsResult?>(null) }
    var attendanceModalMode by remember { mutableStateOf<Boolean?>(null) } // true = Attended, false = Not Arrived
    var showSequentialWhatsApp by remember { mutableStateOf(false) }
    var sequentialQueue by remember { mutableStateOf<List<GuestEntity>>(emptyList()) }
    var sequentialIndex by remember { mutableIntStateOf(0) }
    var showRangeDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var previewGuest by remember { mutableStateOf<GuestEntity?>(null) }
    var showSecureExportDialog by remember { mutableStateOf(false) }
    var exportPasswordInput by remember { mutableStateOf("") }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    val attendedGuests by remember(guests) {
        derivedStateOf { guests.filter { it.attended }.sortedByDescending { it.attendedAt ?: 0L } }
    }
    val notArrivedGuests by remember(guests) {
        derivedStateOf { guests.filter { !it.attended } }
    }

    val filteredGuests by remember(guests, filterMode, searchQuery) {
        derivedStateOf {
            val byStatus = when (filterMode) {
                1 -> guests.filter { it.attended }
                2 -> guests.filter { !it.attended }
                else -> guests
            }
            if (searchQuery.isBlank()) {
                byStatus
            } else {
                val q = searchQuery.trim().lowercase()
                byStatus.filter {
                    it.name.lowercase().contains(q) ||
                        it.code.contains(q) ||
                        it.phone.contains(q)
                }
            }
        }
    }

    val pagedGuests by remember(filteredGuests, visibleCount) {
        derivedStateOf { filteredGuests.take(visibleCount) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Column {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = RoyalGoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${event.date} · ${event.timeFrom} – ${event.timeTo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    LuxIconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("event_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    LuxIconButton(
                        onClick = onOpenScanner,
                        modifier = Modifier.testTag("top_scan_door_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = strings.scanDoorBtn,
                            tint = RoyalGold
                        )
                    }

                    Box {
                        LuxIconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("event_overflow_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (strings.isArabic) "مشاركة / تنزيل دعوة الفرح (باللوكيشن)" else "Share Wedding Invitation (Location QR)") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = RoyalGold) },
                                onClick = {
                                    showOverflowMenu = false
                                    coroutineScope.launch {
                                        InvitationRenderer.shareGeneralWeddingCard(context, event)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.scanDoorBtn) },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenScanner()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.editEventInfoMenu) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showEditInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.editDesignMenu) },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onEditDesign()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.whatsappTemplateMenu) },
                                leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showTemplateDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .arabesqueLattice()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Statistics Dashboard Card (Section 3.4)
            item {
                StatisticsDashboardCard(
                    event = event,
                    totalCount = guests.size,
                    attendedGuests = attendedGuests,
                    notArrivedCount = notArrivedGuests.size,
                    onClickAttended = { attendanceModalMode = true },
                    onClickNotArrived = { attendanceModalMode = false }
                )
            }

            // 2. Primary Action Buttons Bar (Scan at Door, Add Guests, Sequential WhatsApp, Download All ZIP)
            item {
                LuxCard(
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenScanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalGold,
                                    contentColor = ObsidianNight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("scan_door_primary_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.scanDoorBtn, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Button(
                                onClick = { showAddGuestsDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAttended,
                                    contentColor = ObsidianNight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("add_guests_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.addGuestsBtn, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Queue unsent guests (or all guests if all sent)
                                    val unsent = guests.filter { !it.whatsappSent }
                                    sequentialQueue = unsent.ifEmpty { guests }
                                    sequentialIndex = 0
                                    showSequentialWhatsApp = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sequential_whatsapp_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.sequentialWhatsappBtn, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { viewModel.exportEventZip(context, event, guests) },
                                enabled = zipProgress == null && guests.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_zip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = RoyalGoldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (zipProgress != null) {
                                        "ZIP ${zipProgress.first}/${zipProgress.second}"
                                    } else {
                                        strings.downloadAllZipBtn
                                    },
                                    maxLines = 1
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showSecureExportDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("secure_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = RoyalGoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (strings.isArabic) "تصدير آمن أوفلاين (مشفر)" else "Secure Offline Export",
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. Filter Chips, Search Bar, and Selection Mode (☑️)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = filterMode == 0,
                                onClick = { filterMode = 0 },
                                label = { Text("${strings.filterAll} (${guests.size})") }
                            )
                            FilterChip(
                                selected = filterMode == 1,
                                onClick = { filterMode = 1 },
                                label = { Text("${strings.filterAttended} (${attendedGuests.size})") }
                            )
                            FilterChip(
                                selected = filterMode == 2,
                                onClick = { filterMode = 2 },
                                label = { Text("${strings.filterNotArrived} (${notArrivedGuests.size})") }
                            )
                        }

                        FilterChip(
                            selected = selectionMode,
                            onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selectedIds = emptySet()
                            },
                            label = { Text(strings.selectModeBtn) }
                        )
                    }

                    // Selection Toolbar (Section 3.4: وضع التحديد ☑️)
                    AnimatedVisibility(visible = selectionMode) {
                        LuxCard(
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (strings.isArabic) {
                                            "تم تحديد ${selectedIds.size} دعوة"
                                        } else {
                                            "${selectedIds.size} selected"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalGoldLight
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { showRangeDialog = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(strings.selectRangeBtn, style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                selectedIds = if (selectedIds.size == filteredGuests.size) {
                                                    emptySet()
                                                } else {
                                                    filteredGuests.map { it.id }.toSet()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.selectAllBtn, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                if (selectedIds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val chosen = guests.filter { it.id in selectedIds }
                                                viewModel.shareSelectedBatch(context, event, chosen)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RoyalGold,
                                                contentColor = ObsidianNight
                                            )
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.downloadSelectedBtn, style = MaterialTheme.typography.labelSmall)
                                        }

                                        Button(
                                            onClick = {
                                                val chosen = guests.filter { it.id in selectedIds }
                                                sequentialQueue = chosen
                                                sequentialIndex = 0
                                                showSequentialWhatsApp = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WhatsAppGreen,
                                                contentColor = ObsidianNight
                                            )
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.sendSelectedWhatsappBtn, style = MaterialTheme.typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.deleteSelectedGuests(selectedIds.toList()) {
                                                    selectedIds = emptySet()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = CrimsonError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.deleteSelectedBtn,
                                                color = CrimsonError,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchGuestPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.close)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_guests_input")
                    )
                }
            }

            // 4. Guest Cards List (30 per page with Load More)
            items(pagedGuests, key = { it.id }) { guest ->
                val isSelected = guest.id in selectedIds
                GuestInvocationCard(
                    guest = guest,
                    selectionMode = selectionMode,
                    isSelected = isSelected,
                    onToggleSelect = {
                        selectedIds = if (isSelected) selectedIds - guest.id else selectedIds + guest.id
                    },
                    onPreviewClick = { previewGuest = guest },
                    onDownloadClick = {
                        viewModel.downloadSingleGuestImage(context, event, guest)
                    },
                    onWhatsAppClick = {
                        viewModel.sendGuestWhatsApp(context, event, guest)
                    },
                    onDeleteClick = {
                        viewModel.deleteGuest(guest.id)
                    }
                )
            }

            // Load More Button (+30)
            if (filteredGuests.size > visibleCount) {
                item {
                    OutlinedButton(
                        onClick = { visibleCount += 30 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .testTag("load_more_guests_button")
                    ) {
                        Text(
                            text = "${strings.loadMoreBtn} (${visibleCount}/${filteredGuests.size})",
                            color = RoyalGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Add Guests Dialog
    if (showAddGuestsDialog) {
        AddGuestsDialog(
            onDismiss = { showAddGuestsDialog = false },
            onAddByNames = { raw ->
                showAddGuestsDialog = false
                viewModel.addGuestsByNames(event.id, raw) { result ->
                    if (result.skippedItems.isNotEmpty()) {
                        lastAddResult = result
                    }
                }
            },
            onAddBySequence = { from, to, prefix ->
                showAddGuestsDialog = false
                viewModel.addGuestsBySequence(event.id, from, to, prefix) { result ->
                    if (result.skippedItems.isNotEmpty()) {
                        lastAddResult = result
                    }
                }
            },
            onAddByPhones = { rawPhones ->
                showAddGuestsDialog = false
                viewModel.addGuestsByPhones(event.id, rawPhones) { result ->
                    if (result.skippedItems.isNotEmpty()) {
                        lastAddResult = result
                    }
                }
            }
        )
    }

    // Skipped Items Summary Dialog
    if (lastAddResult != null) {
        SkippedGuestsDialog(
            addedCount = lastAddResult!!.addedCount,
            skippedItems = lastAddResult!!.skippedItems,
            onDismiss = { lastAddResult = null }
        )
    }

    // Who Checked In & Who Didn't Modal
    if (attendanceModalMode != null) {
        val showingAttended = attendanceModalMode == true
        AttendanceListModal(
            title = if (showingAttended) strings.attendedStat else strings.notArrivedStat,
            guests = if (showingAttended) attendedGuests else notArrivedGuests,
            showCheckInTime = showingAttended,
            onDismiss = { attendanceModalMode = null }
        )
    }

    // Sequential WhatsApp Dispatch Modal
    if (showSequentialWhatsApp) {
        SequentialWhatsAppDialog(
            queue = sequentialQueue,
            currentIndex = sequentialIndex,
            totalCount = guests.size,
            onSendCurrentAndAdvance = { currentGuest ->
                viewModel.sendGuestWhatsApp(context, event, currentGuest)
                sequentialIndex++
            },
            onSkipCurrent = {
                sequentialIndex++
            },
            onDismiss = { showSequentialWhatsApp = false }
        )
    }

    // Range Selection Dialog (من رقم إلى رقم)
    if (showRangeDialog) {
        SelectRangeDialog(
            maxSequence = guests.maxOfOrNull { it.sequenceNumber } ?: guests.size,
            onSelectRange = { fromSeq, toSeq ->
                selectedIds = guests
                    .filter { it.sequenceNumber in fromSeq..toSeq }
                    .map { it.id }
                    .toSet()
            },
            onDismiss = { showRangeDialog = false }
        )
    }

    // WhatsApp Welcome Message Template Dialog
    if (showTemplateDialog) {
        WhatsAppTemplateDialog(
            initialTemplate = event.whatsappTemplate,
            onSaveTemplate = { newTemplate ->
                viewModel.updateEventMetadata(event.copy(whatsappTemplate = newTemplate))
            },
            onDismiss = { showTemplateDialog = false }
        )
    }

    // Edit Event Info Dialog
    if (showEditInfoDialog) {
        EditEventMetadataDialog(
            event = event,
            onSave = { updated -> viewModel.updateEventMetadata(updated) },
            onDismiss = { showEditInfoDialog = false }
        )
    }

    // Single Guest Invitation Preview Modal
    if (previewGuest != null) {
        val target = previewGuest!!
        GuestInvitationPreviewDialog(
            event = event,
            guest = target,
            onDownloadImage = {
                viewModel.downloadSingleGuestImage(context, event, target)
            },
            onSendWhatsApp = {
                viewModel.sendGuestWhatsApp(context, event, target)
            },
            onDismiss = { previewGuest = null }
        )
    }

    // Secure Offline Export Dialog
    if (showSecureExportDialog) {
        Dialog(onDismissRequest = { showSecureExportDialog = false }) {
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
                        text = if (strings.isArabic) "تصدير آمن للمناسبة (مشفر بكلمة مرور)" else "Secure Event Export (PIN)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (strings.isArabic) {
                            "أدخل كلمة المرور (PIN) لتشفير وتصدير ملف هذه المناسبة للاستخدام أوفلاين:"
                        } else {
                            "Enter password (PIN) to encrypt and export this event for offline use:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = exportPasswordInput,
                        onValueChange = { exportPasswordInput = it },
                        label = { Text(if (strings.isArabic) "كلمة المرور / PIN" else "Password / PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exportStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportStatusMessage!!,
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
                                showSecureExportDialog = false
                                exportPasswordInput = ""
                                exportStatusMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancel)
                        }
                        Button(
                            onClick = {
                                if (exportPasswordInput.isBlank()) {
                                    exportStatusMessage = if (strings.isArabic) "يرجى إدخال كلمة المرور" else "Please enter password"
                                    return@Button
                                }
                                coroutineScope.launch {
                                    val encrypted = viewModel.exportSingleEventSecure(event.id, exportPasswordInput)
                                    if (!encrypted.isNullOrBlank()) {
                                        val safeName = event.name.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
                                        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                        val encFile = File(exportsDir, "elbeit_event_$safeName.enc")
                                        withContext(Dispatchers.IO) {
                                            encFile.writeText(encrypted, Charsets.UTF_8)
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", encFile)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/octet-stream"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                if (strings.isArabic) "مشاركة المناسبة المشفرة" else "Share Secure Event"
                                            )
                                        )
                                        showSecureExportDialog = false
                                        exportPasswordInput = ""
                                        exportStatusMessage = null
                                    } else {
                                        exportStatusMessage = if (strings.isArabic) "فشل التصدير" else "Export failed"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianNight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (strings.isArabic) "تصدير ومشاركة" else "Export & Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsDashboardCard(
    event: EventEntity,
    totalCount: Int,
    attendedGuests: List<GuestEntity>,
    notArrivedCount: Int,
    onClickAttended: () -> Unit,
    onClickNotArrived: () -> Unit
) {
    val strings = LocalAppStrings.current
    val attendedCount = attendedGuests.size
    val ratePercent = if (totalCount > 0) ((attendedCount * 100f) / totalCount.toFloat()).roundToInt() else 0
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val checkInTimestamps = attendedGuests.mapNotNull { it.attendedAt }.sorted()
    val firstCheckIn = checkInTimestamps.firstOrNull()
    val lastCheckIn = checkInTimestamps.lastOrNull()

    // Hourly attendance counts
    val hourlyCounts = remember(checkInTimestamps) {
        val map = linkedMapOf<Int, Int>()
        val cal = Calendar.getInstance()
        for (ts in checkInTimestamps) {
            cal.timeInMillis = ts
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            map[hour] = (map[hour] ?: 0) + 1
        }
        map
    }

    LuxCard(
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .border(1.dp, RoyalGold.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.statsDashboardTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight
                )
                Text(
                    text = "${strings.attendanceRateLabel}: $ratePercent%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = EmeraldAttended
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Metric Tiles: Total, Attended (clickable), Not Arrived (clickable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricTile(
                    label = strings.totalGuestsStat,
                    value = totalCount.toString(),
                    accentColor = RoyalGoldLight,
                    subtitle = null,
                    onClick = null,
                    modifier = Modifier.weight(1f)
                )
                StatMetricTile(
                    label = strings.attendedStat,
                    value = attendedCount.toString(),
                    accentColor = EmeraldAttended,
                    subtitle = strings.tapToViewListHint,
                    onClick = onClickAttended,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_attended_tile")
                )
                StatMetricTile(
                    label = strings.notArrivedStat,
                    value = notArrivedCount.toString(),
                    accentColor = AmberWarning,
                    subtitle = strings.tapToViewListHint,
                    onClick = onClickNotArrived,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_not_arrived_tile")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MidnightOutline)
            Spacer(modifier = Modifier.height(10.dp))

            // Actual Attendance Window (فترة الحضور الفعلية: من أول داخل لآخر داخل)
            Text(
                text = strings.attendanceWindowTitle,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = RoyalGoldLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (firstCheckIn != null && lastCheckIn != null) {
                val durationMinutes = ((lastCheckIn - firstCheckIn) / 60_000L).coerceAtLeast(0L)
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                val durationStr = if (strings.isArabic) {
                    "(${hours} س و ${mins} د)"
                } else {
                    "(${hours}h ${mins}m)"
                }
                Text(
                    text = "${strings.firstCheckInLabel}: ${timeFormatter.format(Date(firstCheckIn))}  •  ${strings.lastCheckInLabel}: ${timeFormatter.format(Date(lastCheckIn))} $durationStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = strings.noAttendanceYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Hourly Attendance Bar Chart & Last 3 Check-ins
            if (attendedGuests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.hourlyChartTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxHourly = (hourlyCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .background(ObsidianNight, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    hourlyCounts.forEach { (hour, count) ->
                        val ratio = (count.toFloat() / maxHourly.toFloat()).coerceIn(0.18f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAttended
                            )
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(ratio * 0.65f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(EmeraldAttended)
                            )
                            Text(
                                text = "${hour.toString().padStart(2, '0')}:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.last3CheckInsTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                attendedGuests.take(3).forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ ${recent.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldAttended
                        )
                        Text(
                            text = recent.attendedAt?.let { timeFormatter.format(Date(it)) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricTile(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    subtitle: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianNight,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GuestInvocationCard(
    guest: GuestEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    LuxCard(
        cornerRadius = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (selectionMode) onToggleSelect() else onPreviewClick()
            }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> RoyalGold
                    guest.attended -> EmeraldAttended.copy(alpha = 0.45f)
                    else -> MidnightOutline
                },
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("guest_card_${guest.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Sequence badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ObsidianNight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = guest.sequenceNumber.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guest.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = RoyalGoldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${guest.code}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = RoyalGold
                    )
                    if (guest.phone.isNotBlank()) {
                        Text(
                            text = guest.phone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Attendance Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (guest.attended) {
                        Surface(
                            color = EmeraldAttendedContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldAttended,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val timeStr = guest.attendedAt?.let { timeFormatter.format(Date(it)) } ?: ""
                                Text(
                                    text = "${strings.filterAttended} $timeStr",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldAttended
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = ObsidianNight,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.filterNotArrived,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (guest.whatsappSent) {
                        Text(
                            text = "✓ WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhatsAppGreen
                        )
                    }
                }
            }

            // Action Buttons: QR Preview, ⬇️ Download, 💬 WhatsApp, 🗑️ Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviewClick) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Preview QR",
                        tint = RoyalGoldLight
                    )
                }
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.testTag("download_guest_${guest.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = RoyalGold
                    )
                }
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.testTag("whatsapp_guest_${guest.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "WhatsApp",
                        tint = WhatsAppGreen
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_guest_${guest.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = CrimsonError.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
