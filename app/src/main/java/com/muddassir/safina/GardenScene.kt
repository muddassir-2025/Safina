package com.muddassir.safina

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/* ---------- helpers ---------- */
private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/** 0..1 progress of p inside [from, to] */
private fun seg(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
private fun smooth(t: Float) = t * t * (3f - 2f * t)

/* ---------- fixed random scenery (same every launch) ---------- */
private val rnd = Random(42)
private class Flower(val x: Float, val y: Float, val color: Color, val r: Float, val order: Float)
private class Blade(val x: Float, val y: Float, val len: Float, val order: Float, val dark: Boolean)
private class Crack(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

private val flowerColors = listOf(
    Color.White, Color(0xFFFFE066), Color(0xFFFF9EC4),
    Color(0xFFB69CFF), Color(0xFFFFA45C), Color(0xFFFF6B6B)
)
private val flowers = List(320) {
    Flower(
        rnd.nextFloat(), 0.46f + rnd.nextFloat() * 0.54f,
        flowerColors[rnd.nextInt(flowerColors.size)], 0.004f + rnd.nextFloat() * 0.004f, rnd.nextFloat()
    )
}
private val blades = List(450) {
    Blade(
        rnd.nextFloat(), 0.45f + rnd.nextFloat() * 0.55f, 0.008f + rnd.nextFloat() * 0.014f,
        rnd.nextFloat(), rnd.nextBoolean()
    )
}
private val cracks = List(45) {
    val x = rnd.nextFloat(); val y = 0.46f + rnd.nextFloat() * 0.54f
    Crack(x, y, x + (rnd.nextFloat() - 0.5f) * 0.16f, y + (rnd.nextFloat() - 0.5f) * 0.02f)
}

// x, y (fraction of height), scale
private val trees = listOf(
    Triple(0.07f, 0.44f, 1.0f), Triple(0.21f, 0.455f, 0.55f), Triple(0.62f, 0.44f, 0.5f),
    Triple(0.85f, 0.44f, 1.1f), Triple(0.95f, 0.46f, 0.7f)
)

/**
 * Draws the whole scene.
 * p     = 0f..1f   garden progress (count / 100)
 * t     = 0f..1f   looping clock for idle motion (clouds, ducks, sway)
 * pulse = 0f..1f   tap ripple (1f = no ripple)
 */
fun DrawScope.drawGarden(p: Float, t: Float, pulse: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.40f
    val twoPi = (2 * PI).toFloat()
    val sway = sin(t * twoPi)

    // ----- sky -----
    val sky = seg(p, 0f, 0.5f)
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(Color(0xFFE6C690), Color(0xFF8CCBE8), sky),
                lerp(Color(0xFFF2DDA6), Color(0xFFE6F0D4), sky)
            ),
            startY = 0f, endY = horizon + h * 0.05f
        ),
        size = Size(w, horizon + h * 0.05f)
    )

    // ----- sun -----
    val sunA = seg(p, 0.35f, 0.6f)
    if (sunA > 0f) {
        val c = Offset(w * 0.8f, horizon - h * 0.02f)
        drawCircle(Color(0xFFFFE08A).copy(alpha = 0.35f * sunA), w * 0.1f, c)
        drawCircle(Color(0xFFFFC94D).copy(alpha = sunA), w * 0.055f, c)
    }

    // ----- clouds -----
    val cloudCol = lerp(Color(0xFFF5EBD5), Color.White, seg(p, 0.3f, 0.6f))
    cloud(w * 0.08f + sway * w * 0.02f, h * 0.12f, w * 0.05f, cloudCol)
    cloud(w * 0.40f - sway * w * 0.015f, h * 0.17f, w * 0.04f, cloudCol)
    cloud(w * 0.78f + sway * w * 0.02f, h * 0.125f, w * 0.05f, cloudCol)

    // ----- hills -----
    val hillCol = lerp(Color(0xFFB99863), Color(0xFF7DB057), seg(p, 0.2f, 0.6f))
    val hill = Path().apply {
        moveTo(0f, horizon + h * 0.02f)
        cubicTo(w * 0.25f, horizon - h * 0.04f, w * 0.5f, horizon + h * 0.01f, w * 0.75f, horizon - h * 0.03f)
        cubicTo(w * 0.9f, horizon - h * 0.05f, w, horizon, w, horizon + h * 0.02f)
        lineTo(w, horizon + h * 0.1f)
        lineTo(0f, horizon + h * 0.1f)
        close()
    }
    drawPath(hill, hillCol)

    // ----- dry ground + cracks -----
    drawRect(Color(0xFFC9A468), Offset(0f, horizon + h * 0.02f), Size(w, h))
    val crackA = 1f - seg(p, 0f, 0.3f)
    if (crackA > 0f) cracks.forEach {
        drawLine(
            Color(0xFF8A6A3C).copy(alpha = 0.5f * crackA),
            Offset(it.x1 * w, it.y1 * h), Offset(it.x2 * w, it.y2 * h), w * 0.003f
        )
    }

    // ----- green spreading outward from the pond -----
    val cx = w / 2f
    val cy = h * 0.76f
    val rx = w * 0.30f
    val ry = rx * 0.20f
    val grassR = lerpF(0f, h * 0.75f, smooth(seg(p, 0.02f, 0.6f)))
    val green = lerp(Color(0xFF8FB04A), Color(0xFF5FA23A), seg(p, 0.2f, 0.8f))
    if (grassR > 1f) {
        clipRect(0f, horizon + h * 0.02f, w, h) {
            drawCircle(
                Brush.radialGradient(
                    0f to green, 0.75f to green, 1f to green.copy(alpha = 0f),
                    center = Offset(cx, cy), radius = grassR
                ),
                radius = grassR, center = Offset(cx, cy)
            )
        }
    }
    fun inGrass(fx: Float, fy: Float) = hypot(fx * w - cx, (fy * h - cy) * 1.3f) < grassR

    // ----- grass blades -----
    blades.forEach {
        if (it.order * 0.8f < p && inGrass(it.x, it.y)) {
            val x = it.x * w; val y = it.y * h
            drawLine(
                if (it.dark) Color(0xFF3F8A2E) else Color(0xFF79BE4B),
                Offset(x, y), Offset(x + sway * w * 0.004f, y - it.len * h), w * 0.004f, StrokeCap.Round
            )
        }
    }

    // ----- flowers -----
    flowers.forEach {
        val grow = smooth(seg(p, it.order * 0.85f, it.order * 0.85f + 0.12f))
        if (grow > 0f && inGrass(it.x, it.y)) {
            val perspective = 0.5f + (it.y - 0.46f) / 0.54f
            drawCircle(it.color, it.r * w * grow * perspective * 1.4f, Offset(it.x * w, it.y * h))
        }
    }

    // ----- trees -----
    val leaf = seg(p, 0.25f, 0.5f)
    val fruit = seg(p, 0.55f, 0.8f)
    trees.forEach { (x, y, s) -> tree(x * w, y * h, s, leaf, fruit, sway * w * 0.003f) }

    // ----- stream (appears near the end) -----
    val sA = seg(p, 0.75f, 0.9f)
    if (sA > 0f) {
        val path = Path().apply {
            moveTo(w * 1.05f, h * 0.58f)
            cubicTo(w * 0.95f, h * 0.66f, w * 0.85f, h * 0.70f, cx + rx * 0.85f, cy)
        }
        drawPath(
            path, Color(0xFF5DB4E3).copy(alpha = 0.9f),
            style = Stroke(width = w * 0.035f * sA, cap = StrokeCap.Round)
        )
    }

    // ----- pond -----
    drawOval(Color(0xFF6B4A2B), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))          // mud
    val water = smooth(seg(p, 0f, 0.05f))
    if (water > 0f) {
        val wr = rx * 0.9f * water
        val wh = ry * 0.9f * water
        drawOval(lerp(Color(0xFF4DA3D4), Color(0xFF6BB8E0), p), Offset(cx - wr, cy - wh), Size(wr * 2, wh * 2))
        drawOval(
            Color.White.copy(alpha = 0.35f),
            Offset(cx - wr * 0.6f, cy - wh * 0.55f), Size(wr * 1.0f, wh * 0.3f)
        )
        // idle ripple
        val idle = (t * 2f) % 1f
        drawOval(
            Color.White.copy(alpha = 0.25f * (1f - idle)),
            Offset(cx - wr * idle, cy - wh * idle), Size(wr * 2 * idle, wh * 2 * idle), style = Stroke(w * 0.003f)
        )
        // tap ripple
        if (pulse < 1f) {
            val r = 0.3f + 0.7f * pulse
            drawOval(
                Color.White.copy(alpha = 0.7f * (1f - pulse)),
                Offset(cx - wr * r, cy - wh * r), Size(wr * 2 * r, wh * 2 * r), style = Stroke(w * 0.006f)
            )
            // water drop falling in
            if (pulse < 0.25f) drawCircle(
                Color(0xFF8FD0F2), w * 0.012f,
                Offset(cx, lerpF(cy - h * 0.12f, cy, pulse / 0.25f))
            )
        }
    }
    drawOval(Color(0xFF8A5E34), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(w * 0.008f)) // rim

    // ----- ducks -----
    val dA = seg(p, 0.65f, 0.75f)
    if (dA > 0f) {
        duck(cx - rx * 0.30f, cy + sin(t * twoPi) * ry * 0.1f, w * 0.03f * dA)
        duck(cx - rx * 0.05f, cy + ry * 0.15f + cos(t * twoPi) * ry * 0.1f, w * 0.026f * dA)
    }

    // ----- birds -----
    val bA = seg(p, 0.8f, 0.9f)
    if (bA > 0f) repeat(3) { i ->
        val bx = ((t + i * 0.3f) % 1f) * w * 1.2f - w * 0.1f
        val by = h * 0.30f + i * h * 0.02f + sin((t * 4f + i) * twoPi) * h * 0.004f
        val s = w * 0.015f
        val col = Color(0xFF5A4326).copy(alpha = 0.7f * bA)
        drawLine(col, Offset(bx - s, by - s * 0.5f), Offset(bx, by), w * 0.003f, StrokeCap.Round)
        drawLine(col, Offset(bx, by), Offset(bx + s, by - s * 0.5f), w * 0.003f, StrokeCap.Round)
    }

    // ----- rainbow at 100 -----
    val rb = seg(p, 0.97f, 1f)
    if (rb > 0f) {
        val colors = listOf(0xFFFF5A5A, 0xFFFFA54D, 0xFFFFE066, 0xFF6CD36C, 0xFF5AB4FF, 0xFF6B6BFF, 0xFFB16BFF)
        val sw = w * 0.016f
        colors.forEachIndexed { i, c ->
            val r = w * 0.62f - i * sw
            drawArc(
                Color(c).copy(alpha = 0.5f * rb), 180f, 180f * rb, false,
                Offset(w / 2 - r, horizon + h * 0.02f - r), Size(r * 2, r * 2), style = Stroke(sw)
            )
        }
    }
}

/* ---------- small pieces ---------- */
private fun DrawScope.cloud(x: Float, y: Float, s: Float, c: Color) {
    drawCircle(c, s * 0.5f, Offset(x, y))
    drawCircle(c, s * 0.65f, Offset(x + s * 0.6f, y - s * 0.2f))
    drawCircle(c, s * 0.5f, Offset(x + s * 1.2f, y))
    drawOval(c, Offset(x - s * 0.5f, y - s * 0.1f), Size(s * 2.2f, s * 0.6f))
}

private fun DrawScope.duck(x: Float, y: Float, s: Float) {
    drawOval(Color.White, Offset(x - s, y - s * 0.5f), Size(s * 2f, s))
    drawCircle(Color.White, s * 0.45f, Offset(x + s * 0.9f, y - s * 0.7f))
    drawOval(Color(0xFFFFA43B), Offset(x + s * 1.25f, y - s * 0.8f), Size(s * 0.6f, s * 0.25f))
}

private fun DrawScope.tree(bx: Float, by: Float, s: Float, leaf: Float, fruit: Float, sway: Float) {
    val w = size.width
    val th = size.height * 0.075f * s
    val brown = Color(0xFF6B4A2B)
    val top = Offset(bx + sway * s, by - th)
    drawLine(brown, Offset(bx, by), top, w * 0.014f * s, StrokeCap.Round)
    val bl = th * 0.55f
    drawLine(brown, top, Offset(top.x - bl * 0.8f, top.y - bl * 0.7f), w * 0.008f * s, StrokeCap.Round)
    drawLine(brown, top, Offset(top.x + bl * 0.8f, top.y - bl * 0.8f), w * 0.008f * s, StrokeCap.Round)
    drawLine(brown, top, Offset(top.x, top.y - bl), w * 0.008f * s, StrokeCap.Round)
    if (leaf > 0f) {
        val r = th * 0.55f * leaf
        val dark = Color(0xFF4E9A3A)
        drawCircle(dark, r, Offset(top.x, top.y - th * 0.3f))
        drawCircle(dark, r * 0.8f, Offset(top.x - r * 0.7f, top.y - th * 0.05f))
        drawCircle(Color(0xFF5BAA40), r * 0.8f, Offset(top.x + r * 0.7f, top.y - th * 0.05f))
        drawCircle(Color(0xFF6DB84C), r * 0.6f, Offset(top.x, top.y - th * 0.45f))
        if (fruit > 0f) for (i in 0 until 5) {
            val a = i * 1.7f
            drawCircle(
                Color(0xFFE5483B), r * 0.12f * fruit,
                Offset(top.x + cos(a) * r * 0.8f, top.y - th * 0.2f + sin(a) * r * 0.7f)
            )
        }
    }
}
