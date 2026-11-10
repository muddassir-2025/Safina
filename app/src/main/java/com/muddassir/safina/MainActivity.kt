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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZikrApp() }
    }
}

private const val CLOCK_SECONDS = 8f
private val MILESTONES = listOf(0.25f, 0.50f, 0.75f, 1.00f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZikrApp() {
    val context = LocalContext.current
    val store = remember { ZikrStore(context) }
    val zikrs = remember { store.load() }
    val startPage = remember {
        val saved = store.lastSceneId
        zikrs.indexOfFirst { it.sceneId == saved }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { zikrs.size })
    val counts = remember {
        mutableStateMapOf<String, Int>().apply { zikrs.forEach { put(it.id, it.count) } }
    }

    val currentPage = pagerState.currentPage
    val zikr = zikrs[currentPage]
    val scene = remember(zikr.sceneId) { Scenes.byId(zikr.sceneId) }
    val target = zikr.target
    val count = counts[zikr.id] ?: 0
    val progress by animateFloatAsState(
        targetValue = count.coerceAtMost(target).toFloat() / target,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val done = count >= target

    val pulse = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    val shimmer = remember { Animatable(0f) }
    val celebrate = remember { Animatable(if (done) 1f else 0f) }
    val golden = remember { Animatable(if (done) 1f else 0f) }
    var tapStrength by remember { mutableFloatStateOf(1f) }

    val scope = rememberCoroutineScope()
    val vibrate = remember { TapVibrate(context) }
    val tapSound = remember { TapSound(context) }
    DisposableEffect(tapSound) { onDispose { tapSound.release() } }

    val calm = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    // One looping clock; paused while the app is in the background.
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

    // Coming back to a page never replays its completion moment.
    LaunchedEffect(currentPage) {
        val z = zikrs[currentPage]
        store.lastSceneId = z.sceneId
        val finished = (counts[z.id] ?: 0) >= z.target
        pulse.snapTo(1f)
        shimmer.snapTo(0f)
        celebrate.snapTo(if (finished) 1f else 0f)
        golden.snapTo(if (finished) 1f else 0f)
    }

    fun applyCount(value: Int) {
        val z = zikrs[currentPage]
        counts[z.id] = value
        store.saveCount(z.id, value)
    }

    fun onTap() {
        val z = zikrs[currentPage]
        val cur = counts[z.id] ?: 0
        val before = cur.coerceAtMost(z.target).toFloat() / z.target
        val next = cur + 1
        applyCount(next)
        tapSound.play()
        val after = next.coerceAtMost(z.target).toFloat() / z.target
        val milestone = MILESTONES.any { before < it && after >= it }
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
        if (after >= 1f && before < 1f) {
            scope.launch { celebrate.snapTo(0f); celebrate.animateTo(1f, tween(2000)) }
            scope.launch { golden.snapTo(0f); golden.animateTo(1f, tween(6000)) }
        }
    }

    // Text colour follows the scene, and crossfades while swiping.
    val offsetFraction = pagerState.currentPageOffsetFraction
    val baseInk = scene.textColor(progress)
    val neighborIndex = if (offsetFraction >= 0f) currentPage + 1 else currentPage - 1
    val neighborInk = if (neighborIndex in zikrs.indices) {
        val nz = zikrs[neighborIndex]
        val np = (counts[nz.id] ?: 0).coerceAtMost(nz.target).toFloat() / nz.target
        Scenes.byId(nz.sceneId).textColor(np)
    } else baseInk
    val ink = lerp(baseInk, neighborInk, abs(offsetFraction).coerceIn(0f, 1f))
    val textShadow = if (ink.luminance() > 0.55f) {
        Color.Black.copy(alpha = 0.42f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }
    val textStyle = TextStyle(
        shadow = Shadow(color = textShadow, blurRadius = 14f, offset = Offset(0f, 1f))
    )

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val pageZikr = zikrs[page]
            val pageScene = remember(pageZikr.sceneId) { Scenes.byId(pageZikr.sceneId) }
            val pageProgress =
                (counts[pageZikr.id] ?: 0).coerceAtMost(pageZikr.target).toFloat() / pageZikr.target
            val active = page == currentPage
            Canvas(Modifier.fillMaxSize()) {
                val frame = SceneFrame(
                    progress = pageProgress,
                    time = if (active) clockTime else 0f,
                    pulse = pulse.value,
                    ripple = tapStrength,
                    shimmer = shimmer.value,
                    golden = golden.value,
                    celebrate = celebrate.value,
                    calm = calm
                )
                with(pageScene) { draw(frame) }
            }
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
                        Text(zikr.arabic, fontSize = 44.sp, color = ink, style = textStyle)
                        Text(
                            zikr.translation, fontSize = 13.sp,
                            color = ink.copy(alpha = 0.75f), textAlign = TextAlign.Center,
                            style = textStyle
                        )
                    } else {
                        Text(zikr.arabicDone, fontSize = 36.sp, color = ink, style = textStyle)
                        Text(
                            zikr.translationDone, fontSize = 13.sp,
                            color = ink.copy(alpha = 0.75f), textAlign = TextAlign.Center,
                            style = textStyle
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
                    "$value", fontSize = 64.sp, fontWeight = FontWeight.Light, color = ink,
                    style = textStyle,
                    modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                )
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(progress, ink, scene.labels)
        }

        // page indicator dots
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zikrs.forEachIndexed { i, _ ->
                Box(
                    Modifier
                        .size(if (i == currentPage) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (i == currentPage) ink else ink.copy(alpha = 0.40f))
                )
            }
        }

        Text(
            "Hold to reset", fontSize = 12.sp, color = ink.copy(alpha = 0.55f), style = textStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .combinedClickable(onClick = {}, onLongClick = { applyCount(0) })
                .padding(14.dp)
        )

        // Debug builds only: invisible long-press in the top-left corner steps the
        // current scene through 25 / 50 / 75 / 100 / 0 of its target.
        if (BuildConfig.DEBUG) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(64.dp)
                    .combinedClickable(onClick = {}, onLongClick = {
                        val z = zikrs[currentPage]
                        val cur = counts[z.id] ?: 0
                        val step = z.target / 4
                        val next = when {
                            cur < step -> step
                            cur < step * 2 -> step * 2
                            cur < step * 3 -> step * 3
                            cur < z.target -> z.target
                            else -> 0
                        }
                        applyCount(next)
                    })
            )
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float, ink: Color, labels: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val y = size.height / 2
            drawLine(ink.copy(alpha = 0.20f), Offset(0f, y), Offset(size.width, y), 4f, StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.75f), Offset(0f, y), Offset(size.width * progress, y), 4f, StrokeCap.Round)
            for (i in 0..3) {
                val x = size.width * i / 3f
                drawCircle(if (progress >= i / 3f) ink else ink.copy(alpha = 0.30f), 7f, Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, fontSize = 10.sp, color = ink.copy(alpha = 0.65f)) }
        }
    }
}
