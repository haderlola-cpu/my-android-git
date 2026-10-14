package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import kotlin.math.cos
import kotlin.math.sin

/**
 * Section 3.1 Logo specification:
 * "اللوجو: بيت فوقه نجمة احتفال وجواه باركود صغير، وتحته الاسم وسطر «تنظيم الحفلات والمناسبات»."
 */
@Composable
fun ElbeitBrandLogo(
    subtitle: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 88.dp,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(MidnightCard, ObsidianNight)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold)),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(iconSize * 0.78f)) {
                val w = size.width
                val h = size.height

                // 1. Celebration Star at the top peak
                val starCenterX = w * 0.5f
                val starCenterY = h * 0.15f
                val outerR = w * 0.11f
                val innerR = outerR * 0.45f
                val starPath = Path()
                for (i in 0 until 16) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 360.0 / 16.0) - 90.0)
                    val px = starCenterX + (r * cos(angle)).toFloat()
                    val py = starCenterY + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(path = starPath, color = RoyalGoldLight)

                // Sparkle dots around the star
                drawCircle(color = RoyalGold, radius = w * 0.022f, center = Offset(w * 0.28f, h * 0.14f))
                drawCircle(color = RoyalGold, radius = w * 0.022f, center = Offset(w * 0.72f, h * 0.14f))

                // 2. Stylized House Roof & Walls
                val housePath = Path().apply {
                    moveTo(w * 0.14f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.26f)
                    lineTo(w * 0.86f, h * 0.50f)
                }
                drawPath(
                    path = housePath,
                    color = RoyalGold,
                    style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val wallsPath = Path().apply {
                    moveTo(w * 0.23f, h * 0.47f)
                    lineTo(w * 0.23f, h * 0.88f)
                    lineTo(w * 0.77f, h * 0.88f)
                    lineTo(w * 0.77f, h * 0.47f)
                }
                drawPath(
                    path = wallsPath,
                    color = RoyalGold,
                    style = Stroke(width = w * 0.048f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 3. Mini QR Barcode inside the house
                val qrSize = w * 0.34f
                val qrLeft = (w - qrSize) / 2f
                val qrTop = h * 0.48f
                val cell = qrSize / 5f

                // Finder pattern top-left
                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft, qrTop),
                    size = Size(cell * 2f, cell * 2f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
                // Finder pattern top-right
                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft + cell * 3f, qrTop),
                    size = Size(cell * 2f, cell * 2f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
                // Finder pattern bottom-left
                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft, qrTop + cell * 3f),
                    size = Size(cell * 2f, cell * 2f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
                // Data cells bottom-right
                drawRect(
                    color = RoyalGold,
                    topLeft = Offset(qrLeft + cell * 3f, qrTop + cell * 3f),
                    size = Size(cell * 0.9f, cell * 0.9f)
                )
                drawRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft + cell * 4.1f, qrTop + cell * 3f),
                    size = Size(cell * 0.9f, cell * 2f)
                )
                drawRect(
                    color = RoyalGold,
                    topLeft = Offset(qrLeft + cell * 3f, qrTop + cell * 4.1f),
                    size = Size(cell * 0.9f, cell * 0.9f)
                )
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "elbeit elaraby",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = RoyalGoldLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryText.copy(alpha = 0.82f),
                modifier = Modifier
                    .background(RoyalGold.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}
