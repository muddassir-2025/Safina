package com.muddassir.safina

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val nightRnd = Random(59)
private val nightStars = List(110) {
    Triple(nightRnd.nextFloat(), nightRnd.nextFloat() * 0.62f, nightRnd.nextFloat())
}
private val nebulaBlobs = List(5) {
    Triple(nightRnd.nextFloat(), 0.10f + nightRnd.nextFloat() * 0.35f, nightRnd.nextFloat())
}
private val nightPath = Path()

object NightScene : Scene {
    override val id = "night"
    override val palette = ScenePalette(Color(0xFFC9B8FF), Color(0xFF8FA6E8))
    override val tapAnchor = Offset(0.5f, 0.62f)
    override val labels = listOf("Dusk", "Stars", "Moon", "Cosmos")

    override fun textColor(progress: Float): Color = Color(0xFFF3EFE2)

    override fun DrawScope.draw(frame: SceneFrame) {
        val p = frame.progress
        val t = frame.time
        val w = size.width
        val h = size.height
        val horizon = h * 0.64f
        val starA = seg(p, 0.05f, 0.40f)
        val moonA = seg(p, 0.05f, 0.30f)
        val milky = seg(p, 0.30f, 0.60f)
        val bloom = seg(p, 0.80f, 1f)

        // ----- sky -----
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF070B18),
                    lerp(Color(0xFF0D1526), Color(0xFF1B2A4A), milky),
                    Color(0xFF22304A)
                ),
                0f, horizon
            ),
            size = Size(w, horizon + 2f)
        )

        // ----- Milky Way band -----
        if (milky > 0f) {
            rotate(degrees = -24f, pivot = Offset(w * 0.5f, h * 0.3f)) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFFA9C4FF).copy(alpha = 0.10f * milky), Color.Transparent),
                        startY = h * 0.05f, endY = h * 0.55f
                    ),
                    topLeft = Offset(0f, h * 0.05f), size = Size(w, h * 0.50f)
                )
            }
            repeat(40) { i ->
                val x = (i * 0.137f + 0.03f) % 1f * w
                val y = (h * 0.16f + (i * 0.071f % 0.34f) * h) + sin((t + i) * TWO_PI) * h * 0.002f
                drawCircle(Color(0xFFDCE7FF).copy(alpha = 0.35f * milky), w * 0.0012f, Offset(x, y))
            }
        }

        // ----- nebula colours near completion -----
        if (bloom > 0f) nebulaBlobs.forEachIndexed { i, (x, y, ph) ->
            val col = listOf(Color(0xFF6E7BFF), Color(0xFF9A6EFF), Color(0xFF4FD3C4))[i % 3]
            softGlow(col, Offset(x * w, y * h), w * 0.28f, (0.10f + 0.03f * sin((t + ph) * TWO_PI)) * bloom)
        }

        // ----- stars appearing one by one -----
        if (starA > 0f) nightStars.forEachIndexed { i, (sx, sy, ph) ->
            val appear = ((starA * 1.35f) - i.toFloat() / nightStars.size).coerceIn(0f, 1f)
            if (appear > 0f) {
                val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin((t * (0.6f + ph) + ph * 2f) * TWO_PI))
                twinkleStar(sx * w, sy * h, w * (0.0012f + 0.0016f * ph), tw * appear)
            }
        }

        // ----- crescent moon with halo -----
        if (moonA > 0f) {
            val mx = lerpF(w * 0.80f, w * 0.76f, moonA)
            val my = lerpF(h * 0.56f, h * 0.18f, smooth(moonA))
            val mr = w * 0.055f
            softGlow(Color(0xFFE8E4FF), Offset(mx, my), mr * 3.4f, 0.20f * moonA)
            drawCircle(Color(0xFFF6F2E2).copy(alpha = moonA), mr, Offset(mx, my))
            drawCircle(Color(0xFF16203A).copy(alpha = moonA), mr * 0.92f, Offset(mx + mr * 0.45f, my - mr * 0.20f))
            // thin drifting cloud across the moon
            val cloudX = ((t * 0.6f + 0.2f) % 1f) * w * 1.2f - w * 0.1f
            drawOval(
                Color(0xFF2C3A56).copy(alpha = 0.35f * moonA),
                Offset(cloudX, my + mr * 0.2f), Size(w * 0.20f, h * 0.014f)
            )
        }

        // ----- dunes -----
        val duneFar = Color(0xFF121A2A)
        val duneMid = Color(0xFF0D1420)
        val duneNear = Color(0xFF090F18)
        drawPath(hillPath(nightPath, w, horizon + h * 0.01f, h * 0.05f, 0f), duneFar)
        drawPath(hillPath(nightPath, w, horizon + h * 0.08f, h * 0.06f, w * 0.25f), duneMid)
        drawPath(hillPath(nightPath, w, h * 0.92f, h * 0.05f, w * 0.1f), duneNear)
        // moonlit rims on the ridges
        if (moonA > 0f) {
            drawLine(Color(0xFF9FB0D0).copy(alpha = 0.35f * moonA), Offset(0f, horizon), Offset(w * 0.55f, horizon - h * 0.012f), w * 0.0025f)
            drawLine(Color(0xFFB9C6E0).copy(alpha = 0.25f * moonA), Offset(w * 0.25f, h * 0.855f), Offset(w, h * 0.86f), w * 0.0022f)
        }

        // ----- lone tree on the far dune -----
        val treeX = w * 0.18f
        val treeY = horizon + h * 0.015f
        drawLine(Color(0xFF0A0E16), Offset(treeX, treeY), Offset(treeX + w * 0.006f, treeY - h * 0.075f), w * 0.008f, StrokeCap.Round)
        drawLine(Color(0xFF0A0E16), Offset(treeX, treeY - h * 0.045f), Offset(treeX - w * 0.030f, treeY - h * 0.075f), w * 0.004f, StrokeCap.Round)
        drawLine(Color(0xFF0A0E16), Offset(treeX, treeY - h * 0.055f), Offset(treeX + w * 0.028f, treeY - h * 0.085f), w * 0.004f, StrokeCap.Round)

        // ----- campfire and tent -----
        val fireA = seg(p, 0.30f, 0.60f)
        val campX = w * 0.20f
        val campY = h * 0.90f
        // tent
        nightPath.reset()
        nightPath.moveTo(campX + w * 0.10f, campY)
        nightPath.lineTo(campX + w * 0.16f, campY - h * 0.065f)
        nightPath.lineTo(campX + w * 0.22f, campY)
        nightPath.close()
        drawPath(nightPath, Color(0xFF2A2A38))
        if (fireA > 0f) {
            val flick = 0.7f + 0.3f * sin((t * 6f) * TWO_PI)
            softGlow(Color(0xFFFFB35C), Offset(campX, campY - h * 0.012f), w * 0.16f, 0.35f * fireA * flick)
            nightPath.reset()
            nightPath.moveTo(campX - w * 0.012f, campY)
            nightPath.lineTo(campX, campY - h * 0.028f * flick)
            nightPath.lineTo(campX + w * 0.012f, campY)
            nightPath.close()
            drawPath(nightPath, Color(0xFFFFC46B).copy(alpha = fireA))
            drawLine(Color(0xFF3A2A1E), Offset(campX - w * 0.018f, campY), Offset(campX + w * 0.018f, campY), w * 0.006f, StrokeCap.Round)
            repeat(4) { i ->
                val u = (t * 0.8f + i * 0.25f) % 1f
                drawCircle(
                    Color(0xFFFFD08A).copy(alpha = 0.6f * (1f - u) * fireA),
                    w * 0.0018f,
                    Offset(campX + sin((t + u) * TWO_PI) * w * 0.02f, campY - h * 0.02f - u * h * 0.08f)
                )
            }
            // lantern by the tent
            softGlow(Color(0xFFFFD37A), Offset(campX + w * 0.10f, campY - h * 0.045f), w * 0.05f, 0.35f * fireA)
            drawRect(Color(0xFFFFD37A).copy(alpha = fireA), Offset(campX + w * 0.098f, campY - h * 0.048f), Size(w * 0.006f, h * 0.008f))
        }

        // ----- camel caravan -----
        val camelA = seg(p, 0.50f, 0.68f)
        if (camelA > 0f) {
            val walk = (t * 0.25f) % 1f
            camel(w * (0.30f + walk * 0.35f), h * 0.845f, w * 0.045f * camelA, w, t)
            camel(w * (0.18f + walk * 0.35f), h * 0.865f, w * 0.036f * camelA, w, t + 0.15f)
        }

        // ----- fennec on a dune -----
        val fennecA = seg(p, 0.60f, 0.78f)
        if (fennecA > 0f) {
            val fx = w * 0.82f
            val fy = h * 0.885f
            drawOval(Color(0xFFC9A98A).copy(alpha = fennecA), Offset(fx - w * 0.018f, fy - w * 0.014f), Size(w * 0.036f, w * 0.018f))
            drawCircle(Color(0xFFC9A98A).copy(alpha = fennecA), w * 0.010f, Offset(fx + w * 0.016f, fy - w * 0.018f))
            drawOval(Color(0xFFD9BC9E).copy(alpha = fennecA), Offset(fx + w * 0.008f, fy - w * 0.040f), Size(w * 0.008f, w * 0.020f))
            drawOval(Color(0xFFD9BC9E).copy(alpha = fennecA), Offset(fx + w * 0.020f, fy - w * 0.040f), Size(w * 0.008f, w * 0.020f))
            drawCircle(Color(0xFF1A1A22), w * 0.0022f, Offset(fx + w * 0.020f, fy - w * 0.018f))
        }

        // ----- oasis and star reflections -----
        val oasisA = seg(p, 0.80f, 1f)
        if (oasisA > 0f) {
            val ox = w * 0.58f
            val oy = h * 0.94f
            drawOval(Color(0xFF122339).copy(alpha = 0.9f * oasisA), Offset(ox - w * 0.16f, oy - w * 0.045f), Size(w * 0.32f, w * 0.09f))
            drawOval(
                Color(0xFF6E9BCB).copy(alpha = 0.30f * oasisA),
                Offset(ox - w * 0.10f, oy - w * 0.030f), Size(w * 0.20f, w * 0.03f)
            )
            repeat(6) { i ->
                val sx = ox + (i - 2.5f) * w * 0.035f
                val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin((t * 2f + i) * TWO_PI))
                drawCircle(Color(0xFFE8F0FF).copy(alpha = 0.5f * tw * oasisA), w * 0.0018f, Offset(sx, oy - w * 0.018f))
            }
        }

        // ----- shooting stars -----
        repeat(2) { i ->
            val cycle = (t * 0.55f + i * 0.47f) % 1f
            if (cycle < 0.055f) {
                val u = cycle / 0.055f
                val sx = w * (0.20f + i * 0.35f) + u * w * 0.22f
                val sy = h * (0.10f + i * 0.09f) + u * h * 0.10f
                drawLine(
                    Color(0xFFEAF1FF).copy(alpha = 0.85f * (1f - u)),
                    Offset(sx, sy), Offset(sx - w * 0.09f, sy - h * 0.035f), w * 0.0025f, StrokeCap.Round
                )
            }
        }

        // ----- tap: a star drops and lands with a soft ring -----
        if (frame.pulse < 1f) {
            val u = frame.pulse
            val lx = w * 0.5f
            val ly = h * 0.62f
            drawLine(
                Color(0xFFFFF3C8).copy(alpha = 0.8f * (1f - u)),
                Offset(lx + w * 0.04f * (1f - u), ly - h * 0.04f * (1f - u)),
                Offset(lx, ly), w * 0.003f, StrokeCap.Round
            )
            drawOval(
                Color(0xFFFFEDB8).copy(alpha = 0.45f * (1f - u)),
                Offset(lx - w * 0.09f * u, ly - w * 0.03f * u), Size(w * 0.18f * u, w * 0.06f * u),
                style = Stroke(w * 0.0025f * frame.ripple)
            )
            softGlow(Color(0xFFFFE7A8), Offset(lx, ly), w * 0.10f * u, 0.35f * (1f - u))
        }

        // ----- completion: a shower of meteors -----
        if (frame.celebrate > 0f) repeat(12) { i ->
            val u = (frame.celebrate * 1.1f + i * 0.06f) % 1.15f
            if (u < 1f) {
                val sx = w * (0.05f + (i * 0.083f) % 0.9f) + u * w * 0.18f
                val sy = h * (0.04f + (i * 0.061f) % 0.35f) + u * h * 0.16f
                drawLine(
                    Color(0xFFFFF6D8).copy(alpha = 0.9f * (1f - u)),
                    Offset(sx, sy), Offset(sx - w * 0.10f, sy - h * 0.04f), w * 0.003f, StrokeCap.Round
                )
            }
        }
        if (frame.golden > 0f) {
            softGlow(Color(0xFFB9A6FF), Offset(w * 0.5f, h * 0.30f), w * 0.9f, 0.10f * frame.golden)
        }
    }
}

/* ---------- pieces ---------- */
private fun DrawScope.camel(x: Float, y: Float, s: Float, w: Float, time: Float) {
    val body = Color(0xFF171B26)
    drawOval(body, Offset(x - s, y - s * 0.9f), Size(s * 2f, s * 0.7f))
    drawCircle(body, s * 0.30f, Offset(x - s * 0.05f, y - s * 1.15f))
    drawLine(body, Offset(x + s * 0.75f, y - s * 0.85f), Offset(x + s * 0.95f, y - s * 1.5f), s * 0.30f, StrokeCap.Round)
    drawOval(body, Offset(x + s * 0.85f, y - s * 1.75f), Size(s * 0.55f, s * 0.30f))
    val step = sin(time * TWO_PI * 2f)
    drawLine(body, Offset(x - s * 0.45f, y - s * 0.3f), Offset(x - s * 0.55f - step * s * 0.12f, y), s * 0.16f, StrokeCap.Round)
    drawLine(body, Offset(x + s * 0.35f, y - s * 0.3f), Offset(x + s * 0.45f + step * s * 0.12f, y), s * 0.16f, StrokeCap.Round)
    drawLine(body, Offset(x - s * 0.9f, y - s * 0.8f), Offset(x - s * 1.1f, y - s * 0.5f), s * 0.08f, StrokeCap.Round)
    drawCircle(Color(0xFF3A3A48), s * 0.05f, Offset(x + s * 1.10f, y - s * 1.72f))
    drawLine(body, Offset(x, y - s * 0.85f), Offset(x, y - s * 1.0f), w * 0.003f)
}
