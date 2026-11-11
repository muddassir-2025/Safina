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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------- fixed scenery ---------- */
private class Flake(val x: Float, val phase: Float, val speed: Float, val size: Float, val depth: Float)

private val winterRnd = Random(23)
private val flakes = List(90) {
    Flake(
        winterRnd.nextFloat(), winterRnd.nextFloat(),
        0.20f + winterRnd.nextFloat() * 0.45f,
        0.0022f + winterRnd.nextFloat() * 0.0038f,
        winterRnd.nextFloat()
    )
}
private val winterStars = List(70) {
    Triple(winterRnd.nextFloat(), winterRnd.nextFloat() * 0.50f, winterRnd.nextFloat())
}
private val winterTrees = listOf(
    0.06f to 0.85f, 0.20f to 1.05f, 0.81f to 1.02f, 0.94f to 0.80f
)
private val winterPath = Path()
private val auroraPath = Path()

object WinterScene : Scene {
    override val id = "winter"
    override val palette = ScenePalette(Color(0xFFBFE3F0), Color(0xFFE8F6FF))
    override val tapAnchor = Offset(0.52f, 0.80f)
    override val labels = listOf("Frost", "Snow", "Thaw", "Aurora")

    override fun textColor(progress: Float): Color =
        lerp(Color(0xFF35424E), Color(0xFFF6F2E6), seg(progress, 0.62f, 0.86f))

    override fun DrawScope.draw(frame: SceneFrame) {
        val p = frame.progress
        val t = frame.time
        val w = size.width
        val h = size.height
        val horizon = h * 0.56f
        val px = w * 0.52f
        val py = h * 0.80f
        val prx = w * 0.27f
        val pry = prx * 0.17f
        val dusk = seg(p, 0.70f, 1f)
        val warm = seg(p, 0.40f, 0.60f) * (1f - dusk)
        val snowA = seg(p, 0.05f, 0.30f)

        // ----- sky -----
        drawRect(
            Brush.verticalGradient(
                listOf(
                    lerp(Color(0xFF9FB0BF), Color(0xFF232847), dusk),
                    lerp(lerp(Color(0xFFC9D3DB), Color(0xFFF0C3A8), warm), Color(0xFF7A5578), dusk)
                ),
                0f, horizon
            ),
            size = Size(w, horizon + 2f)
        )
        if (warm > 0.01f) softGlow(Color(0xFFFFD9B0), Offset(w * 0.72f, horizon), w * 0.42f, 0.30f * warm)

        // ----- stars and aurora -----
        if (dusk > 0.01f) {
            winterStars.forEach { (sx, sy, ph) ->
                val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin((t * 2f + ph * 3f) * TWO_PI))
                twinkleStar(sx * w, sy * h, w * 0.0026f, tw * dusk)
            }
            repeat(3) { i ->
                val a = dusk * (0.15f + 0.06f * sin((t * 1.5f + i * 0.3f) * TWO_PI))
                val col = listOf(Color(0xFF7BE0B0), Color(0xFF6FD6E8), Color(0xFFB58CFF))[i]
                val yBase = h * (0.14f + i * 0.055f)
                auroraPath.reset()
                auroraPath.moveTo(0f, yBase)
                var x = 0f
                while (x <= w) {
                    auroraPath.lineTo(
                        x,
                        yBase + sin((x / w * 2.2f + t * 2f + i) * TWO_PI) * h * 0.035f
                    )
                    x += w / 12f
                }
                drawPath(auroraPath, col.copy(alpha = a), style = Stroke(w * 0.085f, cap = StrokeCap.Round))
                drawPath(auroraPath, col.copy(alpha = a * 0.6f), style = Stroke(w * 0.04f, cap = StrokeCap.Round))
            }
        }

        // ----- hills -----
        val hillCol = lerp(Color(0xFF8C979A), Color(0xFFE8F1F4), snowA)
        drawPath(hillPath(winterPath, w, horizon, h * 0.05f, 0f), hillCol)
        drawPath(
            hillPath(winterPath, w, horizon + h * 0.03f, h * 0.04f, w * 0.2f),
            lerp(Color(0xFF7F8A8E), Color(0xFFD8E6EA), snowA)
        )

        // ----- ground and the feathered snow patch -----
        drawRect(
            lerp(Color(0xFFB4B1A0), Color(0xFFE9EFF0), snowA * 0.9f),
            Offset(0f, horizon), Size(w, h - horizon)
        )
        val patchR = lerpF(w * 0.10f, w * 1.70f, smooth(snowA))
        clipRect(0f, horizon, w, h) {
            featherPatch(Color(0xFFF9FCFE).copy(alpha = 0.95f), Offset(px, py), patchR)
        }

        // ----- frozen pond, thawing from the middle -----
        drawOval(Color(0xFF70838E), Offset(px - prx, py - pry), Size(prx * 2, pry * 2))
        val thaw = smooth(seg(p, 0.40f, 0.60f))
        val crackA = 1f - thaw
        if (crackA > 0.01f) repeat(5) { i ->
            val a = i * 1.3f
            val sx = px + cos(a) * prx * 0.55f
            val sy = py + sin(a) * pry * 0.5f
            drawLine(
                Color(0xFFB9C7CD).copy(alpha = 0.55f * crackA),
                Offset(sx, sy),
                Offset(sx + cos(a + 1.1f) * prx * 0.35f, sy + sin(a + 1.1f) * pry * 0.35f),
                w * 0.0022f
            )
        }
        if (thaw > 0f) {
            val wr = prx * (0.32f + 0.62f * thaw)
            val wh = pry * (0.32f + 0.62f * thaw)
            drawOval(
                lerp(Color(0xFF9BC7D8), Color(0xFFD3EDF7), thaw),
                Offset(px - wr, py - wh), Size(wr * 2, wh * 2)
            )
            drawOval(
                Color.White.copy(alpha = 0.42f),
                Offset(px - wr * 0.6f, py - wh * 0.62f), Size(wr * 1.1f, wh * 0.28f)
            )
        }
        // tap: a snowflake falls and a frost ring spreads
        if (frame.pulse < 1f) {
            val r = 0.2f + 0.8f * frame.pulse
            drawOval(
                Color(0xFFE8F8FF).copy(alpha = 0.7f * (1f - frame.pulse)),
                Offset(px - prx * r, py - pry * r), Size(prx * 2 * r, pry * 2 * r),
                style = Stroke(w * 0.004f * frame.ripple)
            )
            if (frame.pulse < 0.35f) {
                val drop = frame.pulse / 0.35f
                val dy = lerpF(py - h * 0.16f, py, drop)
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.010f, Offset(px, dy))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(px - w * 0.018f, dy), Offset(px + w * 0.018f, dy), w * 0.0025f)
                drawLine(Color.White.copy(alpha = 0.7f), Offset(px, dy - w * 0.018f), Offset(px, dy + w * 0.018f), w * 0.0025f)
            }
        }
        drawOval(
            Color(0xFF8B9AA3), Offset(px - prx, py - pry), Size(prx * 2, pry * 2),
            style = Stroke(w * 0.006f)
        )

        // ----- snowfall (3 depths) -----
        if (snowA > 0.01f) flakes.forEach { f ->
            val u = (t * (0.6f + f.speed) + f.phase) % 1f
            val fy = lerpF(-h * 0.05f, h * 1.02f, u)
            val fx = f.x * w + sin((t * 1.2f + f.phase * 3f) * TWO_PI) * w * 0.02f
            drawCircle(
                Color.White.copy(alpha = snowA * (0.25f + 0.6f * f.depth)),
                w * f.size * (0.7f + f.depth), Offset(fx, fy)
            )
        }

        // ----- trees: bare, then snowy pines tier by tier -----
        val pineA = smooth(seg(p, 0.20f, 0.50f))
        winterTrees.forEach { (fx, s) ->
            val bx = fx * w
            val by = horizon + h * 0.04f
            val th = h * 0.125f * s
            val bark = Color(0xFF2F3438)
            drawLine(bark, Offset(bx, by), Offset(bx, by - th), w * 0.010f * s, StrokeCap.Round)
            drawLine(bark, Offset(bx, by - th * 0.60f), Offset(bx - th * 0.30f, by - th * 0.95f), w * 0.005f * s, StrokeCap.Round)
            drawLine(bark, Offset(bx, by - th * 0.70f), Offset(bx + th * 0.28f, by - th * 1.00f), w * 0.005f * s, StrokeCap.Round)
            if (pineA > 0f) repeat(3) { tier ->
                val ty = by - th * (0.38f + tier * 0.26f)
                val half = th * (0.58f - tier * 0.14f) * pineA
                val top = ty - th * 0.44f * pineA
                winterPath.reset()
                winterPath.moveTo(bx, top)
                winterPath.lineTo(bx - half, ty)
                winterPath.lineTo(bx + half, ty)
                winterPath.close()
                drawPath(winterPath, Color(0xFF2E5A44).copy(alpha = 0.94f * pineA))
                drawLine(
                    Color.White.copy(alpha = 0.85f * pineA),
                    Offset(bx - half * 0.92f, ty), Offset(bx + half * 0.92f, ty),
                    w * 0.006f, StrokeCap.Round
                )
            }
        }

        // ----- life around the cabin (snowman, cat, hen) -----
        val lifeA = seg(p, 0.50f, 0.80f)
        if (lifeA > 0f) {
            snowman(w * 0.36f, h * 0.905f, w * 0.086f * lifeA, w = w)
            cat(w * 0.635f, h * 0.905f, w * 0.052f * lifeA, tail = sin(t * TWO_PI * 1.4f), w = w)
            hen(
                w * 0.565f, h * 0.945f, w * 0.042f * lifeA,
                peck = (0.5f + 0.5f * sin(t * TWO_PI * 1.3f)).coerceAtMost(1f), w = w
            )
        }

        // robins on the left tree branches
        if (lifeA > 0f) {
            val rx = 0.22f * w
            val ry = horizon + h * 0.04f - h * 0.075f
            robin(rx + w * 0.02f, ry, w * 0.010f * lifeA, w)
            robin(rx - w * 0.03f, ry + h * 0.015f, w * 0.009f * lifeA, w)
        }

        // warm cabin in the centre, framed by the trees
        if (lifeA > 0f) {
            val cx = w * 0.52f
            val cy = horizon + h * 0.012f
            val cw = w * 0.19f
            val ch = h * 0.088f
            drawRect(Color(0xFF3B3A40), Offset(cx - cw / 2f, cy - ch), Size(cw, ch))
            winterPath.reset()
            winterPath.moveTo(cx - cw * 0.62f, cy - ch)
            winterPath.lineTo(cx, cy - ch - h * 0.062f)
            winterPath.lineTo(cx + cw * 0.62f, cy - ch)
            winterPath.close()
            drawPath(winterPath, Color(0xFF2C2B31))
            // snow on the roof
            winterPath.reset()
            winterPath.moveTo(cx - cw * 0.60f, cy - ch - h * 0.004f)
            winterPath.lineTo(cx, cy - ch - h * 0.066f)
            winterPath.lineTo(cx + cw * 0.60f, cy - ch - h * 0.004f)
            winterPath.lineTo(cx + cw * 0.52f, cy - ch - h * 0.012f)
            winterPath.lineTo(cx, cy - ch - h * 0.052f)
            winterPath.lineTo(cx - cw * 0.52f, cy - ch - h * 0.012f)
            winterPath.close()
            drawPath(winterPath, Color(0xFFF7FBFF).copy(alpha = 0.95f))
            val flicker = 0.75f + 0.25f * sin(t * TWO_PI * 4f)
            softGlow(Color(0xFFFFC46B), Offset(cx, cy - ch * 0.45f), cw * 0.62f, 0.55f * lifeA * flicker)
            // two windows + door
            drawRect(Color(0xFFFFD98A).copy(alpha = lifeA), Offset(cx - cw * 0.30f, cy - ch * 0.68f), Size(cw * 0.20f, ch * 0.26f))
            drawRect(Color(0xFFFFD98A).copy(alpha = lifeA), Offset(cx + cw * 0.10f, cy - ch * 0.68f), Size(cw * 0.20f, ch * 0.26f))
            drawRect(Color(0xFF2A211C).copy(alpha = lifeA), Offset(cx - cw * 0.07f, cy - ch * 0.52f), Size(cw * 0.15f, ch * 0.52f))
            drawCircle(Color(0xFFFFD98A).copy(alpha = lifeA * 0.9f), cw * 0.012f, Offset(cx + cw * 0.05f, cy - ch * 0.24f))
            // chimney smoke
            drawRect(Color(0xFF403A3B), Offset(cx + cw * 0.22f, cy - ch - h * 0.070f), Size(cw * 0.10f, h * 0.048f))
            repeat(3) { i ->
                val u = (t * 0.35f + i * 0.33f) % 1f
                drawCircle(
                    Color.White.copy(alpha = 0.20f * lifeA * (1f - u)),
                    w * (0.012f + u * 0.022f),
                    Offset(cx + cw * 0.28f + sin((t + u) * TWO_PI) * w * 0.012f, cy - ch - h * 0.085f - u * h * 0.07f)
                )
            }
        }

        // ----- completion: warm snow glow and floating lanterns -----
        if (frame.golden > 0f) {
            softGlow(Color(0xFFFFD8A0), Offset(px, py), w * 0.85f, 0.20f * frame.golden)
            drawRect(Color(0xFFFFD9B0).copy(alpha = 0.10f * frame.golden))
        }
        if (frame.celebrate > 0f) repeat(9) { i ->
            val u = frame.celebrate
            val lx = w * (0.15f + i * 0.085f) + sin((lancerPhase(i) + u) * TWO_PI) * w * 0.02f
            val ly = h * 0.80f - u * h * (0.25f + (i % 3) * 0.10f)
            val a = (1f - u * 0.85f).coerceIn(0f, 1f)
            softGlow(Color(0xFFFFD37A), Offset(lx, ly), w * 0.035f, 0.35f * a)
            drawRect(Color(0xFFFFC258).copy(alpha = a), Offset(lx - w * 0.008f, ly - w * 0.012f), Size(w * 0.016f, w * 0.024f))
        }
    }
}

private fun lancerPhase(i: Int) = i * 0.13f

/* ---------- scene pieces ---------- */
private fun DrawScope.snowman(x: Float, y: Float, s: Float, w: Float) {
    drawOval(Color(0xFF3A3F45).copy(alpha = 0.10f), Offset(x - s * 1.1f, y - s * 0.18f), Size(s * 2.2f, s * 0.36f))
    drawCircle(Color(0xFFFBFDFF), s * 0.55f, Offset(x, y - s * 0.55f))
    drawCircle(Color(0xFFFBFDFF), s * 0.40f, Offset(x, y - s * 1.25f))
    drawCircle(Color(0xFFFBFDFF), s * 0.32f, Offset(x, y - s * 1.80f))
    drawCircle(Color(0xFF323A40), s * 0.04f, Offset(x - s * 0.11f, y - s * 1.88f))
    drawCircle(Color(0xFF323A40), s * 0.04f, Offset(x + s * 0.11f, y - s * 1.88f))
    drawLine(Color(0xFFFF9A3C), Offset(x, y - s * 1.80f), Offset(x + s * 0.22f, y - s * 1.74f), w * 0.005f, StrokeCap.Round)
    drawLine(Color(0xFFC9463D), Offset(x - s * 0.30f, y - s * 1.52f), Offset(x + s * 0.30f, y - s * 1.52f), w * 0.012f, StrokeCap.Round)
    drawLine(Color(0xFFC9463D), Offset(x + s * 0.26f, y - s * 1.52f), Offset(x + s * 0.34f, y - s * 1.30f), w * 0.010f, StrokeCap.Round)
}

private fun DrawScope.cat(x: Float, y: Float, s: Float, tail: Float, w: Float) {
    val fur = Color(0xFF6B6257)
    val light = Color(0xFF8C8275)
    drawOval(Color(0xFF3A3325).copy(alpha = 0.10f), Offset(x - s * 0.7f, y - s * 0.12f), Size(s * 1.5f, s * 0.24f))
    // seated body
    drawOval(fur, Offset(x - s * 0.45f, y - s * 0.92f), Size(s * 0.90f, s * 0.95f))
    drawOval(light, Offset(x - s * 0.10f, y - s * 0.80f), Size(s * 0.45f, s * 0.62f))
    // head
    drawCircle(fur, s * 0.30f, Offset(x - s * 0.05f, y - s * 1.10f))
    // ears
    winterPath.reset()
    winterPath.moveTo(x - s * 0.28f, y - s * 1.28f)
    winterPath.lineTo(x - s * 0.18f, y - s * 1.55f)
    winterPath.lineTo(x - s * 0.02f, y - s * 1.34f)
    winterPath.close()
    drawPath(winterPath, fur)
    winterPath.reset()
    winterPath.moveTo(x + s * 0.10f, y - s * 1.34f)
    winterPath.lineTo(x + s * 0.24f, y - s * 1.52f)
    winterPath.lineTo(x + s * 0.26f, y - s * 1.25f)
    winterPath.close()
    drawPath(winterPath, fur)
    // eyes and nose
    drawCircle(Color(0xFF2E2A26), s * 0.045f, Offset(x - s * 0.15f, y - s * 1.13f))
    drawCircle(Color(0xFF2E2A26), s * 0.045f, Offset(x + s * 0.09f, y - s * 1.13f))
    drawCircle(Color(0xFFC98C86), s * 0.035f, Offset(x - s * 0.03f, y - s * 1.02f))
    // tail curling beside the body
    drawLine(
        fur, Offset(x - s * 0.42f, y - s * 0.18f),
        Offset(x - s * (0.58f + 0.10f * tail), y - s * 0.58f), w * 0.016f, StrokeCap.Round
    )
    drawLine(
        fur, Offset(x - s * (0.58f + 0.10f * tail), y - s * 0.58f),
        Offset(x - s * (0.34f + 0.20f * tail), y - s * 0.72f), w * 0.014f, StrokeCap.Round
    )
}

private fun DrawScope.fox(x: Float, y: Float, s: Float, wag: Float, w: Float) {
    val fur = Color(0xFFE08A45)
    val cream = Color(0xFFF6E6D2)
    drawOval(fur, Offset(x - s * 0.75f, y - s * 0.75f), Size(s * 1.5f, s * 0.75f))
    drawCircle(fur, s * 0.34f, Offset(x + s * 0.55f, y - s * 0.95f))
    drawOval(cream, Offset(x + s * 0.42f, y - s * 0.88f), Size(s * 0.34f, s * 0.26f))
    drawCircle(Color(0xFF3A2E24), s * 0.055f, Offset(x + s * 0.68f, y - s * 1.02f))
    // ears
    drawOval(fur, Offset(x + s * 0.38f, y - s * 1.32f), Size(s * 0.20f, s * 0.32f))
    drawOval(fur, Offset(x + s * 0.58f, y - s * 1.32f), Size(s * 0.20f, s * 0.32f))
    // tail, gently wagging
    drawOval(
        fur, Offset(x - s * (1.25f + 0.06f * wag), y - s * 0.80f),
        Size(s * 0.75f, s * 0.30f)
    )
    drawOval(cream, Offset(x - s * 1.28f, y - s * 0.76f), Size(s * 0.22f, s * 0.20f))
    drawLine(Color(0xFF3A2E24), Offset(x + s * 0.10f, y - s * 0.05f), Offset(x + s * 0.10f, y), w * 0.006f)
    drawLine(Color(0xFF3A2E24), Offset(x + s * 0.34f, y - s * 0.05f), Offset(x + s * 0.34f, y), w * 0.006f)
}

private fun DrawScope.hare(x: Float, y: Float, s: Float) {
    val fur = Color(0xFFF3EDE4)
    drawOval(fur, Offset(x - s, y - s * 0.9f), Size(s * 2f, s * 0.9f))
    drawCircle(fur, s * 0.42f, Offset(x + s * 0.65f, y - s * 1.05f))
    drawOval(fur, Offset(x + s * 0.48f, y - s * 1.85f), Size(s * 0.20f, s * 0.70f))
    drawOval(fur, Offset(x + s * 0.72f, y - s * 1.80f), Size(s * 0.18f, s * 0.62f))
    drawCircle(Color(0xFF5A4A3C), s * 0.05f, Offset(x + s * 0.78f, y - s * 1.10f))
}

private fun DrawScope.robin(x: Float, y: Float, s: Float, w: Float) {
    drawOval(Color(0xFF6A5A4C), Offset(x - s, y - s * 0.6f), Size(s * 2f, s * 1.2f))
    drawCircle(Color(0xFF6A5A4C), s * 0.5f, Offset(x + s * 0.65f, y - s * 0.60f))
    drawOval(Color(0xFFD96A3C), Offset(x + s * 0.10f, y - s * 0.75f), Size(s * 0.85f, s * 0.85f))
    drawCircle(Color(0xFF2E2A26), s * 0.06f, Offset(x + s * 0.85f, y - s * 0.68f))
    drawLine(Color(0xFF2E2A26), Offset(x + s * 0.95f, y - s * 0.58f), Offset(x + s * 1.15f, y - s * 0.56f), w * 0.002f)
}
