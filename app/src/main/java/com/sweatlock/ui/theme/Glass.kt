package com.sweatlock.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft pastel gradient with colour blobs: the "light source" that makes the glass cards read as glass. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFE3ECFF), Color(0xFFF1E7FF), Color(0xFFFFEEE4)))
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun blob(c: Color, cx: Float, cy: Float, r: Float) {
                val center = Offset(cx, cy)
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.55f), Color.Transparent), center = center, radius = r),
                    radius = r, center = center
                )
            }
            blob(Color(0xFF6EA8FF), size.width * 0.10f, size.height * 0.12f, size.width * 0.85f)
            blob(Color(0xFFFF8FD0), size.width * 0.95f, size.height * 0.45f, size.width * 0.75f)
            blob(Color(0xFF7CE8C8), size.width * 0.20f, size.height * 0.92f, size.width * 0.85f)
        }
        content()
    }
}

/** Frosted card: translucent white gradient + bright hairline border + soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .shadow(10.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x1F000000))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.38f))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.25f))), shape),
        content = content
    )
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(
                if (primary) Brush.linearGradient(listOf(Color(0xFF4DA3FF), Accent))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.45f)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.8f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) Color.White else Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RoundGlassButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.75f))
            .border(1.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 20.sp, color = Accent, fontWeight = FontWeight.Medium) }
}

@Composable
fun GlassTabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val tabs = listOf("\uD83C\uDFE0" to "Home", "\uD83D\uDCF5" to "Apps", "\uD83D\uDCCA" to "Stats", "\u2699\uFE0F" to "Settings")
    GlassCard(
        modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(36.dp)
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabs.forEachIndexed { i, (emoji, label) ->
                Column(
                    Modifier.clip(RoundedCornerShape(28.dp))
                        .background(if (i == selected) Accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (i == selected) Accent else Muted)
                }
            }
        }
    }
}
