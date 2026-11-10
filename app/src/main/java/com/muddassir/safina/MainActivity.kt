package com.muddassir.safina

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
private const val CLOCK_SECONDS = 8f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GardenApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tally", Context.MODE_PRIVATE) }
    var count by remember { mutableIntStateOf(prefs.getInt("count", 0)) }

    // Scene progress eases toward count/100 whenever count changes.
    // On first composition it starts at the saved value, so reopening never replays it.
    val progress by animateFloatAsState(
        targetValue = count.coerceAtMost(TARGET).toFloat() / TARGET,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val pulse = remember { Animatable(1f) }   // pond ripple (1 = idle)
    val pop = remember { Animatable(1f) }     // number bounce
    val shimmer = remember { Animatable(0f) } // milestone shimmer across the pond
    var tapStrength by remember { mutableFloatStateOf(1f) }

    val done = count >= TARGET
    val celebrate = remember { Animatable(if (count >= TARGET) 1f else 0f) }
    val golden = remember { Animatable(if (count >= TARGET) 1f else 0f) }

    val scope = rememberCoroutineScope()
    val vibrate = remember { TapVibrate(context) }
    val tapSound = remember { TapSound(context) }
    DisposableEffect(tapSound) { onDispose { tapSound.release() } }

    // Respect the system "remove animations" setting.
    val calm = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    // One looping clock for all idle motion; it stops while the app is in the background.
    var clockTime by remember { mutableFloatStateOf(0f) }
    var paused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> paused = false
                Lifecycle.Event.ON_PAUSE -> paused = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) {
                    val dt = (now - previous) / 1_000_000_000f
                    clockTime = (clockTime + dt / CLOCK_SECONDS) % 1f
                }
                previous = now
            }
        }
    }

    // At 100: petal burst, then the slow warm shift into golden hour.
    LaunchedEffect(done) {
        if (done) {
            if (celebrate.value < 1f) {
                celebrate.snapTo(0f)
                celebrate.animateTo(1f, tween(2000))
            }
            if (golden.value < 1f) golden.animateTo(1f, tween(6000))
        }
    }

    fun onTap() {
        count += 1
        prefs.edit().putInt("count", count).apply()
        tapSound.play()
        val milestone = count % 25 == 0
        if (milestone) vibrate.strong() else vibrate.light()
        tapStrength = if (milestone) 1.8f else 1f
        scope.launch { pulse.snapTo(0f); pulse.animateTo(1f, tween(900)) }
        scope.launch {
            pop.snapTo(if (milestone) 1.5f else 1.2f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        if (milestone) scope.launch {
            shimmer.snapTo(1f)
            shimmer.animateTo(0f, tween(1200))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawGarden(
                p = progress,
                t = clockTime,
                pulse = pulse.value,
                ripple = tapStrength,
                shimmer = shimmer.value,
                golden = golden.value,
                celebrate = celebrate.value,
                calm = calm
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 32.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = done, animationSpec = tween(600), label = "header") { finished ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!finished) {
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
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 5 }) togetherWith
                        fadeOut(tween(140))
                },
                label = "count"
            ) { value ->
                Text(
                    "$value", fontSize = 64.sp, fontWeight = FontWeight.Light, color = Ink,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                )
            }
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

        // Debug builds only: invisible long-press in the top-left corner jumps the
        // scene through 0 -> 25 -> 50 -> 75 -> 100. Nothing is shown in release.
        if (BuildConfig.DEBUG) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(64.dp)
                    .combinedClickable(onClick = {}, onLongClick = {
                        count = when {
                            count < 25 -> 25
                            count < 50 -> 50
                            count < 75 -> 75
                            count < 100 -> 100
                            else -> 0
                        }
                        prefs.edit().putInt("count", count).apply()
                    })
            )
        }
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
