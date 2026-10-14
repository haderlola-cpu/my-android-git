package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldDark
import com.example.ui.theme.RoyalGoldLight
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * مكونات الستايل الفاخر (إضافة جديدة بالكامل، لا تعدّل أي مكوّن قديم).
 * أي شاشة لا تستدعيها لن تتأثر إطلاقاً.
 */

/** نجمة ثمانية (رمز إسلامي) كـ Shape للقص. */
val Star8Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = min(size.width, size.height) / 2f
    val inner = outer * 0.78f
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(i * 22.5 - 90.0)
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** شارة نجمية ذهبية للأيقونات. filled=false تعطي نسخة شفافة بحد ذهبي هادئ. */
@Composable
fun StarBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    filled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = if (filled) {
        Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold, RoyalGoldDark))
    } else {
        Brush.linearGradient(listOf(RoyalGold.copy(alpha = 0.22f), RoyalGold.copy(alpha = 0.22f)))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(Star8Shape)
            .background(bg),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun DrawScope.drawStar8(cx: Float, cy: Float, outer: Float, color: Color, strokeWidth: Float) {
    val inner = outer * 0.62f
    val p = Path()
    for (i in 0 until 16) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(i * 22.5 - 90.0)
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color, style = Stroke(width = strokeWidth))
}

/** خلفية زخرفية هندسية خفيفة (شبكة نجوم). تُرسم خلف المحتوى فقط. */
fun Modifier.arabesqueLattice(
    color: Color = RoyalGold,
    alpha: Float = 0.07f,
    tile: Dp = 46.dp
): Modifier = this.drawBehind {
    val t = tile.toPx()
    val lineColor = color.copy(alpha = alpha)
    val sw = 0.8.dp.toPx()
    val cols = (size.width / t).toInt() + 1
    val rows = (size.height / t).toInt() + 1
    for (r in 0..rows) {
        for (c in 0..cols) {
            drawStar8(c * t + t / 2f, r * t + t / 2f, t * 0.42f, lineColor, sw)
        }
    }
}

/** كارت بزوايا ذهبية (أعلى اليمين وأسفل اليسار). */
@Composable
fun LuxPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .background(
                Brush.linearGradient(listOf(MidnightCard.copy(alpha = 0.95f), EmeraldDeep.copy(alpha = 0.90f))),
                shape
            )
            .border(1.dp, RoyalGold.copy(alpha = 0.30f), shape)
            .drawBehind {
                val r = cornerRadius.toPx()
                val l = r + 14.dp.toPx()
                val w = size.width
                val h = size.height
                val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                val tr = Path().apply {
                    moveTo(w - l, 0f)
                    lineTo(w - r, 0f)
                    arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
                    lineTo(w, l)
                }
                val bl = Path().apply {
                    moveTo(l, h)
                    lineTo(r, h)
                    arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
                    lineTo(0f, h - l)
                }
                drawPath(tr, RoyalGold, style = st)
                drawPath(bl, RoyalGold, style = st)
            },
        content = content
    )
}

/** لوجو القوس العربي داخل نجمة ذهبية كبيرة. */
@Composable
fun LuxLogoBadge(modifier: Modifier = Modifier, badgeSize: Dp = 86.dp) {
    StarBadge(modifier = modifier, size = badgeSize) {
        Canvas(modifier = Modifier.size(badgeSize * 0.58f)) {
            val w = this.size.width
            val h = this.size.height
            val outer = Path().apply {
                moveTo(w * 0.08f, h * 0.94f)
                lineTo(w * 0.08f, h * 0.50f)
                quadraticBezierTo(w * 0.08f, h * 0.16f, w * 0.50f, h * 0.06f)
                quadraticBezierTo(w * 0.92f, h * 0.16f, w * 0.92f, h * 0.50f)
                lineTo(w * 0.92f, h * 0.94f)
            }
            drawPath(outer, EmeraldDeep, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
            val inner = Path().apply {
                moveTo(w * 0.26f, h * 0.94f)
                lineTo(w * 0.26f, h * 0.54f)
                quadraticBezierTo(w * 0.26f, h * 0.32f, w * 0.50f, h * 0.23f)
                quadraticBezierTo(w * 0.74f, h * 0.32f, w * 0.74f, h * 0.54f)
                lineTo(w * 0.74f, h * 0.94f)
            }
            drawPath(inner, EmeraldDeep, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
            val door = Path().apply {
                moveTo(w * 0.41f, h * 0.94f)
                lineTo(w * 0.41f, h * 0.62f)
                quadraticBezierTo(w * 0.41f, h * 0.49f, w * 0.50f, h * 0.44f)
                quadraticBezierTo(w * 0.59f, h * 0.49f, w * 0.59f, h * 0.62f)
                lineTo(w * 0.59f, h * 0.94f)
                close()
            }
            drawPath(door, EmeraldDeep.copy(alpha = 0.9f))
        }
    }
}

/** فاصل زخرفي: خط ذهبي متلاشي مع معين في المنتصف. */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, RoyalGold)))
        )
        Text(
            text = "◆",
            color = RoyalGold,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(RoyalGold, Color.Transparent)))
        )
    }
}

/** خط أفق مساجد (قباب ومآذن) بخط ذهبي رفيع، للأسفل كزخرفة. */
@Composable
fun MosqueSkyline(
    modifier: Modifier = Modifier,
    color: Color = RoyalGold,
    alpha: Float = 0.45f
) {
    Canvas(modifier = modifier) {
        val w = this.size.width
        val h = this.size.height
        val st = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
        val c = color.copy(alpha = alpha)

        fun dome(cx: Float, half: Float, topY: Float) {
            val midY = topY + half * 1.2f
            val p = Path().apply {
                moveTo(cx - half, h)
                lineTo(cx - half, midY)
                quadraticBezierTo(cx - half, topY, cx, topY)
                quadraticBezierTo(cx + half, topY, cx + half, midY)
                lineTo(cx + half, h)
            }
            drawPath(p, c, style = st)
            drawLine(c, androidx.compose.ui.geometry.Offset(cx, topY), androidx.compose.ui.geometry.Offset(cx, topY - 9.dp.toPx()), strokeWidth = st.width)
        }

        fun minaret(cx: Float) {
            val hw = 6.dp.toPx()
            val p = Path().apply {
                moveTo(cx - hw, h)
                lineTo(cx - hw, h * 0.32f)
                lineTo(cx, h * 0.12f)
                lineTo(cx + hw, h * 0.32f)
                lineTo(cx + hw, h)
            }
            drawPath(p, c, style = st)
        }

        dome(w * 0.14f, w * 0.09f, h * 0.38f)
        minaret(w * 0.30f)
        dome(w * 0.50f, w * 0.15f, h * 0.20f)
        minaret(w * 0.70f)
        dome(w * 0.86f, w * 0.09f, h * 0.38f)
    }
}

/** يرسم زوايا ذهبية (أعلى اليمين وأسفل اليسار) فوق المحتوى، من غير ما يغيّر الخلفية أو الحدود الموجودة. */
fun Modifier.goldCorners(cornerRadius: Dp = 18.dp): Modifier = this.drawWithContent {
    drawContent()
    val r = cornerRadius.toPx()
    val l = r + 12.dp.toPx()
    val w = this.size.width
    val h = this.size.height
    val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val tr = Path().apply {
        moveTo(w - l, 0f)
        lineTo(w - r, 0f)
        arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
        lineTo(w, l)
    }
    val bl = Path().apply {
        moveTo(l, h)
        lineTo(r, h)
        arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
        lineTo(0f, h - l)
    }
    drawPath(tr, RoyalGold, style = st)
    drawPath(bl, RoyalGold, style = st)
}

/**
 * بديل مباشر لـ Card بنفس الوسائط (modifier / shape / colors)،
 * يضيف الزوايا الذهبية فقط. كل السلوك القديم (الضغط، الحدود، الـ testTag) يفضل زي ما هو.
 */
@Composable
fun LuxCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: CardColors = CardDefaults.cardColors(containerColor = MidnightCard),
    cornerRadius: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.goldCorners(cornerRadius),
        shape = shape,
        colors = colors,
        content = content
    )
}

/** خط ذهبي رفيع متلاشي أسفل الشريط العلوي. */
fun Modifier.goldBottomLine(): Modifier = this.drawWithContent {
    drawContent()
    val lineH = 1.dp.toPx()
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, RoyalGold, Color.Transparent)),
        topLeft = androidx.compose.ui.geometry.Offset(0f, this.size.height - lineH),
        size = androidx.compose.ui.geometry.Size(this.size.width, lineH)
    )
}

/** إطار المسح: أربع زوايا ذهبية فوق الكاميرا (للعرض فقط، لا يؤثر على قراءة الكود). */
@Composable
fun ScanFrameOverlay(modifier: Modifier = Modifier, inset: Dp = 22.dp, arm: Dp = 34.dp) {
    Canvas(modifier = modifier) {
        val i = inset.toPx()
        val a = arm.toPx()
        val w = this.size.width
        val h = this.size.height
        val st = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            val path = Path().apply {
                moveTo(x + dx * a, y)
                lineTo(x, y)
                lineTo(x, y + dy * a)
            }
            drawPath(path, RoyalGold, style = st)
        }
        corner(i, i, 1f, 1f)
        corner(w - i, i, -1f, 1f)
        corner(i, h - i, 1f, -1f)
        corner(w - i, h - i, -1f, -1f)
    }
}

/** عداد دائري (حلقة تقدم) بحواف مستديرة؛ المحتوى يُرسم في المنتصف. */
@Composable
fun GoldRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 72.dp,
    strokeWidth: Dp = 8.dp,
    ringColor: Color = RoyalGold,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val topLeft = androidx.compose.ui.geometry.Offset(sw / 2f, sw / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** زر أيقونة داخل نجمة ذهبية شفافة (بديل IconButton في الأشرطة العلوية). منطقة اللمس 44dp. */
@Composable
fun LuxIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides RoyalGoldLight) {
            StarBadge(size = 32.dp, filled = false, content = content)
        }
    }
}

/** تذكرة التاريخ: اليوم بخط كبير والشهر تحته، وخط ذهبي منقط على جهة النص. */
@Composable
fun LuxDateTicket(dateText: String, isArabic: Boolean, modifier: Modifier = Modifier) {
    val m = Regex("(\\d{4})\\D+(\\d{1,2})\\D+(\\d{1,2})").find(dateText)
    val day = m?.groupValues?.get(3)?.toIntOrNull()
    val month = m?.groupValues?.get(2)?.toIntOrNull()
    val months = if (isArabic) {
        listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    } else {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    Column(
        modifier = modifier
            .width(54.dp)
            .drawBehind {
                val x = if (layoutDirection == LayoutDirection.Rtl) 0f else this.size.width
                drawLine(
                    color = RoyalGold.copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, this.size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day?.toString() ?: "—",
            color = RoyalGoldLight,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp
        )
        Text(
            text = if (month != null && month in 1..12) months[month - 1] else "",
            color = RoyalGoldLight.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}
