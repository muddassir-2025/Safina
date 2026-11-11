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
private val nightStars = List(190) {
    Triple(nightRnd.nextFloat(), nightRnd.nextFloat() * 0.70f, nightRnd.nextFloat())
}
private val brightStars = List(9) {
    Pair(nightRnd.nextFloat(), 0.03f + nightRnd.nextFloat() * 0.44f)
}
private val snowSparkles = List(26) {
    Triple(nightRnd.nextFloat(), 0.80f + nightRnd.nextFloat() * 0.18f, nightRnd.nextFloat())
}
private val nebulaBlobs = List(5) {
    Triple(nightRnd.nextFloat(), 0.10f + nightRnd.nextFloat() * 0.30f, nightRnd.nextFloat())
}
private val nightPath = Path()

/** A small, distant snowy pine. Quiet enough to leave the sky in charge. */
private fun DrawScope.snowPine(x: Float, baseY: Float, height: Float, w: Float, alpha: Float) {
    val trunk = Color(0xFF0B1119).copy(alpha = alpha)
    drawLine(trunk, Offset(x, baseY), Offset(x, baseY - height), w * 0.006f, StrokeCap.Round)
    repeat(3) { tier ->
        val ty = baseY - height * (0.34f + tier * 0.21f)
        val half = height * (0.34f - tier * 0.08f)
        val top = ty - height * 0.33f
        nightPath.reset()
        nightPath.moveTo(x, top)
        nightPath.lineTo(x - half, ty)
        nightPath.lineTo(x + half, ty)
        nightPath.close()
        drawPath(nightPath, Color(0xFF13202B).copy(alpha = alpha))
        nightPath.reset()
        nightPath.moveTo(x, top + height * 0.03f)
        nightPath.lineTo(x - half * 0.55f, ty - height * 0.010f)
        nightPath.lineTo(x - half * 0.20f, ty - height * 0.004f)
        nightPath.lineTo(x + half * 0.18f, ty - height * 0.012f)
        nightPath.lineTo(x + half * 0.50f, ty - height * 0.002f)
        nightPath.close()
        drawPath(nightPath, Color(0xFFC6D8E9).copy(alpha = 0.38f * alpha))
    }
}

object NightScene : Scene {
    override val id = "night"
    override val palette = ScenePalette(Color(0xFFC9B8FF), Color(0xFF8FA6E8))
    override val tapAnchor = Offset(0.5f, 0.84f)
    override val labels = listOf("Dusk", "Stars", "Snow", "Stillness")

    override fun textColor(progress: Float): Color = Color(0xFFF3EFE2)

    override fun DrawScope.draw(frame: SceneFrame) {
        val p = frame.progress
        val t = frame.time
        val e = frame.elapsed
        val w = size.width
        val h = size.height
        val horizon = h * 0.76f
        val starA = seg(p, 0.04f, 0.34f)
        val moonA = seg(p, 0.04f, 0.26f)
        val snowA = seg(p, 0.20f, 0.55f)
        val milky = seg(p, 0.22f, 0.58f)
        val auroraA = seg(p, 0.74f, 0.98f) * (0.55f + 0.45f * sin(t * TWO_PI))

        // ----- the sky gets most of the frame -----
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF04070F),
                    Color(0xFF0A1220),
                    lerp(Color(0xFF182844), Color(0xFF22364F), snowA * 0.5f),
                    Color(0xFF2E4258)
                ),
                0f, horizon + 2f
            ),
            size = Size(w, horizon + 2f)
        )

        // ----- Milky Way, a wide soft river of light -----
        if (milky > 0f) {
            rotate(degrees = -26f, pivot = Offset(w * 0.5f, h * 0.32f)) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF9FB6FF).copy(alpha = 0.06f * milky),
                            Color(0xFFC9D6FF).copy(alpha = 0.16f * milky),
                            Color(0xFF9FB6FF).copy(alpha = 0.06f * milky),
                            Color.Transparent
                        ),
                        startY = h * 0.02f, endY = h * 0.64f
                    ),
                    topLeft = Offset(0f, h * 0.02f), size = Size(w, h * 0.62f)
                )
                // dense dust along the band
                repeat(90) { i ->
                    val x = ((i * 0.137f + 0.02f) % 1f) * w * 1.3f - w * 0.15f
                    val y = h * 0.10f + ((i * 0.071f) % 0.36f) * h +
                        sin((t + i * 0.3f) * TWO_PI) * h * 0.0018f
                    val bright = 0.25f + 0.45f * (0.5f + 0.5f * sin((t * 0.7f + i) * TWO_PI))
                    drawCircle(
                        Color(0xFFDDE8FF).copy(alpha = (0.20f + 0.45f * (i % 5) / 4f) * bright * milky),
                        w * (0.0007f + 0.0011f * (i % 5) / 4f),
                        Offset(x, y)
                    )
                }
            }
            nebulaBlobs.forEachIndexed { i, (x, y, ph) ->
                val col = listOf(Color(0xFF7B87FF), Color(0xFF9E74FF), Color(0xFF5AD6C6))[i % 3]
                softGlow(
                    col, Offset(x * w, y * h), w * 0.30f,
                    (0.045f + 0.02f * sin((t + ph) * TWO_PI)) * milky
                )
            }
        }

        // ----- faint aurora, only as the dhikr nears completion -----
        if (auroraA > 0.01f) {
            repeat(2) { i ->
                val col = listOf(Color(0xFF6FE3B4), Color(0xFF8BB8E8))[i]
                val yBase = h * (0.10f + i * 0.055f)
                nightPath.reset()
                nightPath.moveTo(0f, yBase)
                var x = 0f
                while (x <= w) {
                    nightPath.lineTo(x, yBase + sin((x / w * 1.6f + t * 1.4f + i) * TWO_PI) * h * 0.022f)
                    x += w / 16f
                }
                drawPath(nightPath, col.copy(alpha = 0.07f * auroraA), style = Stroke(w * 0.055f, cap = StrokeCap.Round))
                drawPath(nightPath, col.copy(alpha = 0.06f * auroraA), style = Stroke(w * 0.024f, cap = StrokeCap.Round))
            }
        }

        // ----- stars: many, gently twinkling -----
        if (starA > 0f) nightStars.forEachIndexed { i, (sx, sy, ph) ->
            val appear = ((starA * 1.4f) - i.toFloat() / nightStars.size).coerceIn(0f, 1f)
            if (appear > 0f) {
                val tw = 0.30f + 0.70f * (0.5f + 0.5f * sin((t * (0.4f + ph * 0.8f) + ph * 2f) * TWO_PI))
                val r = w * (0.0007f + 0.0013f * ph)
                drawCircle(Color(0xFFF2F6FF).copy(alpha = appear * (0.30f + 0.70f * tw)), r, Offset(sx * w, sy * h))
            }
        }
        // a handful of bright stars with soft four-point flares
        brightStars.forEachIndexed { i, (sx, sy) ->
            val a = seg(p, 0.06f + i * 0.02f, 0.30f + i * 0.02f)
            if (a > 0f) {
                val tw = 0.55f + 0.45f * sin((t * 1.1f + i * 0.37f) * TWO_PI)
                val r = w * 0.0022f
                softGlow(Color(0xFFEAF1FF), Offset(sx * w, sy * h), r * 7f, 0.12f * a * tw)
                drawCircle(Color(0xFFFBFDFF).copy(alpha = a * tw), r, Offset(sx * w, sy * h))
                drawLine(
                    Color(0xFFDCE8FF).copy(alpha = a * tw * 0.5f),
                    Offset(sx * w - r * 5f, sy * h), Offset(sx * w + r * 5f, sy * h), r * 0.30f
                )
                drawLine(
                    Color(0xFFDCE8FF).copy(alpha = a * tw * 0.5f),
                    Offset(sx * w, sy * h - r * 5f), Offset(sx * w, sy * h + r * 5f), r * 0.30f
                )
            }
        }

        // ----- a full moon, high in the sky -----
        if (moonA > 0f) {
            val mx = lerpF(w * 0.80f, w * 0.76f, moonA)
            val my = lerpF(h * 0.62f, h * 0.17f, smooth(moonA))
            val mr = w * 0.075f
            softGlow(Color(0xFFCFDDFF), Offset(mx, my), mr * 5.2f, 0.13f * moonA)
            softGlow(Color(0xFFE8EDFF), Offset(mx, my), mr * 2.4f, 0.18f * moonA)
            drawCircle(Color(0xFFFBF6E4).copy(alpha = moonA), mr, Offset(mx, my))
            drawCircle(Color(0xFFF2EDD8).copy(alpha = 0.55f * moonA), mr * 0.94f, Offset(mx, my))
            // soft maria
            drawCircle(Color(0xFFD9D3BE).copy(alpha = 0.20f * moonA), mr * 0.22f, Offset(mx - mr * 0.30f, my - mr * 0.16f))
            drawCircle(Color(0xFFD9D3BE).copy(alpha = 0.16f * moonA), mr * 0.14f, Offset(mx + mr * 0.22f, my + mr * 0.24f))
            drawCircle(Color(0xFFD9D3BE).copy(alpha = 0.14f * moonA), mr * 0.10f, Offset(mx + mr * 0.34f, my - mr * 0.30f))
            drawCircle(Color(0xFFD9D3BE).copy(alpha = 0.12f * moonA), mr * 0.08f, Offset(mx - mr * 0.10f, my + mr * 0.40f))
            // thin drifting cloud over the moon
            val cloudX = ((e * 0.008f + 0.12f) % 1f) * w * 1.3f - w * 0.15f
            val cloudY = my + mr * 1.25f
            drawOval(
                Color(0xFF39495F).copy(alpha = 0.26f * moonA),
                Offset(cloudX, cloudY), Size(w * 0.26f, h * 0.012f)
            )
            drawOval(
                Color(0xFF455669).copy(alpha = 0.16f * moonA),
                Offset(cloudX + w * 0.05f, cloudY + h * 0.008f), Size(w * 0.18f, h * 0.009f)
            )
        }

        // ----- shooting stars, driven by the monotonic clock so they feel random -----
        repeat(4) { i ->
            val span = 0.75f + i * 0.18f
            val raw = (e * 0.055f / span + i * 0.41f) % 1f
            val streak = 0.07f
            if (raw < streak) {
                val u = raw / streak
                val startX = w * (0.06f + (i * 0.23f) % 0.72f)
                val startY = h * (0.06f + (i * 0.11f) % 0.34f)
                val travel = smooth(u)
                val head = Offset(startX + travel * w * 0.24f, startY + travel * h * 0.12f)
                val a = 0.9f * (1f - u) * (1f - u)
                drawLine(
                    Color(0xFFEFF4FF).copy(alpha = a),
                    head, Offset(head.x - w * 0.14f, head.y - h * 0.060f),
                    w * 0.0030f, StrokeCap.Round
                )
                drawLine(
                    Color(0xFFDCE7FF).copy(alpha = a * 0.45f),
                    head, Offset(head.x - w * 0.06f, head.y - h * 0.026f),
                    w * 0.0075f, StrokeCap.Round
                )
                softGlow(Color(0xFFEAF2FF), head, w * 0.05f, 0.20f * a)
            }
        }

        // ----- a low, distant mountain line -----
        val far = lerp(Color(0xFF223348), Color(0xFF344A62), snowA)
        nightPath.reset()
        nightPath.moveTo(0f, horizon)
        nightPath.lineTo(w * 0.12f, h * 0.665f)
        nightPath.lineTo(w * 0.24f, h * 0.705f)
        nightPath.lineTo(w * 0.38f, h * 0.655f)
        nightPath.lineTo(w * 0.52f, h * 0.700f)
        nightPath.lineTo(w * 0.66f, h * 0.650f)
        nightPath.lineTo(w * 0.82f, h * 0.702f)
        nightPath.lineTo(w, h * 0.668f)
        nightPath.lineTo(w, horizon)
        nightPath.lineTo(0f, horizon)
        nightPath.close()
        drawPath(nightPath, far)

        val mid = lerp(Color(0xFF1B2B3F), Color(0xFF2B3F57), snowA)
        nightPath.reset()
        nightPath.moveTo(0f, horizon)
        nightPath.lineTo(w * 0.20f, h * 0.715f)
        nightPath.lineTo(w * 0.36f, h * 0.700f)
        nightPath.lineTo(w * 0.56f, h * 0.725f)
        nightPath.lineTo(w * 0.74f, h * 0.702f)
        nightPath.lineTo(w * 0.90f, h * 0.722f)
        nightPath.lineTo(w, h * 0.705f)
        nightPath.lineTo(w, horizon)
        nightPath.close()
        drawPath(nightPath, mid)

        // moonlit ridgelines
        if (moonA > 0f) {
            drawLine(
                Color(0xFFA9C0D8).copy(alpha = 0.18f * moonA),
                Offset(0f, h * 0.668f), Offset(w * 0.38f, h * 0.657f),
                w * 0.0018f, StrokeCap.Round
            )
            drawLine(
                Color(0xFF9FB6CE).copy(alpha = 0.12f * moonA),
                Offset(w * 0.62f, h * 0.703f), Offset(w, h * 0.669f),
                w * 0.0016f, StrokeCap.Round
            )
        }

        // ----- wide snowfield below -----
        drawRect(
            Brush.verticalGradient(
                listOf(
                    lerp(Color(0xFF283D54), Color(0xFF324B63), snowA),
                    Color(0xFF17283A),
                    Color(0xFF0E1A28)
                ),
                startY = h * 0.745f, endY = h
            ),
            topLeft = Offset(0f, h * 0.745f), size = Size(w, h * 0.255f)
        )
        // one soft crest sweeping across the snow
        nightPath.reset()
        nightPath.moveTo(0f, h * 0.82f)
        var x = 0f
        while (x <= w) {
            nightPath.lineTo(x, h * 0.82f + sin((x / w * 1.4f + 0.4f) * TWO_PI) * h * 0.010f)
            x += w / 18f
        }
        nightPath.lineTo(w, h)
        nightPath.lineTo(0f, h)
        nightPath.close()
        drawPath(nightPath, lerp(Color(0xFF20344A), Color(0xFF2C465F), snowA))
        drawLine(
            Color(0xFFD6E6F6).copy(alpha = 0.30f * snowA),
            Offset(0f, h * 0.816f), Offset(w, h * 0.812f),
            w * 0.0022f, StrokeCap.Round
        )
        // faint moonlit path on the snow
        softGlow(Color(0xFFBBD2EA), Offset(w * 0.74f, h * 0.92f), w * 0.42f, 0.08f * moonA)
        if (snowA > 0.01f) snowSparkles.forEach { (sx, sy, ph) ->
            val tw = 0.30f + 0.70f * (0.5f + 0.5f * sin((t * 2.2f + ph * 3f) * TWO_PI))
            drawCircle(
                Color(0xFFE6F1FF).copy(alpha = snowA * tw * 0.45f),
                w * 0.0011f, Offset(sx * w, sy * h)
            )
        }

        // ----- just a few small pines at the edges -----
        val treeA = seg(p, 0.14f, 0.40f)
        if (treeA > 0f) {
            snowPine(w * 0.07f, h * 0.935f, h * 0.145f, w, treeA)
            snowPine(w * 0.155f, h * 0.955f, h * 0.095f, w, treeA * 0.9f)
            snowPine(w * 0.95f, h * 0.93f, h * 0.125f, w, treeA * 0.95f)
        }

        // ----- tap: a ripple of starlight on the snow -----
        if (frame.pulse < 1f) {
            val u = frame.pulse
            val cx = w * 0.5f
            val cy = h * 0.86f
            softGlow(Color(0xFFDCE9FF), Offset(cx, cy), w * 0.16f * u, 0.20f * (1f - u))
            drawOval(
                Color(0xFFE8F1FF).copy(alpha = 0.42f * (1f - u)),
                Offset(cx - w * 0.10f * u, cy - w * 0.035f * u),
                Size(w * 0.20f * u, w * 0.07f * u),
                style = Stroke(w * 0.0024f * frame.ripple)
            )
            repeat(5) { i ->
                val a = i * 1.256f
                drawCircle(
                    Color(0xFFFFF4D0).copy(alpha = 0.65f * (1f - u)),
                    w * 0.0022f,
                    Offset(cx + cos(a) * w * 0.07f * u, cy + sin(a) * h * 0.025f * u)
                )
            }
        }

        // ----- completion: the sky blooms and meteors fall -----
        if (frame.golden > 0f) {
            softGlow(Color(0xFF9FB6FF), Offset(w * 0.5f, h * 0.28f), w * 1.1f, 0.10f * frame.golden)
            softGlow(Color(0xFF7FE3C0), Offset(w * 0.2f, h * 0.18f), w * 0.7f, 0.07f * frame.golden)
        }
        if (frame.celebrate > 0f) repeat(14) { i ->
            val u = (frame.celebrate * 1.1f + i * 0.06f) % 1.15f
            if (u < 1f) {
                val sx = w * (0.05f + (i * 0.083f) % 0.9f) + u * w * 0.16f
                val sy = h * (0.04f + (i * 0.061f) % 0.34f) + u * h * 0.14f
                drawLine(
                    Color(0xFFFFF6D8).copy(alpha = 0.85f * (1f - u)),
                    Offset(sx, sy), Offset(sx - w * 0.09f, sy - h * 0.036f),
                    w * 0.0026f, StrokeCap.Round
                )
            }
        }
    }
}
