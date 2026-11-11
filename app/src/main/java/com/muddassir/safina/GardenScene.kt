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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/* ---------- helpers ---------- */
/** Green spread radius as a fraction of the screen width. */
private fun grassRadius(p: Float): Float = when {
    p <= 0.05f -> lerpF(0f, 0.12f, p / 0.05f)
    p <= 0.30f -> lerpF(0.12f, 0.45f, (p - 0.05f) / 0.25f)
    p <= 0.50f -> lerpF(0.45f, 1.00f, (p - 0.30f) / 0.20f)
    else -> lerpF(1.00f, 1.70f, seg(p, 0.50f, 0.60f))
}

/* ---------- fixed random scenery (same every launch) ---------- */
private val rnd = Random(11)

private class Flower(val x: Float, val y: Float, val color: Color, val r: Float, val order: Float)
private class Blade(val x: Float, val y: Float, val len: Float, val order: Float, val dark: Boolean)
private class Crack(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
private class Tuft(val x: Float, val y: Float, val hh: Float)
private class RainDrop(val x: Float, val landY: Float, val phase: Float, val len: Float)
private class Drifter(val x: Float, val size: Float, val phase: Float)
private class Speck(val x: Float, val y: Float, val phase: Float, val size: Float)
private class TreeSpec(val x: Float, val scale: Float)

private val flowerColors = listOf(
    Color.White, Color(0xFFFFE066), Color(0xFFFF9EC4),
    Color(0xFFB69CFF), Color(0xFFFFA45C), Color(0xFF7B8CFF)
)
private val flowers = List(260) {
    Flower(
        rnd.nextFloat(), 0.50f + rnd.nextFloat() * 0.49f,
        flowerColors[rnd.nextInt(flowerColors.size)], 0.004f + rnd.nextFloat() * 0.005f, rnd.nextFloat()
    )
}
private val blades = List(400) {
    Blade(
        rnd.nextFloat(), 0.50f + rnd.nextFloat() * 0.49f, 0.010f + rnd.nextFloat() * 0.016f,
        rnd.nextFloat(), rnd.nextBoolean()
    )
}
private val cracks = List(45) {
    val x = rnd.nextFloat(); val y = 0.52f + rnd.nextFloat() * 0.46f
    Crack(x, y, x + (rnd.nextFloat() - 0.5f) * 0.16f, y + (rnd.nextFloat() - 0.5f) * 0.02f)
}
private val tufts = List(28) {
    Tuft(rnd.nextFloat(), 0.55f + rnd.nextFloat() * 0.40f, 0.008f + rnd.nextFloat() * 0.010f)
}
private val rainDrops = List(40) {
    RainDrop(rnd.nextFloat(), 0.52f + rnd.nextFloat() * 0.44f, rnd.nextFloat(), 0.010f + rnd.nextFloat() * 0.016f)
}
private val drifters = List(10) {
    Drifter(rnd.nextFloat(), 0.008f + rnd.nextFloat() * 0.008f, rnd.nextFloat())
}
private val pollen = List(18) {
    Speck(rnd.nextFloat(), 0.45f + rnd.nextFloat() * 0.45f, rnd.nextFloat(), 0.0015f + rnd.nextFloat() * 0.0025f)
}
private val fireflies = List(14) {
    Speck(rnd.nextFloat(), 0.55f + rnd.nextFloat() * 0.40f, rnd.nextFloat(), 0f)
}
private val trees = listOf(
    TreeSpec(0.07f, 1.0f), TreeSpec(0.21f, 0.55f), TreeSpec(0.68f, 0.50f),
    TreeSpec(0.86f, 1.10f), TreeSpec(0.96f, 0.40f)
)
private val lilyPads = listOf(
    Triple(-0.55f, -0.30f, 0.9f), Triple(0.40f, 0.32f, 0.7f),
    Triple(0.08f, -0.52f, 0.6f), Triple(-0.22f, 0.48f, 0.8f)
)
private val sparkles = List(8) { i ->
    Triple(-0.7f + i * 0.2f, -0.45f + ((i * 7) % 5) * 0.22f, i * 0.37f)
}

/* reused across frames so the draw loop does not allocate paths */
private val hillFar = Path()
private val hillMid = Path()
private val hillNear = Path()
private val streamPath = Path()

/**
 * Draws the whole scene.
 * p          = 0f..1f   garden progress (count / 100)
 * t          = 0f..1f   looping clock for idle motion
 * pulse      = 0f..1f   tap ripple (1f = no ripple)
 * ripple     = 1f normal tap, >1 for milestones (bigger rings)
 * shimmer    = 0f..1f   warm shimmer across the pond at milestones
 * golden     = 0f..1f   golden-hour tint after 100
 * celebrate  = 0f..1f   one-shot petal burst at 100
 * calm       = system "remove animations" is on
 */
fun DrawScope.drawGarden(
    p: Float,
    t: Float,
    pulse: Float,
    ripple: Float = 1f,
    shimmer: Float = 0f,
    golden: Float = 0f,
    celebrate: Float = 0f,
    calm: Boolean = false
) {
    val w = size.width
    val h = size.height
    val twoPi = (2 * PI).toFloat()
    val horizon = h * 0.50f
    val podX = w * 0.5f
    val podY = h * 0.80f
    val rx = w * 0.28f
    val ry = rx * 0.25f
    val skyMix = seg(p, 0.35f, 0.70f)

    // ----- sky (dusty tan/gold -> soft blue/cream) -----
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(Color(0xFFB7A073), Color(0xFF87BAD4), skyMix),
                lerp(Color(0xFFC6AF85), Color(0xFF8DBFD8), skyMix),
                lerp(Color(0xFFD9C28A), Color(0xFFDDDBB4), skyMix)
            ),
            startY = 0f, endY = horizon
        ),
        size = Size(w, horizon + 2f)
    )

    // ----- rainbow, behind everything on the horizon -----
    val rb = seg(p, 0.97f, 1f)
    if (rb > 0f) rainbow(w, horizon, rb, golden)

    // ----- sun with soft glow and breathing rays -----
    val sunA = seg(p, 0.45f, 0.70f)
    val sunX = w * 0.86f
    val sunY = lerpF(horizon + h * 0.05f, horizon - h * 0.07f, smooth(sunA))
    if (sunA > 0f) {
        val sr = w * 0.055f
        val breath = 0.75f + 0.25f * sin(t * twoPi * 2f)
        drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.18f * sunA * breath), sr * 2.6f, Offset(sunX, sunY))
        drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.35f * sunA), sr * 1.7f, Offset(sunX, sunY))
        drawCircle(Color(0xFFF4C94A).copy(alpha = sunA), sr, Offset(sunX, sunY))
        repeat(5) { i ->
            val angle = -PI.toFloat() / 2f - 0.62f + i * 0.31f
            val rayBreath = 0.5f + 0.5f * sin(t * twoPi * 1.5f + i * 0.5f)
            val from = Offset(sunX + cos(angle) * sr * 1.3f, sunY + sin(angle) * sr * 1.3f)
            val to = Offset(sunX + cos(angle) * w * 0.55f, sunY + sin(angle) * w * 0.55f)
            drawLine(
                Color(0xFFFFEFC2).copy(alpha = 0.07f * sunA * (0.4f + 0.6f * rayBreath)),
                from, to, w * 0.03f, StrokeCap.Round
            )
        }
    }

    // ----- clouds: cream -> grey-blue rain -> cream-white -----
    val storm = seg(p, 0.25f, 0.40f)
    val clear = seg(p, 0.50f, 0.65f)
    val cloudCol = lerp(lerp(Color(0xFFF4EBD3), Color(0xFFBCC9CF), storm), Color(0xFFFDFBF2), clear)
    val drift = sin(t * twoPi)
    cloud(w * 0.12f + drift * w * 0.02f, h * 0.12f, w * 0.060f, cloudCol)
    cloud(w * 0.82f + drift * w * 0.017f, h * 0.15f, w * 0.065f, cloudCol)
    cloud(w * 0.45f - drift * w * 0.015f, h * 0.09f, w * 0.045f, cloudCol)
    val edgeA = seg(p, 0.88f, 1f)
    if (edgeA > 0f) {
        cloud(-w * 0.01f, h * 0.22f, w * 0.050f, cloudCol.copy(alpha = 0.85f * edgeA))
        cloud(w * 1.01f, h * 0.20f, w * 0.055f, cloudCol.copy(alpha = 0.85f * edgeA))
    }

    // ----- hills: far hazy, mid, near -----
    val hillMix = seg(p, 0.20f, 0.60f)
    hillFar.reset(); hillFar.moveTo(0f, horizon)
    hillFar.cubicTo(w * 0.18f, horizon - h * 0.050f, w * 0.44f, horizon - h * 0.075f, w * 0.62f, horizon - h * 0.045f)
    hillFar.cubicTo(w * 0.80f, horizon - h * 0.020f, w * 0.92f, horizon - h * 0.055f, w, horizon - h * 0.030f)
    hillFar.lineTo(w, horizon + h * 0.03f); hillFar.lineTo(0f, horizon + h * 0.03f); hillFar.close()
    drawPath(hillFar, lerp(Color(0xFFA9A48D), Color(0xFF8FB57A), hillMix))

    hillMid.reset(); hillMid.moveTo(0f, horizon + h * 0.012f)
    hillMid.cubicTo(w * 0.18f, horizon - h * 0.030f, w * 0.38f, horizon + h * 0.006f, w * 0.55f, horizon - h * 0.014f)
    hillMid.cubicTo(w * 0.75f, horizon - h * 0.036f, w * 0.90f, horizon + h * 0.006f, w, horizon - h * 0.004f)
    hillMid.lineTo(w, horizon + h * 0.06f); hillMid.lineTo(0f, horizon + h * 0.06f); hillMid.close()
    drawPath(hillMid, lerp(Color(0xFFB8985F), Color(0xFF7DB057), hillMix))

    hillNear.reset(); hillNear.moveTo(0f, horizon + h * 0.030f)
    hillNear.cubicTo(w * 0.22f, horizon + h * 0.006f, w * 0.42f, horizon + h * 0.034f, w * 0.60f, horizon + h * 0.020f)
    hillNear.cubicTo(w * 0.78f, horizon + h * 0.006f, w * 0.92f, horizon + h * 0.030f, w, horizon + h * 0.018f)
    hillNear.lineTo(w, horizon + h * 0.09f); hillNear.lineTo(0f, horizon + h * 0.09f); hillNear.close()
    drawPath(hillNear, lerp(Color(0xFFA98A57), Color(0xFF5F9A3F), hillMix))

    // ----- light mist at the horizon, fades as the garden grows -----
    val mistA = 0.30f * (1f - seg(p, 0.20f, 0.65f))
    if (mistA > 0.01f) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFFFFFBF0).copy(alpha = mistA), Color.Transparent),
                startY = horizon - h * 0.09f, endY = horizon + h * 0.10f
            ),
            topLeft = Offset(0f, horizon - h * 0.09f), size = Size(w, h * 0.19f)
        )
    }

    // ----- dry ground -----
    val groundTop = horizon + h * 0.02f
    drawRect(Color(0xFFB09469), Offset(0f, groundTop), Size(w, h - groundTop))

    // ----- cracks fade out as the green covers them -----
    val crackA = 1f - seg(p, 0.10f, 0.55f)
    if (crackA > 0.01f) cracks.forEach {
        drawLine(
            Color(0xFF8A6A3C).copy(alpha = 0.45f * crackA),
            Offset(it.x1 * w, it.y1 * h), Offset(it.x2 * w, it.y2 * h), w * 0.0025f
        )
    }

    // ----- dry grass tufts at the very start -----
    val tuftA = 1f - seg(p, 0.12f, 0.45f)
    if (tuftA > 0.01f) tufts.forEach {
        val x = it.x * w; val y = it.y * h
        val c = Color(0xFF8E8C5F).copy(alpha = 0.55f * tuftA)
        drawLine(c, Offset(x, y), Offset(x - w * 0.004f, y - it.hh * h), w * 0.0025f, StrokeCap.Round)
        drawLine(c, Offset(x, y), Offset(x, y - it.hh * h * 1.25f), w * 0.0025f, StrokeCap.Round)
        drawLine(c, Offset(x, y), Offset(x + w * 0.004f, y - it.hh * h * 0.85f), w * 0.0025f, StrokeCap.Round)
    }

    // ----- green spreads outward from the pond, feathered edge -----
    val grassR = grassRadius(p) * w
    val green = lerp(Color(0xFF6A9B3A), Color(0xFF5F9237), seg(p, 0.5f, 1f))
    if (grassR > 2f) {
        clipRect(0f, groundTop, w, h) {
            drawCircle(
                Brush.radialGradient(
                    0f to green, 0.70f to green, 1f to green.copy(alpha = 0f),
                    center = Offset(podX, podY), radius = grassR
                ),
                radius = grassR, center = Offset(podX, podY)
            )
        }
    }

    fun inGrass(fx: Float, fy: Float) =
        hypot(fx * w - podX, (fy * h - podY) * 1.3f) < grassR

    // ----- grass blades, swaying in a wave that travels across the field -----
    blades.forEach { b ->
        if (b.order * 0.75f < p && inGrass(b.x, b.y)) {
            val x = b.x * w; val y = b.y * h
            val wave = sin((t * 2f - b.x * 1.6f) * twoPi)
            val bend = wave * w * 0.0045f * (0.5f + b.order)
            val grow = (0.45f + 0.55f * seg(p, b.order * 0.75f, b.order * 0.75f + 0.25f))
            val top = Offset(x + bend, y - b.len * h * grow)
            drawLine(
                if (b.dark) Color(0xFF3F7A2A) else Color(0xFF79B94B),
                Offset(x, y), top, w * 0.0035f, StrokeCap.Round
            )
        }
    }

    // ----- flowers on the meadow, only where the grass has spread -----
    // Most are flat dots; only a few large near ones get the 5-petal shape, which
    // keeps the draw cost low without losing the meadow look.
    flowers.forEachIndexed { index, f ->
        val grow = smooth(seg(p, f.order * 0.85f, f.order * 0.85f + 0.10f))
        if (grow > 0f && inGrass(f.x, f.y)) {
            val perspective = 0.55f + (f.y - 0.50f) * 1.6f
            val cx = f.x * w + sin((t * 2f + f.order * 5f) * twoPi) * w * 0.0015f
            val cy = f.y * h
            val r = f.r * w * grow * perspective
            if (index % 10 == 0 && r > w * 0.0045f && f.y > 0.72f) {
                repeat(5) { k ->
                    val a = k * (twoPi / 5f) + f.order * 2f
                    drawCircle(f.color, r * 0.62f, Offset(cx + cos(a) * r * 0.55f, cy + sin(a) * r * 0.55f))
                }
                drawCircle(Color(0xFFFFF6D0), r * 0.35f, Offset(cx, cy))
            } else {
                drawCircle(f.color, r, Offset(cx, cy))
            }
        }
    }

    // ----- trees: bare, then leaves, canopies, fruit -----
    val leafDots = seg(p, 0.20f, 0.30f)
    val canopy = smooth(seg(p, 0.30f, 0.45f))
    val fruit = seg(p, 0.55f, 0.85f)
    trees.forEach { tr ->
        tree(
            bx = tr.x * w, by = h * 0.53f, s = tr.scale,
            leafDots = leafDots, canopy = canopy, fruit = fruit,
            sway = sin(t * twoPi) * w * 0.0025f, golden = golden, w = w, h = h
        )
    }

    // ----- stream flows in from the right edge into the pond -----
    val sA = seg(p, 0.75f, 0.90f)
    if (sA > 0f) {
        streamPath.reset()
        streamPath.moveTo(w * 1.06f, h * 0.68f)
        streamPath.cubicTo(w * 0.96f, h * 0.74f, w * 0.86f, h * 0.79f, podX + rx * 0.88f, podY)
        val sw = w * 0.04f * sA
        drawPath(streamPath, Color(0xFF5DB4E3).copy(alpha = 0.95f), style = Stroke(sw, cap = StrokeCap.Round))
        drawPath(streamPath, Color(0xFFA9DFF5).copy(alpha = 0.55f), style = Stroke(sw * 0.35f, cap = StrokeCap.Round))
        repeat(6) { i ->
            val u = ((t * 1.5f + i * 0.17f) % 1f)
            if (u > 0.05f && u < 0.95f) {
                val sx = lerpF(w * 1.06f, podX + rx * 0.88f, u)
                val sy = lerpF(h * 0.68f, podY, u) + sin(u * PI.toFloat()) * h * 0.03f
                drawLine(
                    Color.White.copy(alpha = 0.35f * sA),
                    Offset(sx, sy), Offset(sx - w * 0.02f, sy + h * 0.004f), sw * 0.18f, StrokeCap.Round
                )
            }
        }
    }

    // ----- pond: mud, water, shine, ripples, pads -----
    drawOval(Color(0xFF735A3D), Offset(podX - rx, podY - ry), Size(rx * 2, ry * 2))
    val water = smooth(seg(p, 0f, 0.06f))
    if (water > 0f) {
        val wr = rx * 0.94f * water
        val wh = ry * 0.94f * water
        val waterCol = lerp(lerp(Color(0xFF4DA3D4), Color(0xFF6BB8E0), p), skyMixColor(skyMix), 0.22f)
        drawOval(waterCol, Offset(podX - wr, podY - wh), Size(wr * 2, wh * 2))
        // soft sky reflection band
        drawOval(
            Color(0xFFD9EEF7).copy(alpha = 0.20f),
            Offset(podX - wr * 0.9f, podY - wh * 0.75f), Size(wr * 1.8f, wh * 0.45f)
        )
        // shine streak
        drawOval(
            Color.White.copy(alpha = 0.40f),
            Offset(podX - wr * 0.62f, podY - wh * 0.58f), Size(wr * 1.05f, wh * 0.28f)
        )
        // water sparkles
        sparkles.forEach { (fx, fy, ph) ->
            val tw = sin((t * 3f + ph) * twoPi)
            if (tw > 0.55f) {
                val k = (tw - 0.55f) / 0.45f
                drawCircle(
                    Color.White.copy(alpha = 0.55f * k),
                    w * 0.0035f, Offset(podX + rx * fx, podY + ry * fy)
                )
            }
        }
        // idle ripple every ~2s
        val idle = (t * 4f) % 1f
        drawOval(
            Color.White.copy(alpha = 0.22f * (1f - idle)),
            Offset(podX - wr * idle, podY - wh * idle), Size(wr * 2 * idle, wh * 2 * idle),
            style = Stroke(w * 0.0025f)
        )
        // warm shimmer at milestones
        if (shimmer > 0f) {
            drawOval(
                Color(0xFFFFEDBE).copy(alpha = 0.35f * shimmer),
                Offset(podX - wr * 1.02f, podY - wh * 1.02f), Size(wr * 2.04f, wh * 2.04f),
                style = Stroke(w * 0.006f)
            )
        }
        // tap: falling teardrop, glow ring, expanding ripples
        if (pulse < 1f) {
            drawOval(
                Color(0xFFFFF3C4).copy(alpha = 0.22f * (1f - pulse)),
                Offset(podX - wr * (0.6f + 0.5f * pulse), podY - wh * (0.6f + 0.5f * pulse)),
                Size(wr * 2 * (0.6f + 0.5f * pulse), wh * 2 * (0.6f + 0.5f * pulse)),
                style = Stroke(w * 0.004f)
            )
            val r1 = 0.25f + 0.75f * pulse
            drawOval(
                Color.White.copy(alpha = 0.65f * (1f - pulse)),
                Offset(podX - wr * r1, podY - wh * r1), Size(wr * 2 * r1, wh * 2 * r1),
                style = Stroke(w * 0.005f * ripple)
            )
            val p2 = ((pulse - 0.18f) / 0.82f).coerceAtLeast(0f)
            if (p2 > 0f) {
                val r2 = 0.18f + 0.62f * p2
                drawOval(
                    Color.White.copy(alpha = 0.42f * (1f - p2)),
                    Offset(podX - wr * r2, podY - wh * r2), Size(wr * 2 * r2, wh * 2 * r2),
                    style = Stroke(w * 0.004f * ripple)
                )
            }
            if (pulse < 0.30f) {
                val drop = pulse / 0.30f
                val dy = lerpF(podY - h * 0.16f, podY, drop)
                drawOval(
                    Color(0xFF9AD3F0),
                    Offset(podX - w * 0.006f, dy - w * 0.010f), Size(w * 0.012f, w * 0.020f)
                )
            }
        }
        // lily pads + lotus from p 0.5
        val padA = seg(p, 0.50f, 0.65f)
        if (padA > 0f) lilyPads.forEachIndexed { i, (lx, ly, ls) ->
            val swayY = sin((t * 2f + i * 0.3f) * twoPi) * ry * 0.02f
            val px = podX + rx * lx
            val py = podY + ry * ly + swayY
            val pr = w * 0.022f * ls * padA
            drawOval(Color(0xFF4E8A3E).copy(alpha = 0.95f), Offset(px - pr, py - pr * 0.42f), Size(pr * 2, pr * 0.84f))
            drawOval(Color(0xFF6DB84C).copy(alpha = 0.55f), Offset(px - pr * 0.55f, py - pr * 0.28f), Size(pr * 0.9f, pr * 0.3f))
            if (i % 2 == 0) {
                repeat(5) { k ->
                    val a = k * (twoPi / 5f) + i
                    drawCircle(
                        Color(0xFFFFF0F5).copy(alpha = 0.9f),
                        pr * 0.22f,
                        Offset(px + cos(a) * pr * 0.22f, py - pr * 0.22f + sin(a) * pr * 0.18f)
                    )
                }
                drawCircle(Color(0xFFFFD9A0), pr * 0.12f, Offset(px, py - pr * 0.22f))
            }
        }
    }
    drawOval(
        Color(0xFF8A5E34), Offset(podX - rx, podY - ry), Size(rx * 2, ry * 2),
        style = Stroke(w * 0.008f)
    )

    // ----- ducklings -----
    val dA = seg(p, 0.65f, 0.75f)
    if (dA > 0f) {
        val baseY = podY + ry * 0.15f
        val d1x = podX - rx * 0.28f + sin(t * twoPi) * rx * 0.14f
        val d2x = podX - rx * 0.06f + sin((t - 0.04f) * twoPi) * rx * 0.14f
        val d1y = baseY + sin(t * twoPi * 3f) * ry * 0.06f
        val d2y = baseY + ry * 0.10f + sin((t - 0.04f) * twoPi * 3f) * ry * 0.06f
        duckling(d1x, d1y, w * 0.026f * dA, dip = maxD(0f, sin(t * twoPi * 2f)), w = w)
        duckling(d2x, d2y, w * 0.023f * dA, dip = maxD(0f, sin((t + 0.5f) * twoPi * 2f)), w = w)
    }

    // ----- rabbits -----
    val rA = seg(p, 0.70f, 0.80f)
    if (rA > 0f) {
        val hopL = (sin(t * twoPi) + 1f) * 0.5f
        rabbit(w * 0.14f + sin(t * twoPi * 0.5f) * w * 0.012f, h * 0.90f - hopL * h * 0.008f, w * 0.038f * rA, earTwitch = earTwitch(t, 0f), w = w)
        rabbit(w * 0.80f + sin((t + 0.35f) * twoPi * 0.5f) * w * 0.012f, h * 0.90f - (1f - hopL) * h * 0.006f, w * 0.035f * rA, earTwitch = earTwitch(t, 0.45f), w = w)
        // two hens pecking near the tree line
        hen(
            w * 0.235f + sin((t + 0.60f) * twoPi * 0.5f) * w * 0.008f, h * 0.640f,
            w * 0.030f * rA, peck = maxD(0f, sin((t * 1.3f + 0.1f) * twoPi)), w = w
        )
        hen(
            w * 0.695f + sin((t + 0.80f) * twoPi * 0.5f) * w * 0.008f, h * 0.665f,
            w * 0.032f * rA, peck = maxD(0f, sin((t * 1.1f + 0.55f) * twoPi)), w = w
        )
    }

    // ----- fawn behind the pond's upper-left edge -----
    val fA = seg(p, 0.85f, 0.92f)
    if (fA > 0f) {
        val drink = smooth(maxD(0f, sin((t * 0.9f + 0.2f) * twoPi)))
        fawn(
            podX - rx - w * 0.060f, podY - ry + h * 0.005f,
            w * 0.064f * fA, drink = drink * 0.6f, w = w
        )
        // a second, smaller deer standing by the right-hand trees
        fawn(
            w * 0.895f, h * 0.680f,
            w * 0.050f * fA, drink = (1f - drink) * 0.35f, w = w
        )
    }

    // ----- bees and dragonflies -----
    val insectA = seg(p, 0.70f, 0.82f)
    if (insectA > 0f) {
        repeat(2) { i ->
            val bx = w * (0.28f + i * 0.34f) + sin((t * 2f + i * 0.4f) * twoPi) * w * 0.05f
            val by = h * (0.70f + i * 0.10f) + cos((t * 3f + i) * twoPi) * h * 0.02f
            bee(bx, by, w * 0.008f * insectA)
        }
        repeat(2) { i ->
            val dx = podX + sin((t + i * 0.5f) * twoPi) * rx * 0.75f
            val dy = podY - ry * 0.35f - i * ry * 0.25f
            dragonfly(dx, dy, w * 0.010f * insectA, w = w)
        }
    }

    // ----- fish: a slow arc out of the pond every loop -----
    val fishA = seg(p, 0.75f, 0.85f)
    if (fishA > 0f && !calm) {
        val cyc = (t * 0.9f + 0.15f) % 1f
        if (cyc < 0.22f) {
            val u = cyc / 0.22f
            val fx = podX + rx * lerpF(-0.25f, 0.35f, u)
            val fy = podY - sin(u * PI.toFloat()) * ry * 1.6f
            fish(fx, fy, w * 0.010f * fishA, w = w)
            if (u > 0.9f) {
                drawOval(
                    Color.White.copy(alpha = 0.35f * (1f - (u - 0.9f) / 0.1f)),
                    Offset(podX + rx * 0.35f - rx * 0.2f, podY - ry * 0.2f), Size(rx * 0.4f, ry * 0.4f),
                    style = Stroke(w * 0.002f)
                )
            }
        }
    }

    // ----- birds -----
    val birdA = seg(p, 0.70f, 0.80f)
    if (birdA > 0f) repeat(3) { i ->
        val bx = ((t * 0.9f + i * 0.3f) % 1f) * w * 1.2f - w * 0.1f
        val by = h * 0.28f + i * h * 0.025f + sin((t * 4f + i) * twoPi) * h * 0.004f
        val s = w * 0.014f
        val flap = sin((t * 6f + i) * twoPi) * 0.25f
        val col = Color(0xFF4A3822).copy(alpha = 0.65f * birdA)
        drawLine(col, Offset(bx - s, by - s * (0.4f + flap)), Offset(bx, by), w * 0.0028f, StrokeCap.Round)
        drawLine(col, Offset(bx, by), Offset(bx + s, by - s * (0.4f + flap)), w * 0.0028f, StrokeCap.Round)
    }

    // ----- light rain while the clouds are grey-blue -----
    val rainA = seg(p, 0.15f, 0.24f) * (1f - seg(p, 0.40f, 0.50f))
    if (rainA > 0.01f && !calm) rainDrops.forEach { d ->
        val u = (t * 6f + d.phase) % 1f
        val x = d.x * w + u * w * 0.05f
        val y = lerpF(-h * 0.02f, d.landY * h, u)
        drawLine(
            Color(0xFF8FA6B8).copy(alpha = 0.30f * rainA),
            Offset(x, y), Offset(x - w * 0.012f, y - d.len * h), w * 0.0022f, StrokeCap.Round
        )
        if (u > 0.90f) {
            val k = (u - 0.90f) / 0.10f
            drawOval(
                Color.White.copy(alpha = 0.35f * rainA * (1f - k)),
                Offset(x - w * 0.012f * k, d.landY * h - h * 0.003f * k),
                Size(w * 0.024f * k, h * 0.006f * k), style = Stroke(w * 0.0015f)
            )
        }
    }

    // ----- falling leaves -----
    val driftA = seg(p, 0.60f, 0.85f)
    if (driftA > 0.01f) drifters.forEach { d ->
        val u = (t * 0.55f + d.phase) % 1f
        val dy = lerpF(h * 0.30f, h * 0.95f, u)
        val dx = d.x * w + sin((t * 2f + d.phase * 6f) * twoPi) * w * 0.03f
        val a = (1f - u * 0.7f) * driftA * 0.85f
        rotate(degrees = sin((t * 2f + d.phase) * twoPi) * 35f, pivot = Offset(dx, dy)) {
            drawOval(
                Color(0xFF8CBF4F).copy(alpha = a),
                Offset(dx - d.size * w, dy - d.size * w * 0.5f), Size(d.size * w * 2f, d.size * w)
            )
        }
    }

    // ----- pollen drifting up in the sunlight -----
    val pollenA = seg(p, 0.45f, 0.80f)
    if (pollenA > 0.01f && !calm) pollen.forEach { s ->
        val u = (t * 0.35f + s.phase) % 1f
        val py = s.y * h - u * h * 0.20f
        val px = s.x * w + sin((t * 1.4f + s.phase * 4f) * twoPi) * w * 0.015f
        val a = (1f - u) * pollenA * (0.35f + 0.35f * sin((t * 2f + s.phase) * twoPi))
        if (a > 0.02f) drawCircle(Color(0xFFFFFDF0).copy(alpha = a.coerceIn(0f, 0.7f)), s.size * w, Offset(px, py))
    }

    // ----- petals floating up after the 100th tap -----
    if (celebrate > 0f) {
        repeat(18) { i ->
            val ph = i / 18f
            val rise = celebrate
            val px = podX + sin(i * 2.399f + celebrate * 4f) * w * (0.08f + ph * 0.28f)
            val py = podY - h * 0.04f - rise * h * (0.22f + ph * 0.34f)
            val a = (1f - rise * rise).coerceIn(0f, 1f) * 0.9f
            rotate(degrees = sin(celebrate * 6f + i) * 45f, pivot = Offset(px, py)) {
                drawOval(
                    Color(0xFFFFC6DA).copy(alpha = a),
                    Offset(px - w * 0.006f, py - w * 0.003f), Size(w * 0.012f, w * 0.006f)
                )
            }
        }
    }

    // ----- fireflies in the golden hour -----
    if (golden > 0f) fireflies.forEach { f ->
        val fx = f.x * w + sin((t * 1.2f + f.phase) * twoPi) * w * 0.03f
        val fy = f.y * h + cos((t * 0.9f + f.phase) * twoPi) * h * 0.02f
        val glow = 0.45f + 0.55f * sin((t * 3f + f.phase) * twoPi)
        drawCircle(Color(0xFFFFE9A0).copy(alpha = 0.22f * glow * golden), w * 0.013f, Offset(fx, fy))
        drawCircle(Color(0xFFFFF7D6).copy(alpha = 0.85f * glow * golden), w * 0.0035f, Offset(fx, fy))
    }

    // ----- golden hour tint over everything -----
    if (golden > 0.01f) {
        drawRect(Color(0xFFFFC978).copy(alpha = 0.16f * golden))
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFFFFE3A0).copy(alpha = 0.30f * golden), Color.Transparent),
                center = Offset(sunX, sunY), radius = w * 0.95f
            )
        )
    }
}

private fun skyMixColor(skyMix: Float) = lerp(Color(0xFF8DBFD8), Color(0xFFDDDBB4), skyMix)

private fun maxD(a: Float, b: Float) = if (a > b) a else b

/** Ear twitches now and then, gated by the clock so it stays occasional. */
private fun earTwitch(t: Float, offset: Float): Float {
    val s = sin((t * 3f + offset) * 6.2831855f)
    return if (s > 0.94f) (s - 0.94f) / 0.06f else 0f
}

/* ---------- scene pieces ---------- */
private fun DrawScope.rainbow(w: Float, horizon: Float, rb: Float, golden: Float) {
    val colors = listOf(
        0xFFFF5A5A, 0xFFFFA54D, 0xFFFFE066, 0xFF6CD36C,
        0xFF5AB4FF, 0xFF6B6BFF, 0xFFB16BFF
    )
    val band = w * 0.05f / 7f
    val alpha = (0.45f + 0.20f * golden) * rb
    colors.forEachIndexed { i, c ->
        val r = w * 0.62f - i * band
        drawArc(
            Color(c).copy(alpha = alpha),
            180f, 180f * rb, false,
            Offset(w / 2 - r, horizon - r), Size(r * 2, r * 2),
            style = Stroke(band)
        )
    }
}

private fun DrawScope.tree(
    bx: Float, by: Float, s: Float,
    leafDots: Float, canopy: Float, fruit: Float,
    sway: Float, golden: Float, w: Float, h: Float
) {
    val th = h * 0.10f * s
    val brown = Color(0xFF7A5530)
    val top = Offset(bx + sway, by - th)

    if (golden > 0f) {
        drawOval(
            Color(0xFF2F2A1E).copy(alpha = 0.12f * golden),
            Offset(bx - th * 1.1f, by - th * 0.03f), Size(th * 2.2f, th * 0.18f)
        )
    }

    drawLine(brown, Offset(bx, by), top, w * 0.013f * s, StrokeCap.Round)

    val bl = th * 0.52f
    val b1 = Offset(top.x - bl * 0.95f, top.y - bl * 0.75f)
    val b2 = Offset(top.x + bl * 0.95f, top.y - bl * 0.85f)
    val b3 = Offset(top.x - bl * 0.50f, top.y - bl * 1.15f)
    val b4 = Offset(top.x + bl * 0.45f, top.y - bl * 1.20f)
    drawLine(brown, top, b1, w * 0.007f * s, StrokeCap.Round)
    drawLine(brown, top, b2, w * 0.007f * s, StrokeCap.Round)
    drawLine(brown, top, b3, w * 0.006f * s, StrokeCap.Round)
    drawLine(brown, top, b4, w * 0.006f * s, StrokeCap.Round)
    // small forks
    drawLine(brown, b1, Offset(b1.x - bl * 0.18f, b1.y - bl * 0.22f), w * 0.004f * s, StrokeCap.Round)
    drawLine(brown, b2, Offset(b2.x + bl * 0.16f, b2.y - bl * 0.24f), w * 0.004f * s, StrokeCap.Round)

    if (leafDots > 0f) {
        val dot = w * 0.0045f * s * leafDots
        val leaf = Color(0xFF6DB84C).copy(alpha = 0.95f)
        drawCircle(leaf, dot, b1)
        drawCircle(leaf, dot, b2)
        drawCircle(leaf, dot * 0.9f, b3)
        drawCircle(leaf, dot * 0.9f, b4)
        drawCircle(leaf, dot * 0.8f, top)
    }

    if (canopy > 0f) {
        val r = th * 0.62f * canopy
        drawCircle(Color(0xFF4E9A3A), r, Offset(top.x, top.y - th * 0.18f))
        drawCircle(Color(0xFF4E9A3A), r * 0.82f, Offset(top.x - r * 0.75f, top.y - th * 0.02f))
        drawCircle(Color(0xFF5BAA40), r * 0.82f, Offset(top.x + r * 0.75f, top.y - th * 0.02f))
        drawCircle(Color(0xFF6DB84C), r * 0.68f, Offset(top.x + r * 0.15f, top.y - th * 0.40f))
        drawCircle(Color(0xFF5BAA40), r * 0.62f, Offset(top.x - r * 0.35f, top.y - th * 0.35f))

        if (fruit > 0f) repeat(6) { i ->
            val a = i * 1.7f
            drawCircle(
                if (i % 2 == 0) Color(0xFFE5483B) else Color(0xFFF29A2E),
                r * 0.11f * fruit,
                Offset(top.x + cos(a) * r * 0.75f, top.y - th * 0.18f + sin(a) * r * 0.62f)
            )
        }
    }
}

private fun DrawScope.duckling(x: Float, y: Float, s: Float, dip: Float, w: Float) {
    val body = Color(0xFFFDFBF4)
    drawOval(body, Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    drawCircle(body, s * 0.48f, Offset(x + s * 0.85f, y - s * (0.75f + dip * 0.35f)))
    drawCircle(Color(0xFF3A3226), s * 0.07f, Offset(x + s * 1.0f, y - s * (0.85f + dip * 0.35f)))
    drawOval(
        Color(0xFFFFA43B),
        Offset(x + s * 1.18f, y - s * (0.75f + dip * 0.35f)), Size(s * 0.55f, s * 0.22f)
    )
    drawLine(
        Color.White.copy(alpha = 0.25f), Offset(x - s * 0.6f, y + s * 0.55f),
        Offset(x + s * 0.4f, y + s * 0.55f), w * 0.0018f
    )
}

private fun DrawScope.rabbit(x: Float, y: Float, s: Float, earTwitch: Float, w: Float) {
    val fur = Color(0xFFF7EEDD)
    drawOval(Color(0xFF3A3325).copy(alpha = 0.07f), Offset(x - s * 1.1f, y - s * 0.22f), Size(s * 2.2f, s * 0.45f))
    drawOval(fur, Offset(x - s * 0.8f, y - s * 1.05f), Size(s * 1.7f, s * 1.15f))
    drawCircle(fur, s * 0.44f, Offset(x + s * 0.48f, y - s * 1.0f))
    rotate(degrees = -10f + earTwitch * 16f, pivot = Offset(x + s * 0.48f, y - s * 1.3f)) {
        drawOval(fur, Offset(x + s * 0.30f, y - s * 2.0f), Size(s * 0.30f, s * 0.80f))
        drawOval(Color(0xFFEBA7B6), Offset(x + s * 0.35f, y - s * 1.92f), Size(s * 0.18f, s * 0.58f))
    }
    rotate(degrees = 8f - earTwitch * 12f, pivot = Offset(x + s * 0.48f, y - s * 1.3f)) {
        drawOval(fur, Offset(x + s * 0.52f, y - s * 1.98f), Size(s * 0.28f, s * 0.76f))
        drawOval(Color(0xFFEBA7B6), Offset(x + s * 0.57f, y - s * 1.90f), Size(s * 0.16f, s * 0.54f))
    }
    drawCircle(Color(0xFF3A3226), s * 0.07f, Offset(x + s * 0.62f, y - s * 1.06f))
    drawCircle(Color(0xFFD9A3A3), s * 0.06f, Offset(x + s * 0.78f, y - s * 0.95f))
    drawLine(fur, Offset(x - s * 0.7f, y - s * 0.25f), Offset(x - s * 0.8f, y), w * 0.006f, StrokeCap.Round)
}

internal fun DrawScope.hen(x: Float, y: Float, s: Float, peck: Float, w: Float) {
    val body = Color(0xFFF2E4CB)
    val wing = Color(0xFFDFCBA9)
    val red = Color(0xFFC9553F)
    // legs
    drawLine(Color(0xFFB8863F), Offset(x - s * 0.22f, y), Offset(x - s * 0.22f, y - s * 0.42f), w * 0.0035f, StrokeCap.Round)
    drawLine(Color(0xFFB8863F), Offset(x + s * 0.24f, y), Offset(x + s * 0.24f, y - s * 0.42f), w * 0.0035f, StrokeCap.Round)
    // tail feathers
    drawOval(body, Offset(x - s * 1.05f, y - s * 0.95f), Size(s * 0.85f, s * 0.62f))
    drawOval(wing, Offset(x - s * 1.15f, y - s * 1.15f), Size(s * 0.55f, s * 0.38f))
    // body
    drawOval(body, Offset(x - s * 0.72f, y - s * 1.02f), Size(s * 1.44f, s * 0.82f))
    drawOval(wing, Offset(x - s * 0.30f, y - s * 0.90f), Size(s * 0.62f, s * 0.42f))
    // head, dips to peck
    val hx = x + s * 0.62f + peck * s * 0.10f
    val hy = y - s * (0.86f + peck * 0.34f)
    drawCircle(body, s * 0.34f, Offset(hx, hy))
    drawCircle(red, s * 0.11f, Offset(hx - s * 0.05f, hy - s * 0.30f))
    drawCircle(red, s * 0.07f, Offset(hx + s * 0.08f, hy - s * 0.24f))
    // beak
    drawOval(Color(0xFFE8A63C), Offset(hx + s * 0.22f, hy - s * 0.10f), Size(s * 0.28f, s * 0.15f))
    drawCircle(Color(0xFF3A3226), s * 0.045f, Offset(hx + s * 0.10f, hy - s * 0.07f))
    // wattle
    drawCircle(red, s * 0.06f, Offset(hx + s * 0.22f, hy + s * 0.10f))
}

private fun DrawScope.fawn(x: Float, y: Float, s: Float, drink: Float, w: Float) {
    val tan = Color(0xFFD9B07A)
    val spot = Color(0xFFF6EEDC)
    // legs
    drawLine(tan, Offset(x - s * 0.35f, y), Offset(x - s * 0.30f, y - s * 0.85f), w * 0.005f, StrokeCap.Round)
    drawLine(tan, Offset(x + s * 0.30f, y), Offset(x + s * 0.34f, y - s * 0.85f), w * 0.005f, StrokeCap.Round)
    // body
    drawOval(tan, Offset(x - s * 0.62f, y - s * 1.55f), Size(s * 1.24f, s * 0.78f))
    // neck + head, lowered when drinking
    val headX = x + s * 0.55f
    val headY = y - s * (1.55f - drink * 0.75f)
    drawLine(tan, Offset(x + s * 0.35f, y - s * 1.35f), Offset(headX, headY), w * 0.008f, StrokeCap.Round)
    drawOval(tan, Offset(headX - s * 0.16f, headY - s * 0.16f), Size(s * 0.34f, s * 0.26f))
    drawOval(tan, Offset(headX - s * 0.30f, headY - s * 0.30f), Size(s * 0.14f, s * 0.24f))
    drawOval(tan, Offset(headX + s * 0.02f, headY - s * 0.32f), Size(s * 0.14f, s * 0.24f))
    drawCircle(Color(0xFF3A3226), s * 0.045f, Offset(headX + s * 0.06f, headY - s * 0.06f))
    // white spots
    drawCircle(spot, s * 0.045f, Offset(x - s * 0.22f, y - s * 1.35f))
    drawCircle(spot, s * 0.040f, Offset(x + s * 0.05f, y - s * 1.28f))
    drawCircle(spot, s * 0.035f, Offset(x - s * 0.42f, y - s * 1.25f))
}

private fun DrawScope.bee(x: Float, y: Float, s: Float) {
    drawOval(Color(0xFFFFD54F), Offset(x - s, y - s * 0.65f), Size(s * 2f, s * 1.3f))
    drawLine(Color(0xFF4A3A18), Offset(x - s * 0.25f, y - s * 0.6f), Offset(x - s * 0.25f, y + s * 0.6f), s * 0.16f)
    drawLine(Color(0xFF4A3A18), Offset(x + s * 0.25f, y - s * 0.6f), Offset(x + s * 0.25f, y + s * 0.6f), s * 0.16f)
    drawOval(Color.White.copy(alpha = 0.65f), Offset(x - s * 0.6f, y - s * 1.15f), Size(s * 0.9f, s * 0.55f))
    drawOval(Color.White.copy(alpha = 0.55f), Offset(x + s * 0.05f, y - s * 1.10f), Size(s * 0.8f, s * 0.5f))
}

private fun DrawScope.dragonfly(x: Float, y: Float, s: Float, w: Float) {
    val body = Color(0xFF3E6E7A)
    drawLine(body, Offset(x - s * 1.2f, y), Offset(x + s * 1.4f, y), w * 0.0022f, StrokeCap.Round)
    drawOval(Color.White.copy(alpha = 0.35f), Offset(x - s * 0.5f, y - s * 0.75f), Size(s * 1.0f, s * 0.55f))
    drawOval(Color.White.copy(alpha = 0.35f), Offset(x + s * 0.1f, y - s * 0.75f), Size(s * 1.0f, s * 0.55f))
    drawOval(Color.White.copy(alpha = 0.28f), Offset(x - s * 0.4f, y - s * 0.15f), Size(s * 0.9f, s * 0.45f))
    drawOval(Color.White.copy(alpha = 0.28f), Offset(x + s * 0.1f, y - s * 0.15f), Size(s * 0.9f, s * 0.45f))
    drawCircle(body, s * 0.18f, Offset(x + s * 1.4f, y))
}

/** The original garden as a Scene, unchanged visually. */
object GardenScene : Scene {
    override val id = "garden"
    override val palette = ScenePalette(Color(0xFF6DB84C), Color(0xFFFFE9A0))
    override val tapAnchor = Offset(0.5f, 0.80f)
    override val labels = listOf("Drop", "Pond", "Meadow", "Garden")
    override fun textColor(progress: Float) = Color(0xFF5A4326)
    override fun DrawScope.draw(frame: SceneFrame) = drawGarden(
        p = frame.progress,
        t = frame.time,
        pulse = frame.pulse,
        ripple = frame.ripple,
        shimmer = frame.shimmer,
        golden = frame.golden,
        celebrate = frame.celebrate,
        calm = frame.calm
    )
}

private fun DrawScope.fish(x: Float, y: Float, s: Float, w: Float) {
    drawOval(Color(0xFFF0A54B), Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    rotate(degrees = -20f, pivot = Offset(x - s, y)) {
        drawOval(Color(0xFFFFC47A), Offset(x - s * 1.7f, y - s * 0.4f), Size(s * 0.9f, s * 0.8f))
    }
    drawCircle(Color(0xFF4A3822), s * 0.09f, Offset(x + s * 0.55f, y - s * 0.1f))
    drawLine(Color.White.copy(alpha = 0.4f), Offset(x - s * 0.4f, y + s * 0.5f), Offset(x + s * 0.6f, y + s * 0.5f), w * 0.0015f)
}
