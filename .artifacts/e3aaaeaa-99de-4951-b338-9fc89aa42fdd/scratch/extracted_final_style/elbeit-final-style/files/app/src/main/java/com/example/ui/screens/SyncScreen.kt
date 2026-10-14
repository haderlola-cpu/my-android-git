package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncQueueEntity
import com.example.ui.components.GoldRing
import com.example.ui.components.LuxCard
import com.example.ui.components.arabesqueLattice
import com.example.ui.components.goldBottomLine
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldAttended
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import com.example.util.LocalAppStrings

/** تبويب المزامنة: حالة طابور المزامنة الفعلي + زر مزامنة الآن (يستخدم SyncWorker الموجود). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    queue: List<SyncQueueEntity>,
    onSyncNow: () -> Unit
) {
    val ar = LocalAppStrings.current.isArabic
    val failed = queue.filter { it.retryCount > 0 }
    val pending = queue.size - failed.size
    val allSynced = queue.isEmpty()

    Scaffold(
        containerColor = ObsidianNight,
        topBar = {
            TopAppBar(
                modifier = Modifier.goldBottomLine(),
                title = {
                    Column {
                        Text(
                            text = if (ar) "المزامنة" else "Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = RoyalGoldLight
                        )
                        Text(
                            text = if (ar) "حفظ بياناتك على السحابة" else "Back up your data to the cloud",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LuxCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_status_card"),
                cornerRadius = 20.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldRing(
                        progress = if (allSynced) 1f else 0.7f,
                        ringSize = 104.dp,
                        strokeWidth = 8.dp,
                        ringColor = if (allSynced) EmeraldAttended else RoyalGold
                    ) {
                        Icon(
                            imageVector = if (allSynced) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (allSynced) EmeraldAttended else RoyalGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (allSynced) {
                            if (ar) "كل بياناتك متزامنة" else "Everything is synced"
                        } else {
                            if (ar) "عناصر بانتظار المزامنة: ${queue.size}" else "Items waiting to sync: ${queue.size}"
                        },
                        color = IvoryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (ar) "تتم المزامنة تلقائياً عند توفر الإنترنت" else "Sync runs automatically when you are online",
                        color = MutedSilver,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SyncStat(Modifier.weight(1f), queue.size.toString(), if (ar) "الإجمالي" else "Total", IvoryText)
                SyncStat(Modifier.weight(1f), pending.toString(), if (ar) "معلّق" else "Pending", AmberWarning)
                SyncStat(Modifier.weight(1f), failed.size.toString(), if (ar) "تعثّر" else "Failed", if (failed.isEmpty()) IvoryText else CrimsonError)
            }

            if (failed.isNotEmpty()) {
                LuxCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (ar) "آخر الأخطاء" else "Latest errors",
                            color = RoyalGoldLight,
                            fontWeight = FontWeight.ExtraBold
                        )
                        failed.take(5).forEach { item ->
                            Text(
                                text = item.entityType + ": " + (item.lastError ?: ""),
                                color = MutedSilver,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSyncNow,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianNight),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sync_now_button")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (ar) "مزامنة الآن" else "Sync now", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SyncStat(modifier: Modifier, value: String, label: String, valueColor: Color) {
    LuxCard(modifier = modifier, cornerRadius = 16.dp, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(label, color = MutedSilver, fontSize = 11.sp)
        }
    }
}
