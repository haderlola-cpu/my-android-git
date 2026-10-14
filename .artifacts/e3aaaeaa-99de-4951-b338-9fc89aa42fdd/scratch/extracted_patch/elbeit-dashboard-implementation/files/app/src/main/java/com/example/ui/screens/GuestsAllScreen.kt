package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventWithCounts
import com.example.data.GuestEntity
import com.example.ui.components.LuxCard
import com.example.ui.components.StarBadge
import com.example.ui.components.arabesqueLattice
import com.example.ui.components.goldBottomLine
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings

/** تبويب المدعوين: كل ضيوف كل المناسبات في مكان واحد، مع بحث وفلتر. للعرض والفتح فقط. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestsAllScreen(
    guests: List<GuestEntity>,
    events: List<EventWithCounts>,
    onOpenEvent: (Long) -> Unit
) {
    val ar = LocalAppStrings.current.isArabic
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) } // 0 الكل، 1 حاضر، 2 لم يصل

    val eventNames = remember(events) { events.associate { it.event.id to it.event.name } }
    val visibleGuests = remember(guests) { guests.filter { !it.isDeleted } }

    val filtered by remember(visibleGuests, query, filter) {
        derivedStateOf {
            val q = query.trim().lowercase()
            visibleGuests.filter { g ->
                val okFilter = when (filter) {
                    1 -> g.attended
                    2 -> !g.attended
                    else -> true
                }
                val okQuery = q.isEmpty() ||
                    g.name.lowercase().contains(q) ||
                    g.code.contains(q) ||
                    g.phone.contains(q)
                okFilter && okQuery
            }
        }
    }
    val attendedCount = visibleGuests.count { it.attended }

    Scaffold(
        containerColor = ObsidianNight,
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Column {
                        Text(
                            text = if (ar) "المدعوون" else "Guests",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = RoyalGoldLight
                        )
                        Text(
                            text = if (ar) "كل ضيوف كل المناسبات" else "All guests across events",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSilver
                        )
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
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (ar) "بحث بالاسم أو الكود أو الموبايل..." else "Search by name, code or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalGold) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
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
                    .padding(top = 12.dp)
                    .testTag("all_guests_search")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    text = (if (ar) "الكل" else "All") + " (" + visibleGuests.size + ")",
                    selected = filter == 0,
                    modifier = Modifier.weight(1f)
                ) { filter = 0 }
                FilterPill(
                    text = (if (ar) "حاضر" else "In") + " (" + attendedCount + ")",
                    selected = filter == 1,
                    modifier = Modifier.weight(1f)
                ) { filter = 1 }
                FilterPill(
                    text = (if (ar) "لم يصل" else "Pending") + " (" + (visibleGuests.size - attendedCount) + ")",
                    selected = filter == 2,
                    modifier = Modifier.weight(1f)
                ) { filter = 2 }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (ar) "لا يوجد ضيوف مطابقون" else "No matching guests",
                        color = MutedSilver,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                ) {
                    items(filtered, key = { it.id }) { g ->
                        GuestRow(g, eventNames[g.eventId].orEmpty(), ar) { onOpenEvent(g.eventId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) RoyalGold else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) ObsidianNight else MutedSilver,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun GuestRow(g: GuestEntity, eventName: String, ar: Boolean, onClick: () -> Unit) {
    LuxCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("all_guest_" + g.id),
        cornerRadius = 16.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StarBadge(size = 38.dp, filled = false) {
                Text(
                    text = g.name.trim().take(1),
                    color = RoyalGoldLight,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = g.name,
                    color = IvoryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = eventName + "  ·  #" + g.code,
                    color = MutedSilver,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val c = if (g.attended) EmeraldAttended else AmberWarning
            Text(
                text = if (g.attended) (if (ar) "حاضر" else "In") else (if (ar) "لم يصل" else "Pending"),
                color = c,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(c.copy(alpha = 0.18f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}
