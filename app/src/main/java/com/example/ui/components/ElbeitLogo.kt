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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.IvoryText
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldLight
import kotlin.math.cos
import kotlin.math.sin

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
                        colors = listOf(EmeraldDeep, MaterialTheme.colorScheme.background)
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

                // 1. Heritage Crescent/Star Peak
                val starCenterX = w * 0.5f
                val starCenterY = h * 0.14f
                val outerR = w * 0.09f
                val innerR = outerR * 0.45f
                val starPath = Path()
                for (i in 0 until 12) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 360.0 / 12.0) - 90.0)
                    val px = starCenterX + (r * cos(angle)).toFloat()
                    val py = starCenterY + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(path = starPath, color = RoyalGoldLight)

                // 2. Arabian Arch Gate (القوس العربي الأندلسي)
                val archPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.88f)
                    lineTo(w * 0.18f, h * 0.52f)
                    cubicTo(
                        w * 0.18f, h * 0.30f,
                        w * 0.40f, h * 0.22f,
                        w * 0.50f, h * 0.22f
                    )
                    cubicTo(
                        w * 0.60f, h * 0.22f,
                        w * 0.82f, h * 0.30f,
                        w * 0.82f, h * 0.52f
                    )
                    lineTo(w * 0.82f, h * 0.88f)
                    close()
                }
                drawPath(
                    path = archPath,
                    color = RoyalGold,
                    style = Stroke(width = w * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Inner Arch Line
                val innerArch = Path().apply {
                    moveTo(w * 0.28f, h * 0.88f)
                    lineTo(w * 0.28f, h * 0.55f)
                    cubicTo(
                        w * 0.28f, h * 0.38f,
                        w * 0.42f, h * 0.32f,
                        w * 0.50f, h * 0.32f
                    )
                    cubicTo(
                        w * 0.58f, h * 0.32f,
                        w * 0.72f, h * 0.38f,
                        w * 0.72f, h * 0.55f
                    )
                    lineTo(w * 0.72f, h * 0.88f)
                }
                drawPath(
                    path = innerArch,
                    color = RoyalGoldLight.copy(alpha = 0.6f),
                    style = Stroke(width = w * 0.025f)
                )

                // 3. QR Barcode inside the Gate
                val qrSize = w * 0.28f
                val qrLeft = (w - qrSize) / 2f
                val qrTop = h * 0.52f
                val cell = qrSize / 4f

                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft, qrTop),
                    size = Size(cell * 1.8f, cell * 1.8f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft + cell * 2.2f, qrTop),
                    size = Size(cell * 1.8f, cell * 1.8f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
                drawRoundRect(
                    color = RoyalGoldLight,
                    topLeft = Offset(qrLeft + cell * 1.1f, qrTop + cell * 2.2f),
                    size = Size(cell * 1.8f, cell * 1.8f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = w * 0.025f)
                )
            }
        }

        if (!compact) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "البيت العربي",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = RoyalGoldLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle.ifBlank { "مناسباتك.. تنظيم أسهل" },
                style = MaterialTheme.typography.bodyMedium,
                color = IvoryText.copy(alpha = 0.9f),
                modifier = Modifier
                    .background(RoyalGold.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun ElbeitBrandLogoPreview() {
    ElbeitBrandLogo(subtitle = "مناسباتك.. تنظيم أسهل")
}
