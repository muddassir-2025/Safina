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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val oceanRnd = Random(31)
private class Coral(val x: Float, val y: Float, val s: Float, val kind: Int, val order: Float, val color: Color)
private class Bubble(val x: Float, val y: Float, val phase: Float, val size: Float)

private val coralColors = listOf(
    Color(0xFFE9779B), Color(0xFFF2A65A), Color(0xFFB57BE0), Color(0xFFFF8E8E), Color(0xFF7BD1C8)
)
private val corals = List(16) { i ->
    Coral(
        0.05f + oceanRnd.nextFloat() * 0.90f, 0.78f + oceanRnd.nextFloat() * 0.20f,
        0.6f + oceanRnd.nextFloat() * 0.9f, i % 4, oceanRnd.nextFloat(),
        coralColors[oceanRnd.nextInt(coralColors.size)]
    )
}
private val bubbles = List(34) {
    Bubble(oceanRnd.nextFloat(), 0.55f + oceanRnd.nextFloat() * 0.42f, oceanRnd.nextFloat(), 0.002f + oceanRnd.nextFloat() * 0.004f)
}
private val school = List(9) {
    oceanRnd.nextFloat()
}
private val oceanPath = Path()

object OceanScene : Scene {
    override val id = "ocean"
    override val palette = ScenePalette(Color(0xFF6FD3D8), Color(0xFFBFEFF0))
    override val tapAnchor = Offset(0.5f, 0.90f)
    override val labels = listOf("Sand", "Tide", "Reef", "Deep")

    override fun textColor(progress: Float): Color =
        lerp(Color(0xFF2E4650), Color(0xFFF2F7F5), seg(progress, 0.12f, 0.32f))

    override fun DrawScope.draw(frame: SceneFrame) {
        val p = frame.progress
        val t = frame.time
        val w = size.width
        val h = size.height
        val tide = smooth(seg(p, 0.05f, 0.35f))
        val surfaceY = lerpF(h * 0.94f, h * 0.16f, tide)
        val deep = seg(p, 0.35f, 1f)

        // ----- sky above the waterline -----
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFFC9D8DC), Color(0xFFE7EFEC)),
                0f, h * 0.70f
            ),
            size = Size(w, h)
        )

        // ----- dry seabed -----
        drawRect(Color(0xFFC8B48C), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFC8B48C), Color(0xFFB09A72)), h * 0.72f, h),
            topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f)
        )
        repeat(7) { i ->
            val x = w * (0.08f + i * 0.14f)
            val y = h * (0.80f + (i % 3) * 0.055f)
            drawOval(
                Color(0xFF9C8763).copy(alpha = 0.55f),
                Offset(x - w * 0.05f, y - h * 0.008f), Size(w * 0.10f, h * 0.016f)
            )
        }
        // shells and a dead coral skeleton
        repeat(5) { i ->
            val x = w * (0.14f + i * 0.18f)
            val y = h * (0.95f - (i % 2) * 0.02f)
            drawOval(Color(0xFFF1E3CE), Offset(x - w * 0.012f, y - w * 0.008f), Size(w * 0.024f, w * 0.016f))
        }
        val dead = Color(0xFFB3A48D)
        repeat(4) { i ->
            drawLine(
                dead, Offset(w * 0.33f, h * 0.90f),
                Offset(w * (0.29f + i * 0.025f), h * (0.78f + i * 0.012f)),
                w * 0.006f, StrokeCap.Round
            )
        }

        // ----- water rising -----
        if (tide > 0f) {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF6FD3D8).copy(alpha = 0.80f),
                        Color(0xFF2A87A8).copy(alpha = 0.88f),
                        Color(0xFF124A70).copy(alpha = 0.94f)
                    ),
                    surfaceY, h
                ),
                topLeft = Offset(0f, surfaceY), size = Size(w, h - surfaceY)
            )
            // wavy surface line
            oceanPath.reset()
            oceanPath.moveTo(0f, surfaceY)
            var x = 0f
            while (x <= w) {
                oceanPath.lineTo(x, surfaceY + sin((x / w * 3f + t * 2f) * TWO_PI) * h * 0.008f)
                x += w / 24f
            }
            drawPath(oceanPath, Color(0xFFEAFAFB).copy(alpha = 0.55f), style = Stroke(w * 0.004f, cap = StrokeCap.Round))
            drawPath(
                oceanPath, Color(0xFF8FE4E8).copy(alpha = 0.5f),
                style = Stroke(w * 0.018f, cap = StrokeCap.Round)
            )

            // god rays
            repeat(4) { i ->
                val swayX = sin((t * 0.8f + i * 0.25f) * TWO_PI) * w * 0.03f
                val topX = w * (0.18f + i * 0.22f) + swayX
                val beam = Color(0xFFD9F7F4).copy(alpha = 0.10f * tide * (0.6f + 0.4f * sin((t + i * 0.2f) * TWO_PI)))
                oceanPath.reset()
                oceanPath.moveTo(topX - w * 0.03f, surfaceY)
                oceanPath.lineTo(topX + w * 0.03f, surfaceY)
                oceanPath.lineTo(topX + w * 0.16f, h)
                oceanPath.lineTo(topX - w * 0.02f, h)
                oceanPath.close()
                drawPath(oceanPath, beam)
            }
        }

        // ----- coral garden -----
        val reefA = seg(p, 0.30f, 0.60f)
        if (reefA > 0f) corals.forEachIndexed { i, c ->
            val grow = smooth(seg(p, c.order * 0.55f + 0.30f, c.order * 0.55f + 0.42f))
            if (grow > 0f) {
                coral(
                    c.x * w, c.y * h, w * 0.028f * c.s * reefA * grow,
                    c.kind, c.color.copy(alpha = 0.95f), w, sin((t * 0.8f + i * 0.2f) * TWO_PI)
                )
            }
        }
        // starfish on the sand
        if (reefA > 0f) {
            starfish(w * 0.72f, h * 0.94f, w * 0.016f * reefA)
            starfish(w * 0.18f, h * 0.97f, w * 0.013f * reefA)
        }

        // ----- fish -----
        val fishA = seg(p, 0.45f, 0.80f)
        if (fishA > 0f) {
            school.forEachIndexed { i, ph ->
                val cx = w * (0.20f + ph * 0.55f)
                val cy = h * (0.55f + 0.18f * sin((t * 0.7f + ph * 4f) * TWO_PI))
                val wobble = sin((t * 1.1f + i * 0.3f) * TWO_PI)
                val fx = cx + wobble * w * 0.06f
                val fy = cy + cos((t * 0.9f + i * 0.4f) * TWO_PI) * h * 0.03f
                schoolFish(fx, fy, w * 0.008f, Color(0xFFF2E7C2), w, if (wobble > 0f) 1f else -1f)
            }
            clownfish(w * 0.30f + sin(t * TWO_PI * 0.6f) * w * 0.03f, h * 0.86f, w * 0.011f * fishA, w)
            clownfish(w * 0.66f + cos(t * TWO_PI * 0.5f) * w * 0.03f, h * 0.90f, w * 0.010f * fishA, w)
            turtle(w * (0.15f + 0.7f * (t % 1f)), h * 0.62f + sin(t * TWO_PI) * h * 0.01f, w * 0.045f * fishA, w)
        }

        // ----- whale and jellyfish -----
        val deepA = seg(p, 0.75f, 1f)
        if (deepA > 0f) {
            whale(w * (0.55f + 0.35f * sin(t * TWO_PI * 0.5f)), h * 0.42f, w * 0.13f * deepA, deepA)
            repeat(4) { i ->
                val jx = w * (0.18f + i * 0.20f) + sin((t * 0.6f + i * 0.3f) * TWO_PI) * w * 0.02f
                val jy = h * (0.72f - i * 0.10f) - sin((t * 0.4f + i * 0.2f) * TWO_PI) * h * 0.02f
                jellyfish(jx, jy, w * 0.020f * deepA, t + i * 0.25f, w)
            }
        }

        // ----- bubbles -----
        if (tide > 0f) bubbles.forEach { b ->
            val u = (t * (0.35f + b.size * 30f) + b.phase) % 1f
            val by = lerpF(h, surfaceY, u)
            val bx = b.x * w + sin((t * 2f + b.phase * 4f) * TWO_PI) * w * 0.012f
            drawCircle(
                Color(0xDFEFFBFB).copy(alpha = 0.35f * tide * (0.4f + u * 0.6f)),
                w * b.size, Offset(bx, by), style = Stroke(w * 0.0018f)
            )
        }

        // ----- tap: a bubble rises and pops -----
        if (frame.pulse < 1f) {
            val u = frame.pulse
            val bx = w * 0.5f
            val by = lerpF(h * 0.88f, h * 0.60f, u)
            drawCircle(Color.White.copy(alpha = 0.55f * (1f - u)), w * 0.012f, Offset(bx, by), style = Stroke(w * 0.003f))
            val r = 0.25f + 0.75f * u
            drawOval(
                Color(0xFFDFF8FF).copy(alpha = 0.55f * (1f - u)),
                Offset(w * 0.5f - w * 0.09f * r, h * 0.62f - w * 0.03f * r),
                Size(w * 0.18f * r, w * 0.06f * r),
                style = Stroke(w * 0.003f * frame.ripple)
            )
        }

        // ----- completion: brighter water, one slow circle of fish, golden bubbles -----
        if (frame.golden > 0f) {
            drawRect(Color(0xFFBFF3F0).copy(alpha = 0.10f * frame.golden))
            softGlow(Color(0xFFFFF0C0), Offset(w * 0.5f, h * 0.30f), w * 0.8f, 0.16f * frame.golden)
        }
        if (frame.celebrate > 0f) repeat(10) { i ->
            val a = i * 0.63f
            val u = (frame.celebrate + i * 0.08f) % 1f
            drawCircle(
                Color(0xFFFFE39A).copy(alpha = 0.5f * (1f - u)),
                w * (0.004f + 0.006f * i % 3),
                Offset(w * 0.5f + cos(a + u * 3f) * w * 0.22f, h * 0.75f - u * h * 0.45f),
                style = Stroke(w * 0.002f)
            )
        }
    }
}

/* ---------- pieces ---------- */
private fun DrawScope.coral(x: Float, y: Float, s: Float, kind: Int, color: Color, w: Float, swayAmt: Float) {
    when (kind) {
        0 -> {
            drawLine(color, Offset(x, y), Offset(x - s * 0.15f, y - s * 1.4f), w * 0.006f, StrokeCap.Round)
            drawLine(color, Offset(x, y - s * 0.7f), Offset(x + s * 0.8f, y - s * 1.5f), w * 0.005f, StrokeCap.Round)
            drawLine(color, Offset(x, y - s * 0.9f), Offset(x - s * 0.9f, y - s * 1.7f), w * 0.005f, StrokeCap.Round)
            drawLine(color, Offset(x, y - s * 1.2f), Offset(x + s * 0.2f, y - s * 2.0f), w * 0.005f, StrokeCap.Round)
        }
        1 -> {
            drawCircle(color, s * 0.85f, Offset(x, y - s * 0.45f))
            drawCircle(color.copy(alpha = 0.8f), s * 0.55f, Offset(x - s * 0.7f, y - s * 0.3f))
            drawCircle(color.copy(alpha = 0.8f), s * 0.5f, Offset(x + s * 0.7f, y - s * 0.35f))
            drawCircle(Color.White.copy(alpha = 0.16f), s * 0.3f, Offset(x - s * 0.2f, y - s * 0.6f))
        }
        2 -> {
            repeat(3) { i ->
                val baseX = x + (i - 1) * s * 0.45f
                oceanPath.reset()
                oceanPath.moveTo(baseX, y)
                var step = 0f
                while (step <= 1f) {
                    val px = baseX + sin((step * 2f + i * 0.4f) * TWO_PI) * s * 0.35f + swayAmt * s * 0.2f * step
                    oceanPath.lineTo(px, y - step * s * (1.6f + i * 0.15f))
                    step += 0.2f
                }
                drawPath(oceanPath, color.copy(alpha = 0.75f), style = Stroke(w * 0.007f, cap = StrokeCap.Round))
            }
        }
        else -> {
            drawOval(color, Offset(x - s * 0.7f, y - s * 0.35f), Size(s * 1.4f, s * 0.45f))
            repeat(7) { k ->
                val a = -PI_F / 2f + (k - 3) * 0.22f + swayAmt * 0.06f
                drawLine(
                    color.copy(alpha = 0.85f),
                    Offset(x, y - s * 0.35f),
                    Offset(x + cos(a) * s * 1.25f, y - s * 0.35f + sin(a) * s * 1.25f),
                    w * 0.004f, StrokeCap.Round
                )
            }
        }
    }
}

private const val PI_F = 3.1415927f

private fun DrawScope.starfish(x: Float, y: Float, s: Float) {
    repeat(5) { k ->
        val a = k * (TWO_PI / 5f) - PI_F / 2f
        drawLine(
            Color(0xFFF09A6A), Offset(x, y),
            Offset(x + cos(a) * s, y + sin(a) * s * 0.6f),
            s * 0.34f, StrokeCap.Round
        )
    }
    drawCircle(Color(0xFFFFC49A), s * 0.28f, Offset(x, y))
}

private fun DrawScope.schoolFish(x: Float, y: Float, s: Float, color: Color, w: Float, dir: Float) {
    drawOval(color, Offset(x - s, y - s * 0.5f), Size(s * 1.8f, s))
    drawOval(color.copy(alpha = 0.9f), Offset(x - s * 1.7f, y - s * 0.4f), Size(s * 0.7f, s * 0.8f))
    drawCircle(Color(0xFF3B3B33), s * 0.09f, Offset(x + dir * s * 0.5f, y - s * 0.08f))
    drawLine(color.copy(alpha = 0.5f), Offset(x - s, y + s * 0.45f), Offset(x + s, y + s * 0.45f), w * 0.0015f)
}

private fun DrawScope.clownfish(x: Float, y: Float, s: Float, w: Float) {
    val orange = Color(0xFFF07A2E)
    drawOval(orange, Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    drawOval(orange.copy(alpha = 0.95f), Offset(x - s * 1.9f, y - s * 0.45f), Size(s * 0.8f, s * 0.9f))
    repeat(2) { i ->
        drawLine(Color.White, Offset(x - s * 0.2f + i * s * 0.9f, y - s * 0.5f), Offset(x - s * 0.2f + i * s * 0.9f, y + s * 0.5f), s * 0.22f)
    }
    drawCircle(Color(0xFF2E2A26), s * 0.09f, Offset(x + s * 0.55f, y - s * 0.1f))
    drawLine(Color(0xFF2E2A26), Offset(x - s * 0.9f, y + s * 0.55f), Offset(x + s * 0.7f, y + s * 0.55f), w * 0.0015f)
}

private fun DrawScope.turtle(x: Float, y: Float, s: Float, w: Float) {
    drawOval(Color(0xFF5E8C6A), Offset(x - s, y - s * 0.55f), Size(s * 2f, s * 1.1f))
    drawOval(Color(0xFF6FA47C).copy(alpha = 0.7f), Offset(x - s * 0.7f, y - s * 0.35f), Size(s * 1.2f, s * 0.7f))
    drawCircle(Color(0xFF6FA47C), s * 0.32f, Offset(x + s * 1.05f, y - s * 0.1f))
    repeat(4) { i ->
        drawLine(
            Color(0xFF5E8C6A), Offset(x + (i - 1.5f) * s * 0.4f, y - s * 0.4f),
            Offset(x + (i - 1.5f) * s * 0.55f, y - s * 0.85f), s * 0.16f, StrokeCap.Round
        )
    }
    drawCircle(Color(0xFF2E2A26), s * 0.06f, Offset(x + s * 1.18f, y - s * 0.14f))
    drawLine(Color(0xFF6FA47C).copy(alpha = 0.6f), Offset(x - s, y + s * 0.4f), Offset(x + s, y + s * 0.4f), w * 0.0015f)
}

private fun DrawScope.whale(x: Float, y: Float, s: Float, alpha: Float) {
    val col = Color(0xFF2B5F86).copy(alpha = 0.55f * alpha)
    drawOval(col, Offset(x - s, y - s * 0.35f), Size(s * 2f, s * 0.7f))
    drawOval(col.copy(alpha = 0.45f), Offset(x - s * 1.9f, y - s * 0.22f), Size(s * 0.8f, s * 0.5f))
    drawLine(col.copy(alpha = 0.45f), Offset(x - s * 0.2f, y - s * 0.25f), Offset(x + s * 0.15f, y - s * 0.65f), s * 0.08f, StrokeCap.Round)
}

private fun DrawScope.jellyfish(x: Float, y: Float, s: Float, time: Float, w: Float) {
    val pulse = 0.85f + 0.15f * sin(time * TWO_PI)
    softGlow(Color(0xFFC9A6FF), Offset(x, y), s * 2.2f, 0.20f)
    drawOval(
        Color(0xFFD8BFFF).copy(alpha = 0.75f),
        Offset(x - s * pulse, y - s * 0.6f), Size(s * 2f * pulse, s * 1.1f)
    )
    repeat(5) { i ->
        val off = (i - 2) * s * 0.35f
        drawLine(
            Color(0xFFE2D0FF).copy(alpha = 0.5f),
            Offset(x + off, y + s * 0.3f),
            Offset(x + off + sin((time + i * 0.1f) * TWO_PI) * s * 0.3f, y + s * 1.4f),
            w * 0.0018f, StrokeCap.Round
        )
    }
}
