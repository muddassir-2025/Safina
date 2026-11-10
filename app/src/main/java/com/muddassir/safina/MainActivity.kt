package com.muddassir.safina

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.onDispose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GardenApp() }
    }
}

private val Ink = Color(0xFF5A4326)
private const val TARGET = 100

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GardenApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tally", Context.MODE_PRIVATE) }
    var count by remember { mutableIntStateOf(prefs.getInt("count", 0)) }

    // Scene progress eases toward count/100 whenever count changes
    val progress by animateFloatAsState(
        targetValue = count.coerceAtMost(TARGET).toFloat() / TARGET,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val pulse = remember { Animatable(1f) }   // pond ripple (1 = idle)
    val pop = remember { Animatable(1f) }     // number bounce
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val tapSound = remember { TapSound(context) }
    DisposableEffect(tapSound) { onDispose { tapSound.release() } }

    // looping clock for idle motion
    val clock = rememberInfiniteTransition(label = "clock")
    val t by clock.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )

    fun onTap() {
        count += 1
        prefs.edit().putInt("count", count).apply()
        tapSound.play()
        val milestone = count % 25 == 0
        haptic.performHapticFeedback(
            if (milestone) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
        scope.launch { pulse.snapTo(0f); pulse.animateTo(1f, tween(900)) }
        scope.launch {
            pop.snapTo(if (milestone) 1.5f else 1.2f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val done = count >= TARGET

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
    ) {
        Canvas(Modifier.fillMaxSize()) { drawGarden(progress, t, pulse.value) }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 32.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!done) {
                Text("أَسْتَغْفِرُ اللَّهَ", fontSize = 44.sp, color = Ink)
                Text(
                    "Astaghfirullah · I seek Allah's forgiveness", fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center
                )
            } else {
                Text("تَقَبَّلَ اللَّهُ مِنَّا وَمِنْكُمْ", fontSize = 36.sp, color = Ink)
                Text(
                    "May Allah accept it from us and from you", fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$count", fontSize = 64.sp, fontWeight = FontWeight.Light, color = Ink,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            )
            Spacer(Modifier.height(8.dp))
            ProgressTrack(progress)
        }

        Text(
            "Hold to reset", fontSize = 12.sp, color = Ink.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .combinedClickable(onClick = {}, onLongClick = {
                    count = 0
                    prefs.edit().putInt("count", 0).apply()
                })
                .padding(16.dp)
        )
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    val labels = listOf("Drop", "Pond", "Meadow", "Garden")
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val y = size.height / 2
            drawLine(Ink.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y), 4f, StrokeCap.Round)
            drawLine(Ink.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width * progress, y), 4f, StrokeCap.Round)
            for (i in 0..3) {
                val x = size.width * i / 3f
                drawCircle(if (progress >= i / 3f) Ink else Ink.copy(alpha = 0.3f), 7f, Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, fontSize = 10.sp, color = Ink.copy(alpha = 0.6f)) }
        }
    }
}
