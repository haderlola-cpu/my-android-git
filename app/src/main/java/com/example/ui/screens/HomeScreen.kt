package com.example.ui.screens

import com.example.ui.components.LuxDateTicket
import com.example.ui.components.LuxIconButton
import com.example.ui.components.goldBottomLine
import com.example.ui.components.goldCorners
import com.example.ui.components.LuxCard
import com.example.ui.components.LuxLogoBadge
import com.example.ui.components.arabesqueLattice
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EventWithCounts
import com.example.ui.components.ElbeitBrandLogo
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.DesignCatalog
import com.example.util.LocalAppStrings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    events: List<EventWithCounts>,
    onOpenEvent: (Long) -> Unit,
    onCreateNewEvent: () -> Unit,
    onQuickScanEvent: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val strings = LocalAppStrings.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var eventPendingDelete by remember { mutableStateOf<EventWithCounts?>(null) }

    val filteredEvents by remember(events, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                events
            } else {
                val q = searchQuery.trim().lowercase()
                events.filter {
                    it.event.name.lowercase().contains(q) ||
                        it.event.date.lowercase().contains(q)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LuxLogoBadge(badgeSize = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = RoyalGoldLight
                            )
                            Text(
                                text = strings.companySubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    LuxIconButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("home_language_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = strings.languageLabel,
                            tint = RoyalGold
                        )
                    }
                    LuxIconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settingsTitle,
                            tint = RoyalGoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNewEvent,
                containerColor = RoyalGold,
                contentColor = ObsidianNight,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = strings.newEventButton) },
                text = {
                    Text(
                        text = strings.newEventButton,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("create_event_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .arabesqueLattice()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchEventsPlaceholder) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = strings.searchEventsPlaceholder)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = strings.close)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = RoyalGold.copy(alpha = 0.35f),
                    cursorColor = RoyalGold,
                    focusedContainerColor = Color.Black.copy(alpha = 0.22f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.22f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("search_events_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            val totalGuestsAll = events.sumOf { it.totalGuests }
            val totalAttendedAll = events.sumOf { it.attendedGuests }
            val totalNotArrivedAll = (totalGuestsAll - totalAttendedAll).coerceAtLeast(0)
            val totalEventsCount = events.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HomeStatCard(label = if (strings.isArabic) "المدعوين" else "Guests", count = totalGuestsAll, color = RoyalGoldLight, modifier = Modifier.weight(1f))
                HomeStatCard(label = if (strings.isArabic) "الحضور" else "Attended", count = totalAttendedAll, color = EmeraldAttended, modifier = Modifier.weight(1f))
                HomeStatCard(label = if (strings.isArabic) "الغياب" else "Absence", count = totalNotArrivedAll, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                HomeStatCard(label = if (strings.isArabic) "الفعاليات" else "Events", count = totalEventsCount, color = RoyalGold, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(RoyalGold.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.noEventsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RoyalGoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.noEventsSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEvents, key = { it.event.id }) { item ->
                        EventSummaryCard(
                            item = item,
                            onClick = { onOpenEvent(item.event.id) },
                            onQuickScan = { onQuickScanEvent(item.event.id) },
                            onDeleteClick = { eventPendingDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (eventPendingDelete != null) {
        val target = eventPendingDelete!!
        AlertDialog(
            containerColor = MidnightCard,
            modifier = Modifier.goldCorners(28.dp),
            onDismissRequest = { eventPendingDelete = null },
            title = { Text(strings.deleteEventTitle) },
            text = {
                Text("${strings.deleteEventConfirm}\n\n«${target.event.name}»")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEvent(target.event.id)
                        eventPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventPendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun EventSummaryCard(
    item: EventWithCounts,
    onClick: () -> Unit,
    onQuickScan: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val event = item.event
    val preset = DesignCatalog.presetBackgrounds.getOrElse(event.backgroundPresetId) {
        DesignCatalog.presetBackgrounds.first()
    }
    val attendanceProgress = if (item.totalGuests > 0) {
        item.attendedGuests.toFloat() / item.totalGuests.toFloat()
    } else 0f
    val attendancePercent = (attendanceProgress * 100f).roundToInt()

    LuxCard(
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = preset.accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                LuxDateTicket(dateText = event.date, isArabic = strings.isArabic)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = RoyalGoldLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = RoyalGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val timeText = buildString {
                            if (event.date.isNotBlank()) append(event.date)
                            if (event.timeFrom.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(event.timeFrom)
                            }
                            if (event.timeTo.isNotBlank()) {
                                append(" – ")
                                append(event.timeTo)
                            }
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onQuickScan,
                        modifier = Modifier.testTag("quick_scan_event_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = strings.scanDoorBtn,
                            tint = RoyalGold
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_event_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.delete,
                            tint = CrimsonError.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = RoyalGoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.guestsLabel}: ${item.totalGuests}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldAttended,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${strings.attendedLabel}: ${item.attendedGuests} ($attendancePercent%)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldAttended
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { attendanceProgress },
                color = EmeraldAttended,
                trackColor = ObsidianNight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun HomeStatCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightCard,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, RoyalGold.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
