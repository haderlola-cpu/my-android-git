package com.example.ui.screens

import com.example.ui.components.LuxIconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EventWithCounts
import com.example.ui.components.GoldRing
import com.example.ui.components.LuxCard
import com.example.ui.components.LuxLogoBadge
import com.example.ui.components.arabesqueLattice
import com.example.ui.components.goldBottomLine
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings
import java.util.Calendar

private val MONTHS_AR = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
private val MONTHS_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private data class ParsedDate(val year: Int, val month: Int, val day: Int)

private fun parseDate(s: String): ParsedDate? {
    val m = Regex("(\\d{4})\\D+(\\d{1,2})\\D+(\\d{1,2})").find(s) ?: return null
    val (y, mo, d) = m.destructured
    val month = mo.toInt()
    val day = d.toInt()
    if (month !in 1..12 || day !in 1..31) return null
    return ParsedDate(y.toInt(), month, day)
}

private fun daysUntil(s: String): Int? {
    val p = parseDate(s) ?: return null
    val target = Calendar.getInstance().apply {
        clear()
        set(p.year, p.month - 1, p.day)
    }.timeInMillis
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((target - today) / 86_400_000L).toInt()
}

private fun pickNextEvent(events: List<EventWithCounts>): EventWithCounts? {
    val upcoming = events.mapNotNull { e ->
        val d = daysUntil(e.event.date)
        if (d != null && d >= 0) e to d else null
    }
    return upcoming.minByOrNull { it.second }?.first ?: events.firstOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    events: List<EventWithCounts>,
    onOpenEvent: (Long) -> Unit,
    onCreateNewEvent: () -> Unit,
    onQuickScanEvent: (Long) -> Unit,
    onOpenEventsList: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val strings = LocalAppStrings.current
    val ar = strings.isArabic
    val totalGuests = events.sumOf { it.totalGuests }
    val attendedTotal = events.sumOf { it.attendedGuests }
    val next = remember(events) { pickNextEvent(events) }

    Scaffold(
        containerColor = ObsidianNight,
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LuxLogoBadge(badgeSize = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (ar) "لوحة التحكم" else "Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = RoyalGoldLight
                            )
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSilver
                            )
                        }
                    }
                },
                actions = {
                    LuxIconButton(onClick = onToggleLanguage, modifier = Modifier.testTag("dashboard_language_button")) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = RoyalGold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // بطاقة المناسبة القادمة + عداد الحضور
            LuxCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_next_event"),
                cornerRadius = 20.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                if (next == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(32.dp))
                        Text(
                            text = if (ar) "لا توجد مناسبات بعد" else "No events yet",
                            color = IvoryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (ar) "ابدأ بإنشاء أول مناسبة وتصميم دعواتها" else "Create your first event and design its invitations",
                            color = MutedSilver,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val pct = if (next.totalGuests == 0) 0 else (next.attendedGuests * 100 / next.totalGuests)
                    val days = daysUntil(next.event.date)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEvent(next.event.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GoldRing(progress = pct / 100f, ringSize = 76.dp) {
                            Text(text = "$pct%", color = RoyalGoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val tag = when {
                                days == null -> if (ar) "المناسبة" else "Event"
                                days < 0 -> if (ar) "مناسبة منتهية" else "Past event"
                                days == 0 -> if (ar) "المناسبة اليوم" else "Today"
                                else -> if (ar) "المناسبة القادمة · بعد $days يوم" else "Next event · in $days days"
                            }
                            Text(text = tag, color = RoyalGold, fontSize = 11.sp)
                            Text(
                                text = next.event.name,
                                color = IvoryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = (if (ar) "الحضور: " else "Attended: ") + "${next.attendedGuests} / ${next.totalGuests}",
                                color = MutedSilver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // الأرقام الإجمالية
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DashKpi(Modifier.weight(1f), Icons.Default.Event, events.size.toString(), if (ar) "المناسبات" else "Events", IvoryText)
                DashKpi(Modifier.weight(1f), Icons.Default.Groups, totalGuests.toString(), if (ar) "المدعوون" else "Guests", IvoryText)
                DashKpi(Modifier.weight(1f), Icons.Default.CheckCircle, attendedTotal.toString(), if (ar) "الحاضرون" else "Attended", EmeraldAttended)
            }

            // إجراءات سريعة
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCreateNewEvent,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianNight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_new_event")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (ar) "مناسبة جديدة" else "New event", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = { next?.let { onQuickScanEvent(it.event.id) } },
                    enabled = next != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_quick_scan")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (ar) "مسح سريع" else "Quick scan", color = RoyalGoldLight, fontWeight = FontWeight.Bold)
                }
            }

            // آخر المناسبات
            if (events.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ar) "المناسبات" else "Events",
                        color = RoyalGoldLight,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenEventsList) {
                        Text(if (ar) "عرض الكل" else "View all", color = RoyalGold)
                    }
                }
                events.take(3).forEach { e ->
                    DashEventRow(e, ar) { onOpenEvent(e.event.id) }
                }
            }
        }
    }
}

@Composable
private fun DashKpi(modifier: Modifier, icon: ImageVector, value: String, label: String, valueColor: Color) {
    LuxCard(modifier = modifier, cornerRadius = 16.dp, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(20.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(label, color = MutedSilver, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DashEventRow(e: EventWithCounts, ar: Boolean, onClick: () -> Unit) {
    val p = parseDate(e.event.date)
    val pct = if (e.totalGuests == 0) 0 else (e.attendedGuests * 100 / e.totalGuests)
    LuxCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_event_" + e.event.id),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                Text(
                    text = p?.day?.toString() ?: "—",
                    color = RoyalGoldLight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    text = p?.let { (if (ar) MONTHS_AR else MONTHS_EN)[it.month - 1] } ?: "",
                    color = MutedSilver,
                    fontSize = 10.sp
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = e.event.name,
                    color = IvoryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (if (ar) "الضيوف: " else "Guests: ") + e.totalGuests + "  ·  " + pct + "%",
                    color = MutedSilver,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = RoyalGold)
        }
    }
}
