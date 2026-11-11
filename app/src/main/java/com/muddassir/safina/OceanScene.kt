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

private class Bubble(val x: Float, val phase: Float, val size: Float)
private class CoralClump(val x: Float, val y: Float, val scale: Float, val kind: Int, val color: Color)

private val oceanRnd = Random(31)
private val bubbleList = List(32) {
    Bubble(oceanRnd.nextFloat(), oceanRnd.nextFloat(), 0.002f + oceanRnd.nextFloat() * 0.004f)
}
private val coraList = listOf(
    CoralClump(0.12f, 0.94f, 1.20f, 0, Color(0xFFF27E9D)),
    CoralClump(0.24f, 0.98f, 0.80f, 1, Color(0xFFF0A64F)),
    CoralClump(0.82f, 0.93f, 1.10f, 2, Color(0xFFB579E6)),
    CoralClump(0.92f, 0.98f, 0.85f, 1, Color(0xFFF27E9D)),
    CoralClump(0.68f, 0.99f, 0.65f, 0, Color(0xFF6DD1C4)),
    CoralClump(0.05f, 0.99f, 0.60f, 2, Color(0xFFF0A64F))
)
private val fishPhases = List(8) { oceanRnd.nextFloat() }
private val oceanPath = Path()

private const val OCEAN_PI = 3.1415927f

object OceanScene : Scene {
    override val id = "ocean"
    override val palette = ScenePalette(Color(0xFF6FD3D8), Color(0xFFBFEFF0))
    override val tapAnchor = Offset(0.5f, 0.88f)
    override val labels = listOf("Sand", "Tide", "Reef", "Deep")

    override fun textColor(progress: Float): Color =
        lerp(Color(0xFF2E4650), Color(0xFFF6F3E8), seg(progress, 0.10f, 0.28f))

    override fun DrawScope.draw(frame: SceneFrame) {
        val p = frame.progress
        val t = frame.time
        val w = size.width
        val h = size.height
        val tide = smooth(seg(p, 0.05f, 0.35f))
        val surfaceY = lerpF(h * 0.95f, h * 0.14f, tide)
        val deep = seg(p, 0.35f, 1f)

        // ----- warm sky above the waterline -----
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFFBFD6DC), Color(0xFFE9F0EA), Color(0xFFF4EAD6)),
                0f, h * 0.72f
            ),
            size = Size(w, h)
        )
        softGlow(Color(0xFFFFE8B8), Offset(w * 0.78f, h * 0.22f), w * 0.45f, 0.30f)

        // ----- dry seabed waiting for the tide -----
        oceanPath.reset()
        oceanPath.moveTo(0f, h * 0.72f)
        var dx = 0f
        while (dx <= w) {
            oceanPath.lineTo(dx, h * 0.72f + sin((dx / w * 1.8f + 0.2f) * TWO_PI) * h * 0.012f)
            dx += w / 16f
        }
        oceanPath.lineTo(w, h)
        oceanPath.lineTo(0f, h)
        oceanPath.close()
        drawPath(oceanPath, Color(0xFFCBB58C))
        drawPath(
            oceanPath,
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFFB49C73).copy(alpha = 0.65f)),
                h * 0.72f, h
            )
        )
        repeat(9) { i ->
            val x = w * (0.06f + i * 0.105f)
            val y = h * (0.80f + (i % 3) * 0.05f)
            drawOval(
                Color(0xFF9C8763).copy(alpha = 0.45f),
                Offset(x - w * 0.028f, y - h * 0.006f), Size(w * 0.056f, h * 0.012f)
            )
        }
        repeat(5) { i ->
            val x = w * (0.16f + i * 0.17f)
            val y = h * (0.95f - (i % 2) * 0.02f)
            drawOval(
                Color(0xFFF5E9D2).copy(alpha = 0.9f),
                Offset(x - w * 0.012f, y - w * 0.008f), Size(w * 0.024f, w * 0.016f)
            )
        }

        // ----- the tide filling the scene -----
        if (tide > 0f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        lerp(Color(0xFF8FE0E4), Color(0xFF5FC4D6), deep).copy(alpha = 0.88f),
                        lerp(Color(0xFF3D9CBE), Color(0xFF1F7398), deep).copy(alpha = 0.93f),
                        lerp(Color(0xFF155B82), Color(0xFF0A3B60), deep).copy(alpha = 0.97f)
                    ),
                    surfaceY, h
                ),
                topLeft = Offset(0f, surfaceY), size = Size(w, h - surfaceY)
            )
            oceanPath.reset()
            oceanPath.moveTo(0f, surfaceY)
            var x = 0f
            while (x <= w) {
                oceanPath.lineTo(
                    x, surfaceY + sin((x / w * 3f + t * 2f) * TWO_PI) * h * 0.008f
                )
                x += w / 24f
            }
            drawPath(
                oceanPath, Color(0xFFE8FBFC).copy(alpha = 0.45f),
                style = Stroke(w * 0.012f, cap = StrokeCap.Round)
            )
            drawPath(
                oceanPath, Color(0xFFF7FFFF).copy(alpha = 0.55f),
                style = Stroke(w * 0.003f, cap = StrokeCap.Round)
            )
            repeat(5) { i ->
                val swayX = sin((t * 0.8f + i * 0.25f) * TWO_PI) * w * 0.03f
                val topX = w * (0.14f + i * 0.19f) + swayX
                val beam = Color(0xFFDFF9F6).copy(
                    alpha = 0.085f * tide * (0.6f + 0.4f * sin((t + i * 0.2f) * TWO_PI))
                )
                oceanPath.reset()
                oceanPath.moveTo(topX - w * 0.028f, surfaceY)
                oceanPath.lineTo(topX + w * 0.028f, surfaceY)
                oceanPath.lineTo(topX + w * 0.15f, h)
                oceanPath.lineTo(topX - w * 0.02f, h)
                oceanPath.close()
                drawPath(oceanPath, beam)
            }
        }

        // ----- kelp on both sides -----
        val reefA = seg(p, 0.28f, 0.62f)
        if (reefA > 0f) {
            listOf(0.04f, 0.08f, 0.94f, 0.98f).forEachIndexed { i, fx ->
                val baseX = w * fx
                val height = h * (0.28f + (i % 3) * 0.06f) * reefA
                oceanPath.reset()
                oceanPath.moveTo(baseX, h * 0.98f)
                var step = 0f
                while (step <= 1f) {
                    oceanPath.lineTo(
                        baseX + sin((step * 1.5f + t * 0.8f + i * 0.3f) * TWO_PI) * w * 0.024f * step,
                        h * 0.98f - step * height
                    )
                    step += 0.12f
                }
                drawPath(
                    oceanPath, Color(0xFF2F7A57).copy(alpha = 0.85f * reefA),
                    style = Stroke(w * 0.013f, cap = StrokeCap.Round)
                )
                repeat(5) { leaf ->
                    val u = 0.25f + leaf * 0.15f
                    val lx = baseX + sin((u * 1.5f + t * 0.8f + i * 0.3f) * TWO_PI) * w * 0.024f * u
                    val ly = h * 0.98f - u * height
                    drawOval(
                        Color(0xFF3C8F63).copy(alpha = 0.75f * reefA),
                        Offset(lx, ly), Size(w * 0.048f, w * 0.013f)
                    )
                }
            }
        }

        // ----- coral garden, growing in -----
        if (reefA > 0f) coraList.forEachIndexed { i, c ->
            val grow = smooth(seg(p, 0.26f + i * 0.03f, 0.46f + i * 0.03f))
            if (grow > 0f) {
                coralClump(
                    c.x * w, c.y * h, w * 0.055f * c.scale * grow,
                    c.kind, c.color, sin((t * 0.7f + i * 0.27f) * TWO_PI), w
                )
            }
        }
        if (reefA > 0f) {
            starfish(w * 0.58f, h * 0.94f, w * 0.020f * reefA)
            starfish(w * 0.38f, h * 0.97f, w * 0.016f * reefA)
        }

        // ----- fish crossing the whole frame, corner to corner -----
        val fishA = seg(p, 0.42f, 0.70f)
        if (fishA > 0f) {
            fishPhases.forEachIndexed { i, ph ->
                // `elapsed` never wraps with the 8s ambience loop, so a fish truly
                // enters at the left edge and exits at the right before returning.
                val travel = (frame.elapsed * 0.085f + ph * 0.16f) % 1.30f
                val fx = w * (-0.15f + travel)
                val fy = h * (0.44f + 0.10f * i / fishPhases.size) +
                    sin((t * 0.8f + i * 0.35f) * TWO_PI) * h * 0.014f
                val wig = sin((t * 2.2f + i * 0.7f) * TWO_PI)
                fish(fx, fy, w * (0.019f + (i % 3) * 0.0035f) * fishA, wig, w)
            }
            clownfish(
                w * 0.14f + sin(t * TWO_PI * 0.7f) * w * 0.02f,
                h * 0.87f + cos(t * TWO_PI * 0.5f) * h * 0.008f,
                w * 0.019f * fishA, w, sin((t * 2f) * TWO_PI)
            )
            clownfish(
                w * 0.84f + cos(t * TWO_PI * 0.6f) * w * 0.02f,
                h * 0.85f + sin(t * TWO_PI * 0.4f) * h * 0.008f,
                w * 0.016f * fishA, w, sin((t * 2f + 1f) * TWO_PI)
            )
            turtle(
                w * (-0.14f + (frame.elapsed * 0.024f) % 1.28f),
                h * 0.62f + sin(t * TWO_PI * 0.35f) * h * 0.02f,
                w * 0.078f * fishA, w, sin((t * 1.4f) * TWO_PI)
            )
        }

        // ----- jellyfish drifting -----
        if (fishA > 0f) repeat(4) { i ->
            val jx = w * (0.24f + i * 0.18f) + sin((t * 0.4f + i * 0.35f) * TWO_PI) * w * 0.03f
            val jy = h * (0.66f - i * 0.10f) - sin((t * 0.3f + i * 0.5f) * TWO_PI) * h * 0.015f
            jellyfish(jx, jy, w * (0.026f + (i % 2) * 0.006f) * fishA, t + i * 0.3f, w)
        }

        // ----- a distant manta, very calm -----
        val deepA = seg(p, 0.75f, 1f)
        if (deepA > 0f) {
            manta(
                w * (0.05f + (frame.elapsed * 0.012f) % 0.88f),
                h * 0.34f + sin(t * TWO_PI * 0.3f) * h * 0.015f,
                w * 0.13f, deepA * 0.5f, sin(t * TWO_PI * 0.6f)
            )
        }

        // ----- rising bubbles -----
        if (tide > 0f) bubbleList.forEach { b ->
            val u = (t * (0.30f + b.size * 32f) + b.phase) % 1f
            val by = lerpF(h * 0.99f, surfaceY, u)
            val bx = b.x * w + sin((t * 2f + b.phase * 5f) * TWO_PI) * w * 0.012f
            drawCircle(
                Color(0xFFE8F9FA).copy(alpha = 0.30f * tide * (0.35f + u * 0.65f)),
                w * b.size, Offset(bx, by), style = Stroke(w * 0.0015f)
            )
        }

        // ----- tap: a bubble rises and the surface blooms -----
        if (frame.pulse < 1f) {
            val u = frame.pulse
            val cx = w * 0.5f
            val cy = h * 0.72f
            val r = 0.2f + 0.8f * u
            drawOval(
                Color(0xFFE6FBFF).copy(alpha = 0.55f * (1f - u)),
                Offset(cx - w * 0.11f * r, cy - w * 0.038f * r),
                Size(w * 0.22f * r, w * 0.076f * r),
                style = Stroke(w * 0.003f * frame.ripple)
            )
            drawOval(
                Color(0xFFE6FBFF).copy(alpha = 0.30f * (1f - u)),
                Offset(cx - w * 0.16f * r, cy - w * 0.055f * r),
                Size(w * 0.32f * r, w * 0.11f * r),
                style = Stroke(w * 0.002f * frame.ripple)
            )
            drawCircle(
                Color.White.copy(alpha = 0.65f * (1f - u)), w * 0.008f,
                Offset(cx, cy - h * 0.06f * u), style = Stroke(w * 0.002f)
            )
        }

        // ----- completion: warm light and golden bubbles -----
        if (frame.golden > 0f) {
            softGlow(Color(0xFFFFE7B0), Offset(w * 0.5f, surfaceY), w * 0.95f, 0.20f * frame.golden)
            drawRect(Color(0xFFFFDFA0).copy(alpha = 0.07f * frame.golden))
        }
        if (frame.celebrate > 0f) repeat(10) { i ->
            val a = i * 0.63f
            val u = (frame.celebrate + i * 0.08f) % 1f
            drawCircle(
                Color(0xFFFFE9A8).copy(alpha = 0.55f * (1f - u)),
                w * (0.003f + (i % 3) * 0.002f),
                Offset(
                    w * 0.5f + cos(a + u * 3f) * w * 0.26f,
                    h * 0.72f - u * h * 0.42f
                ),
                style = Stroke(w * 0.0016f)
            )
        }
    }
}

/* ---------- pieces ---------- */

private fun DrawScope.coralClump(
    x: Float,
    y: Float,
    s: Float,
    kind: Int,
    color: Color,
    sway: Float,
    w: Float
) {
    when (kind) {
        0 -> {
            // branching staghorn coral
            repeat(5) { i ->
                val a = -OCEAN_PI / 2f + (i - 2) * 0.34f + sway * 0.05f
                val len = s * (1.6f + (i % 2) * 0.5f)
                val midX = x + cos(a) * len * 0.55f
                val midY = y + sin(a) * len * 0.55f
                drawLine(color, Offset(x, y), Offset(midX, midY), w * 0.010f, StrokeCap.Round)
                drawLine(
                    color.copy(alpha = 0.9f), Offset(midX, midY),
                    Offset(midX + cos(a + 0.4f) * len * 0.35f, midY + sin(a + 0.4f) * len * 0.35f),
                    w * 0.007f, StrokeCap.Round
                )
            }
            drawOval(Color(0xFFF7B3C6).copy(alpha = 0.8f), Offset(x - s * 0.5f, y - s * 0.12f), Size(s, s * 0.28f))
        }
        1 -> {
            // brain coral mounds
            drawCircle(color, s * 0.95f, Offset(x, y - s * 0.30f))
            drawCircle(color.copy(alpha = 0.85f), s * 0.70f, Offset(x - s * 0.55f, y - s * 0.18f))
            drawCircle(color.copy(alpha = 0.85f), s * 0.62f, Offset(x + s * 0.60f, y - s * 0.20f))
            repeat(3) { i ->
                drawCircle(
                    Color.White.copy(alpha = 0.12f), s * (0.42f - i * 0.10f),
                    Offset(x - s * 0.10f + i * s * 0.16f, y - s * 0.42f),
                    style = Stroke(w * 0.0018f)
                )
            }
        }
        else -> {
            // sea fan
            repeat(3) { i ->
                val baseX = x + (i - 1) * s * 0.35f
                oceanPath.reset()
                oceanPath.moveTo(baseX, y)
                var step = 0f
                while (step <= 1f) {
                    oceanPath.lineTo(
                        baseX + sin((step * 2f + i * 0.4f) * TWO_PI) * s * 0.32f + sway * s * 0.15f * step,
                        y - step * s * 1.7f
                    )
                    step += 0.2f
                }
                drawPath(
                    oceanPath, color.copy(alpha = 0.8f),
                    style = Stroke(w * 0.007f, cap = StrokeCap.Round)
                )
            }
            drawOval(color.copy(alpha = 0.45f), Offset(x - s * 0.7f, y - s * 0.30f), Size(s * 1.4f, s * 0.42f))
        }
    }
}

private fun DrawScope.starfish(x: Float, y: Float, s: Float) {
    repeat(5) { k ->
        val a = k * (TWO_PI / 5f) - OCEAN_PI / 2f
        drawLine(
            Color(0xFFF09A6A), Offset(x, y),
            Offset(x + cos(a) * s, y + sin(a) * s * 0.62f),
            s * 0.34f, StrokeCap.Round
        )
    }
    drawCircle(Color(0xFFFFC49A), s * 0.26f, Offset(x, y))
}

/** A little reef fish with a tail that sweeps with the swim cycle. */
private fun DrawScope.fish(x: Float, y: Float, s: Float, wiggle: Float, w: Float) {
    val body = Color(0xFFF6DE9E)
    drawOval(body, Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    rotate(degrees = wiggle * 10f, pivot = Offset(x - s * 0.8f, y)) {
        drawOval(
            Color(0xFFE9C873), Offset(x - s * 1.85f, y - s * 0.42f),
            Size(s * 0.85f, s * 0.84f)
        )
    }
    drawOval(body.copy(alpha = 0.9f), Offset(x - s * 0.2f, y - s * 0.85f), Size(s * 0.9f, s * 0.45f))
    drawCircle(Color(0xFF2E2A26), s * 0.10f, Offset(x + s * 0.58f, y - s * 0.10f))
    drawLine(Color(0xFF3B3B33).copy(alpha = 0.6f), Offset(x - s * 0.7f, y + s * 0.45f), Offset(x + s * 0.6f, y + s * 0.30f), w * 0.0014f)
}

private fun DrawScope.clownfish(x: Float, y: Float, s: Float, w: Float, wiggle: Float) {
    val orange = Color(0xFFF07A2E)
    drawOval(orange, Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    rotate(degrees = wiggle * 12f, pivot = Offset(x - s * 0.85f, y)) {
        drawOval(orange.copy(alpha = 0.95f), Offset(x - s * 1.9f, y - s * 0.45f), Size(s * 0.8f, s * 0.9f))
    }
    drawLine(Color.White, Offset(x - s * 0.25f, y - s * 0.52f), Offset(x - s * 0.25f, y + s * 0.52f), s * 0.20f)
    drawLine(Color.White, Offset(x + s * 0.45f, y - s * 0.42f), Offset(x + s * 0.45f, y + s * 0.42f), s * 0.16f)
    drawCircle(Color(0xFF2E2A26), s * 0.09f, Offset(x + s * 0.62f, y - s * 0.12f))
    drawLine(Color(0xFF2E2A26).copy(alpha = 0.5f), Offset(x - s * 0.9f, y + s * 0.55f), Offset(x + s * 0.7f, y + s * 0.55f), w * 0.0014f)
}

private fun DrawScope.turtle(x: Float, y: Float, s: Float, w: Float, paddle: Float) {
    val shell = Color(0xFF4E7F63)
    val skin = Color(0xFF6FA47C)
    drawOval(Color(0xFF356050).copy(alpha = 0.35f), Offset(x - s * 0.4f, y - s * 0.15f), Size(s * 1.8f, s * 0.5f))
    // flippers
    rotate(degrees = paddle * 18f, pivot = Offset(x, y)) {
        drawOval(skin, Offset(x - s * 0.95f, y - s * 0.90f), Size(s * 0.75f, s * 0.30f))
        drawOval(skin, Offset(x + s * 0.20f, y - s * 0.90f), Size(s * 0.75f, s * 0.30f))
    }
    // shell
    drawOval(shell, Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.10f))
    drawOval(Color(0xFF6E9A78).copy(alpha = 0.60f), Offset(x - s * 0.72f, y - s * 0.30f), Size(s * 1.35f, s * 0.60f))
    // shell pattern
    repeat(3) { i ->
        drawCircle(
            Color(0xFF3E6B54).copy(alpha = 0.55f), s * 0.18f,
            Offset(x - s * 0.35f + i * s * 0.34f, y - s * 0.24f)
        )
    }
    // head + eye
    drawCircle(skin, s * 0.34f, Offset(x + s * 1.08f, y - s * 0.18f))
    drawCircle(Color(0xFF2E2A26), s * 0.055f, Offset(x + s * 1.22f, y - s * 0.24f))
    drawLine(Color(0xFF6FA47C).copy(alpha = 0.7f), Offset(x - s, y + s * 0.38f), Offset(x + s * 0.9f, y + s * 0.38f), w * 0.0014f)
}

private fun DrawScope.jellyfish(x: Float, y: Float, s: Float, time: Float, w: Float) {
    val pulse = 0.86f + 0.14f * sin(time * TWO_PI)
    softGlow(Color(0xFFC9A6FF), Offset(x, y), s * 2.4f, 0.18f)
    drawOval(
        Color(0xFFDCC6FF).copy(alpha = 0.62f),
        Offset(x - s * pulse, y - s * 0.55f), Size(s * 2f * pulse, s * 1.05f)
    )
    drawOval(
        Color(0xFFF0E6FF).copy(alpha = 0.30f),
        Offset(x - s * pulse * 0.7f, y - s * 0.45f), Size(s * 1.3f * pulse, s * 0.55f)
    )
    repeat(5) { i ->
        val off = (i - 2) * s * 0.34f
        drawLine(
            Color(0xFFEADFFF).copy(alpha = 0.40f),
            Offset(x + off, y + s * 0.35f),
            Offset(
                x + off + sin((time + i * 0.12f) * TWO_PI) * s * 0.28f,
                y + s * (1.35f + (i % 2) * 0.25f)
            ),
            w * 0.0016f, StrokeCap.Round
        )
    }
}

private fun DrawScope.manta(x: Float, y: Float, s: Float, alpha: Float, flap: Float) {
    val col = Color(0xFF123F58).copy(alpha = 0.55f * alpha)
    oceanPath.reset()
    oceanPath.moveTo(x - s, y - s * 0.10f * flap)
    oceanPath.quadraticTo(x - s * 0.35f, y - s * 0.55f, x, y)
    oceanPath.quadraticTo(x + s * 0.35f, y - s * 0.55f, x + s, y - s * 0.10f * flap)
    oceanPath.quadraticTo(x + s * 0.30f, y + s * 0.16f, x, y + s * 0.04f)
    oceanPath.quadraticTo(x - s * 0.30f, y + s * 0.16f, x - s, y - s * 0.10f * flap)
    oceanPath.close()
    drawPath(oceanPath, col)
    drawLine(
        col.copy(alpha = 0.7f * alpha), Offset(x, y + s * 0.02f),
        Offset(x, y + s * 0.45f), s * 0.025f, StrokeCap.Round
    )
}
