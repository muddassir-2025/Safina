package com.muddassir.safina

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class SafinaPalette(
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val warm: Color,
    val surface: Color,
    val field: Color,
    val border: Color,
    val onInk: Color,
    val backdropTop: Color,
    val backdropMid: Color,
    val backdropBottom: Color
)

internal val LightPalette = SafinaPalette(
    ink = Color(0xFF3D453E),
    muted = Color(0xFF6C756D),
    accent = Color(0xFF5E7A62),
    warm = Color(0xFFB8875C),
    surface = Color(0xFFFFFBF1),
    field = Color.White.copy(alpha = 0.62f),
    border = Color.White.copy(alpha = 0.82f),
    onInk = Color(0xFFF6F2E6),
    backdropTop = Color(0xFFF3F0E3),
    backdropMid = Color(0xFFE7EEEA),
    backdropBottom = Color(0xFFEDE9E0)
)

internal val DarkPalette = SafinaPalette(
    ink = Color(0xFFE8E3D4),
    muted = Color(0xFFA3AA9F),
    accent = Color(0xFF91AF8C),
    warm = Color(0xFFD6A77E),
    surface = Color(0xFF1C231E),
    field = Color(0xFF252E27),
    border = Color(0xFF39433A),
    onInk = Color(0xFF15201A),
    backdropTop = Color(0xFF111613),
    backdropMid = Color(0xFF18201B),
    backdropBottom = Color(0xFF0F1411)
)

val LocalSafinaPalette = androidx.compose.runtime.staticCompositionLocalOf { LightPalette }

val SafinaInk: Color @Composable get() = LocalSafinaPalette.current.ink
val SafinaMuted: Color @Composable get() = LocalSafinaPalette.current.muted
val SafinaAccent: Color @Composable get() = LocalSafinaPalette.current.accent
val SafinaWarm: Color @Composable get() = LocalSafinaPalette.current.warm
val SafinaSurface: Color @Composable get() = LocalSafinaPalette.current.surface
val SafinaField: Color @Composable get() = LocalSafinaPalette.current.field
val SafinaBorder: Color @Composable get() = LocalSafinaPalette.current.border
val SafinaOnInk: Color @Composable get() = LocalSafinaPalette.current.onInk

/** Soft animated gradient with drifting light motes, shared by dashboard and studio. */
@Composable
fun AppBackdrop(
    time: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        val palette = LocalSafinaPalette.current
        // The glow tints are picked per theme, otherwise they wash out dark mode.
        val dark = palette.backdropTop.luminance() < 0.35f
        val glowA = if (dark) Color(0xFF31452F).copy(alpha = 0.55f)
        else Color(0xFFBFD8C4).copy(alpha = 0.42f)
        val glowB = if (dark) Color(0xFF23303C).copy(alpha = 0.55f)
        else Color(0xFFC6D8E8).copy(alpha = 0.45f)
        val mote = if (dark) Color(0xFFC8D8CB) else Color.White
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(palette.backdropTop, palette.backdropMid, palette.backdropBottom)
                )
            )
            val drift = sin(time * TWO_PI * 0.25f)
            drawCircle(
                Brush.radialGradient(
                    listOf(glowA, Color.Transparent),
                    center = Offset(size.width * (0.18f + drift * 0.05f), size.height * 0.16f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * (0.18f + drift * 0.05f), size.height * 0.16f)
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(glowB, Color.Transparent),
                    center = Offset(size.width * (0.88f - drift * 0.04f), size.height * 0.72f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * (0.88f - drift * 0.04f), size.height * 0.72f)
            )
            repeat(14) { i ->
                val phase = (time * 0.35f + i * 0.137f) % 1f
                val x = ((i * 0.173f) % 1f) * size.width +
                    sin((time + i) * TWO_PI * 0.5f) * size.width * 0.015f
                val y = size.height * (1.05f - phase * 1.1f)
                val a = (1f - phase) * if (dark) 0.09f else 0.16f
                drawCircle(mote.copy(alpha = a), size.width * 0.0022f, Offset(x, y))
            }
        }
        content()
    }
}

/** Human name for a scene id, including the user's uploaded HTML scenes. */
fun sceneTitle(sceneId: String, uploaded: List<HtmlSceneMeta>): String =
    if (sceneId.startsWith(HTML_SCENE_PREFIX)) {
        uploaded.firstOrNull { sceneId == "$HTML_SCENE_PREFIX${it.id}" }?.name ?: "Custom scene"
    } else {
        when (sceneId) {
            "garden" -> "Garden"
            "winter" -> "Winter"
            "ocean" -> "Ocean"
            "night" -> "Night"
            else -> "Scene"
        }
    }

/** Two-stop gradient used as a tiny scene swatch. */
fun sceneColors(sceneId: String): List<Color> {
    if (sceneId.startsWith(HTML_SCENE_PREFIX)) {
        val hash = abs(sceneId.hashCode())
        val hue = (hash % 360).toFloat()
        return listOf(
            Color.hsv(hue, 0.42f, 0.86f),
            Color.hsv((hue + 42f) % 360f, 0.52f, 0.62f)
        )
    }
    val p = Scenes.byId(sceneId).palette
    val deep = lerp(p.accent, Color(0xFF1F2A26), 0.45f)
    return listOf(lerp(p.accent, Color.White, 0.18f), deep)
}

@Composable
fun SceneSwatch(sceneId: String, size: Dp = 34.dp, radius: Dp = 10.dp) {
    val colors = sceneColors(sceneId)
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(colors))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.30f),
                radius = this.size.width * 0.16f,
                center = Offset(this.size.width * 0.32f, this.size.height * 0.34f)
            )
        }
    }
}

/* ---------- minimal hand-drawn glyphs (no icon dependency) ---------- */

@Composable
fun HomeGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
        val roof = Path().apply {
            moveTo(w * 0.13f, h * 0.46f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.87f, h * 0.46f)
        }
        drawPath(roof, tint, style = stroke)
        val body = Path().apply {
            moveTo(w * 0.23f, h * 0.42f)
            lineTo(w * 0.23f, h * 0.84f)
            lineTo(w * 0.77f, h * 0.84f)
            lineTo(w * 0.77f, h * 0.42f)
        }
        drawPath(body, tint, style = stroke)
    }
}

@Composable
fun MoreGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        listOf(0.25f, 0.5f, 0.75f).forEach { y ->
            drawCircle(tint, w * 0.075f, Offset(w * 0.5f, h * y))
        }
    }
}

@Composable
fun GripGlyph(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        listOf(0.28f, 0.5f, 0.72f).forEach { y ->
            drawCircle(tint, w * 0.06f, Offset(w * 0.36f, h * y))
            drawCircle(tint, w * 0.06f, Offset(w * 0.64f, h * y))
        }
    }
}

@Composable
fun ImageGlyph(tint: Color, size: Dp = 19.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.13f, h * 0.18f),
            size = Size(w * 0.74f, h * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
            style = Stroke(width = stroke)
        )
        drawCircle(tint, w * 0.075f, Offset(w * 0.34f, h * 0.36f))
        val hills = Path().apply {
            moveTo(w * 0.20f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.58f, h * 0.66f)
            lineTo(w * 0.68f, h * 0.56f)
            lineTo(w * 0.80f, h * 0.72f)
        }
        drawPath(hills, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun CloseGlyph(tint: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), w * 0.09f, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), w * 0.09f, StrokeCap.Round)
    }
}

@Composable
fun InfoGlyph(tint: Color, size: Dp = 19.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(tint, w * 0.37f, Offset(w / 2f, h / 2f), style = Stroke(width = w * 0.085f))
        drawCircle(tint, w * 0.045f, Offset(w / 2f, h * 0.34f))
        drawLine(tint, Offset(w / 2f, h * 0.48f), Offset(w / 2f, h * 0.68f), w * 0.085f, StrokeCap.Round)
    }
}

@Composable
fun PlusGlyph(tint: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), stroke.width, StrokeCap.Round)
    }
}

@Composable
fun ChevronLeftGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.18f)
            lineTo(w * 0.34f, h * 0.5f)
            lineTo(w * 0.62f, h * 0.82f)
        }
        drawPath(
            path, tint,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SearchGlyph(tint: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            tint,
            radius = w * 0.27f,
            center = Offset(w * 0.44f, h * 0.43f),
            style = Stroke(width = w * 0.09f)
        )
        drawLine(
            tint, Offset(w * 0.64f, h * 0.63f), Offset(w * 0.84f, h * 0.83f),
            w * 0.09f, StrokeCap.Round
        )
    }
}

/** Tiny inline progress bar with a rounded track. */
@Composable
fun MiniProgress(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val y = size.height / 2f
        drawLine(
            tint.copy(alpha = 0.18f), Offset(0f, y),
            Offset(size.width, y), size.height, StrokeCap.Round
        )
        drawLine(
            tint, Offset(0f, y),
            Offset((size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(size.height * 0.5f), y),
            size.height, StrokeCap.Round
        )
    }
}

/** Rounded translucent panel used across dashboard and studio. */
fun panelBrush(): Brush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.78f), Color(0xFFFFFBF1).copy(alpha = 0.62f))
)

@Composable
fun GearGlyph(tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w / 2f, h / 2f)
        val stroke = w * 0.075f
        drawCircle(tint, w * 0.30f, c, style = Stroke(width = stroke))
        drawCircle(tint, w * 0.10f, c, style = Stroke(width = stroke))
        repeat(8) { i ->
            val a = i * (TWO_PI / 8f)
            drawLine(
                tint,
                Offset(c.x + cos(a) * w * 0.32f, c.y + sin(a) * h * 0.32f),
                Offset(c.x + cos(a) * w * 0.44f, c.y + sin(a) * h * 0.44f),
                stroke, StrokeCap.Round
            )
        }
    }
}

/** Everything the options sheet needs, shared by every screen. */
data class SettingsActions(
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val darkMode: Boolean,
    val onToggleSound: () -> Unit,
    val onToggleVibration: () -> Unit,
    val onToggleDarkMode: () -> Unit,
    val onStudio: () -> Unit,
    val onQuran: () -> Unit = {},
    val onHome: (() -> Unit)? = null,
    val signedIn: Boolean = false,
    val identity: String? = null,
    val onSignIn: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onSignOut: () -> Unit = {}
)

/**
 * Translucent gear shown on every screen. Opens a floating panel with real toggles
 * for sound and vibration, plus scene studio and the way back home.
 */
@Composable
fun SettingsMenu(
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    darkIcon: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)

    Box(modifier) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF121712).copy(alpha = if (darkIcon) 0.45f else 0.10f))
                .border(
                    1.dp,
                    if (darkIcon) Color.White.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.55f),
                    CircleShape
                )
                .clickable { open = !open }
                .padding(3.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.safina_icon),
                contentDescription = "Settings",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(
                    -with(density) { 14.dp.roundToPx() },
                    topInset + with(density) { 58.dp.roundToPx() }
                ),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier
                        .width(262.dp)
                        .shadow(
                            26.dp, RoundedCornerShape(22.dp),
                            ambientColor = Color(0xFF2A302A), spotColor = Color(0xFF2A302A)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(LocalSafinaPalette.current.surface)
                        .border(1.dp, LocalSafinaPalette.current.border, RoundedCornerShape(22.dp))
                        .padding(vertical = 8.dp)
                ) {
                    ToggleRow("Sound", actions.soundEnabled) { actions.onToggleSound() }
                    ToggleRow("Vibration", actions.vibrationEnabled) { actions.onToggleVibration() }
                    ToggleRow("Dark mode", actions.darkMode) { actions.onToggleDarkMode() }
                    Box(
                        Modifier
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SafinaInk.copy(alpha = 0.07f))
                    )
                    if (actions.signedIn) {
                        ActionRow(actions.identity?.let { "Profile · $it" } ?: "Profile") {
                            open = false
                            actions.onProfile()
                        }
                    } else {
                        ActionRow("Sign in") {
                            open = false
                            actions.onSignIn()
                        }
                    }
                    Box(
                        Modifier
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SafinaInk.copy(alpha = 0.07f))
                    )
                    ActionRow("Quran") {
                        open = false
                        actions.onQuran()
                    }
                    ActionRow("Scene studio") {
                        open = false
                        actions.onStudio()
                    }
                    actions.onHome?.let { home ->
                        ActionRow("Back to dashboard") {
                            open = false
                            home()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = SafinaInk)
        Spacer(Modifier.weight(1f))
        MinimalSwitch(checked)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = SafinaInk)
        Spacer(Modifier.weight(1f))
        Text("→", fontSize = 13.sp, color = SafinaMuted.copy(alpha = 0.6f))
    }
}

/** Minimal pill switch; the whole row is the touch target. */
@Composable
fun MinimalSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "switch"
    )
    val track by animateColorAsState(
        if (checked) SafinaAccent else Color(0xFFDAD5C8), label = "track"
    )
    Box(
        modifier
            .width(38.dp)
            .height(22.dp)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            Modifier
                .offset(x = (2f + 16f * t).dp)
                .align(Alignment.CenterStart)
                .size(18.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFFFFDF7))
        )
    }
}

internal fun Size.center() = Offset(width / 2f, height / 2f)
