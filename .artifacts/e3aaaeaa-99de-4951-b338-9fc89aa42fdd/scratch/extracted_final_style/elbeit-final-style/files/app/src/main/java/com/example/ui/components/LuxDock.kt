package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight

data class DockItem(val key: String, val label: String, val icon: ImageVector)

/** شريط تنقل سفلي عائم بحد ذهبي، والعنصر النشط بخط ذهبي صغير فوقه. */
@Composable
fun LuxDock(
    items: List<DockItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightSurface.copy(alpha = 0.97f), shape)
                .border(1.dp, RoyalGold.copy(alpha = 0.45f), shape)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.key) }
                        .testTag("dock_" + item.key),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .background(
                                if (selected) RoyalGold else Color.Transparent,
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) RoyalGoldLight else IvoryText.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(22.dp)
                    )
                    Text(
                        text = item.label,
                        color = if (selected) RoyalGoldLight else IvoryText.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
