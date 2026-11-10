package com.muddassir.safina

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

const val TWO_PI = 6.2831855f

fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** 0..1 progress of p inside [from, to] */
fun seg(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
fun smooth(t: Float) = t * t * (3f - 2f * t)

/** -1..1 continuous wave for idle motion. */
fun sway(time: Float, phase: Float = 0f) = sin((time + phase) * TWO_PI)

private fun rad(deg: Float) = deg * PI.toFloat() / 180f

data class ScenePalette(val accent: Color, val glow: Color)

/** Everything a scene needs for one frame. A scene never knows about counts or targets. */
data class SceneFrame(
    val progress: Float,
    val time: Float,
    val pulse: Float = 1f,
    val ripple: Float = 1f,
    val shimmer: Float = 0f,
    val golden: Float = 0f,
    val celebrate: Float = 0f,
    val calm: Boolean = false
)

interface Scene {
    val id: String
    val palette: ScenePalette
    val tapAnchor: Offset
    val labels: List<String>
    fun textColor(progress: Float): Color
    fun DrawScope.draw(frame: SceneFrame)
}

/* ---------- shared drawing helpers ---------- */

fun DrawScope.softGlow(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (alpha <= 0.001f || radius <= 0f) return
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
            center = center, radius = radius
        ),
        radius = radius, center = center
    )
}

fun DrawScope.featherPatch(color: Color, center: Offset, radius: Float) {
    if (radius <= 1f) return
    drawCircle(
        Brush.radialGradient(
            0f to color, 0.65f to color, 1f to color.copy(alpha = 0f),
            center = center, radius = radius
        ),
        radius = radius, center = center
    )
}

fun DrawScope.cloud(x: Float, y: Float, s: Float, c: Color) {
    drawCircle(c, s * 0.50f, Offset(x, y))
    drawCircle(c, s * 0.68f, Offset(x + s * 0.55f, y - s * 0.22f))
    drawCircle(c, s * 0.52f, Offset(x + s * 1.15f, y - s * 0.02f))
    drawOval(c, Offset(x - s * 0.55f, y - s * 0.05f), Size(s * 2.3f, s * 0.62f))
}

fun DrawScope.twinkleStar(x: Float, y: Float, r: Float, alpha: Float) {
    if (alpha <= 0.01f) return
    drawCircle(Color.White.copy(alpha = alpha), r, Offset(x, y))
    drawLine(Color.White.copy(alpha = alpha * 0.55f), Offset(x - r * 2.6f, y), Offset(x + r * 2.6f, y), r * 0.35f)
    drawLine(Color.White.copy(alpha = alpha * 0.55f), Offset(x, y - r * 2.6f), Offset(x, y + r * 2.6f), r * 0.35f)
}

/** A soft, layered cloud bank used for mist. */
fun DrawScope.mistBand(y: Float, h: Float, color: Color, alpha: Float) {
    if (alpha <= 0.01f) return
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
            startY = y - h, endY = y + h
        ),
        topLeft = Offset(0f, y - h), size = Size(size.width, h * 2f)
    )
}

/** Reusable triangle-ish hill silhouette helper. */
fun hillPath(path: androidx.compose.ui.graphics.Path, w: Float, base: Float, amp: Float, phase: Float): androidx.compose.ui.graphics.Path {
    path.reset()
    path.moveTo(0f, base)
    path.cubicTo(w * 0.20f, base - amp, w * 0.36f + phase, base - amp * 0.55f, w * 0.52f, base - amp * 0.2f)
    path.cubicTo(w * 0.70f, base + amp * 0.2f, w * 0.86f - phase, base - amp * 0.7f, w, base - amp * 0.25f)
    path.lineTo(w, base + amp); path.lineTo(0f, base + amp); path.close()
    return path
}

/** Degrees helper kept private to this file's uses. */
internal fun arcRadians(deg: Float) = rad(deg)
