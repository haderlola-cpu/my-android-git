package com.example.ui.screens.reception

import com.example.ui.components.LuxIconButton
import com.example.ui.components.goldBottomLine
import com.example.ui.components.LuxCard
import com.example.ui.components.arabesqueLattice
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EventEntity
import com.example.data.GuestEntity
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.EmeraldAttendedContainer
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceptionGuestListScreen(
    event: EventEntity,
    guests: List<GuestEntity>,
    totalCount: Int,
    attendedCount: Int,
    notArrivedCount: Int,
    searchQuery: String,
    selectedFilter: GuestFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (GuestFilter) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Column {
                        Text(
                            text = if (strings.isArabic) "سجل ضيوف البوابة" else "Gate Guest Roster",
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
                    LuxIconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reception_guest_list_back_btn")
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
                .background(ObsidianNight)
                .arabesqueLattice()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Live Counters Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReceptionStatPill(
                    label = strings.totalGuestsStat,
                    count = totalCount,
                    color = RoyalGold,
                    modifier = Modifier.weight(1f)
                )
                ReceptionStatPill(
                    label = strings.attendedStat,
                    count = attendedCount,
                    color = EmeraldAttended,
                    modifier = Modifier.weight(1f)
                )
                ReceptionStatPill(
                    label = strings.notArrivedStat,
                    count = notArrivedCount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Text Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(if (strings.isArabic) "بحث بالاسم أو التسلسل أو كود الباركود..." else "Search by name, seq, or code...")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = RoyalGold)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("reception_guest_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips (All, Attended, Not Arrived)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == GuestFilter.ALL,
                    onClick = { onFilterChange(GuestFilter.ALL) },
                    label = { Text(strings.filterAll) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RoyalGold,
                        selectedLabelColor = ObsidianNight
                    ),
                    modifier = Modifier.testTag("reception_filter_all")
                )
                FilterChip(
                    selected = selectedFilter == GuestFilter.ATTENDED,
                    onClick = { onFilterChange(GuestFilter.ATTENDED) },
                    label = { Text(strings.filterAttended) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldAttended,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("reception_filter_attended")
                )
                FilterChip(
                    selected = selectedFilter == GuestFilter.NOT_ARRIVED,
                    onClick = { onFilterChange(GuestFilter.NOT_ARRIVED) },
                    label = { Text(strings.filterNotArrived) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MidnightCard,
                        selectedLabelColor = RoyalGoldLight
                    ),
                    modifier = Modifier.testTag("reception_filter_not_arrived")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guest Cards List
            if (guests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = RoyalGold.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (strings.isArabic) "لا توجد نتائج مطابقة" else "No matching guests found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(guests, key = { it.id }) { guest ->
                        ReceptionGuestRow(
                            guest = guest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceptionStatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MidnightCard,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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

@Composable
private fun ReceptionGuestRow(
    guest: GuestEntity
) {
    val strings = LocalAppStrings.current
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    LuxCard(
        cornerRadius = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(
                width = 1.dp,
                color = if (guest.attended) EmeraldAttended.copy(alpha = 0.4f) else RoyalGold.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("reception_guest_row_${guest.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sequence Number Badge
                Surface(
                    color = RoyalGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${guest.sequenceNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RoyalGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = guest.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "كود: ${guest.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Attendance Chip / Button (strictly NO phone number or edit/delete options)
            if (guest.attended) {
                val timeStr = guest.attendedAt?.let { timeFormatter.format(Date(it)) } ?: ""
                Surface(
                    color = EmeraldAttendedContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, EmeraldAttended, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldAttended,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (timeStr.isNotBlank()) "حضر ($timeStr)" else strings.filterAttended,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            } else {
                Surface(
                    color = MidnightCard,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, RoyalGold.copy(alpha = 0.3f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = if (strings.isArabic) "لم يصل بعد" else "Not Arrived",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalGold.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
