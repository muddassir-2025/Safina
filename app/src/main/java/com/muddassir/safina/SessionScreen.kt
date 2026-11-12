package com.muddassir.safina

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val MILESTONES = listOf(0.25f, 0.50f, 0.75f, 1.00f)
private val HtmlDarkInk = Color(0xFFF6F2E6)
private val HtmlLightInk = Color(0xFF35424E)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionScreen(
    cards: List<Zikr>,
    counts: Map<String, Int>,
    store: ZikrStore,
    htmlVersion: Int,
    time: Float,
    elapsed: Float,
    paused: Boolean,
    settings: SettingsActions,
    startCardId: String?,
    onCount: (String, Int) -> Unit,
    onSnapshot: (Zikr, String) -> Unit,
    onHome: () -> Unit,
    onStudio: () -> Unit
) {
    if (cards.isEmpty()) return
    BackHandler { onHome() }

    val startPage = remember(startCardId) {
        cards.indexOfFirst { it.id == startCardId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { cards.size })
    val htmlDark = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrate = remember { TapVibrate(context) }
    val tapSound = remember { TapSound(context) }
    DisposableEffect(tapSound) { onDispose { tapSound.release() } }

    val currentPage = pagerState.currentPage
    val zikr = cards.getOrNull(currentPage)
    LaunchedEffect(zikr?.id) {
        val z = zikr ?: return@LaunchedEffect
        Analytics.sessionStarted(z.id, z.sceneId, z.target)
    }
    val target = zikr?.target ?: 1
    val count = zikr?.let { counts[it.id] ?: it.count } ?: 0
    val done = zikr != null && count >= target
    val targetProgress = if (target > 0) count.coerceAtMost(target).toFloat() / target else 0f
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val pulse = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    val shimmer = remember { Animatable(0f) }
    val celebrate = remember { Animatable(if (done) 1f else 0f) }
    val golden = remember { Animatable(if (done) 1f else 0f) }
    var tapStrength by remember { mutableFloatStateOf(1f) }
    var tapSignal by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableStateOf(Offset(0.5f, 0.65f)) }

    LaunchedEffect(currentPage, cards.size) {
        val z = cards.getOrNull(currentPage)
        if (z != null) {
            val finished = (counts[z.id] ?: 0) >= z.target
            pulse.snapTo(1f)
            shimmer.snapTo(0f)
            celebrate.snapTo(if (finished) 1f else 0f)
            golden.snapTo(if (finished) 1f else 0f)
        }
    }

    fun onTapAt(nx: Float, ny: Float) {
        val z = cards.getOrNull(pagerState.currentPage) ?: return
        val cur = counts[z.id] ?: z.count
        val before = cur.coerceAtMost(z.target).toFloat() / z.target
        val next = cur + 1
        onCount(z.id, next)
        if (settings.soundEnabled) tapSound.play()
        if (z.sceneId.startsWith(HTML_SCENE_PREFIX)) {
            lastTap = Offset(nx, ny)
            tapSignal++
        }
        val after = next.coerceAtMost(z.target).toFloat() / z.target
        val milestone = MILESTONES.any { before < it && after >= it }
        if (settings.vibrationEnabled) {
            if (milestone) vibrate.strong() else vibrate.light()
        }
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

    fun inkFor(z: Zikr?, p: Float): Color = when {
        z == null -> HtmlLightInk
        z.sceneId.startsWith(HTML_SCENE_PREFIX) ->
            if (htmlDark[z.id] == true) HtmlDarkInk else HtmlLightInk
        else -> Scenes.byId(z.sceneId).textColor(p)
    }

    val offsetFraction = pagerState.currentPageOffsetFraction
    val baseInk = inkFor(zikr, progress)
    val neighborIndex = if (offsetFraction >= 0f) currentPage + 1 else currentPage - 1
    val neighbor = cards.getOrNull(neighborIndex)
    val neighborInk = if (neighbor != null) {
        val np = (counts[neighbor.id] ?: 0).coerceAtMost(neighbor.target).toFloat() / neighbor.target
        inkFor(neighbor, np)
    } else baseInk
    val rawInk = lerp(baseInk, neighborInk, abs(offsetFraction).coerceIn(0f, 1f))
    val ink by animateColorAsState(rawInk, tween(500), label = "ink")
    val textShadow = if (ink.luminance() > 0.55f) {
        Color.Black.copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.32f)
    }
    val textStyle = TextStyle(
        shadow = Shadow(color = textShadow, blurRadius = 10f, offset = Offset(0f, 1f))
    )

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                // Only the scene area counts taps, so the settings control never adds a count.
                .pointerInput(currentPage, cards.size) {
                    detectTapGestures { point ->
                        onTapAt(point.x / size.width, point.y / size.height)
                    }
                }
        ) { page ->
            val pageZikr = cards[page]
            val active = page == currentPage
            val isHtml = pageZikr.sceneId.startsWith(HTML_SCENE_PREFIX)
            val pageRaw = (counts[pageZikr.id] ?: 0)
                .coerceAtMost(pageZikr.target).toFloat() / pageZikr.target.coerceAtLeast(1)

            if (isHtml) {
                HtmlPage(
                    zikr = pageZikr,
                    store = store,
                    version = htmlVersion,
                    progress = if (active) progress else pageRaw,
                    tapSignal = if (active) tapSignal else 0,
                    tap = lastTap,
                    paused = !active || paused,
                    onDarkScene = { isDark -> htmlDark[pageZikr.id] = isDark },
                    onSnapshot = { url -> onSnapshot(pageZikr, url) }
                )
            } else {
                val scene = Scenes.byId(pageZikr.sceneId)
                Canvas(Modifier.fillMaxSize()) {
                    with(scene) {
                        draw(
                            SceneFrame(
                                progress = if (active) progress else pageRaw,
                                time = if (active) time else 0f,
                                elapsed = elapsed,
                                pulse = pulse.value,
                                ripple = tapStrength,
                                shimmer = shimmer.value,
                                golden = golden.value,
                                celebrate = celebrate.value,
                                calm = false
                            )
                        )
                    }
                }
            }
        }

        SettingsMenu(
            actions = settings.copy(onStudio = onStudio, onHome = onHome),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp),
            darkIcon = true
        )

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 84.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = done, animationSpec = tween(600), label = "header") { finished ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!finished) {
                        Text(zikr?.arabic.orEmpty(), fontSize = 40.sp, color = ink, style = textStyle)
                        Text(
                            zikr?.translation.orEmpty(), fontSize = 12.5.sp,
                            color = ink.copy(alpha = 0.78f), textAlign = TextAlign.Center,
                            style = textStyle
                        )
                    } else {
                        Text(zikr?.arabicDone.orEmpty(), fontSize = 34.sp, color = ink, style = textStyle)
                        Text(
                            zikr?.translationDone.orEmpty(), fontSize = 12.5.sp,
                            color = ink.copy(alpha = 0.78f), textAlign = TextAlign.Center,
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
                    "$value", fontSize = 60.sp, fontWeight = FontWeight.Light, color = ink,
                    style = textStyle,
                    modifier = Modifier
                        .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                        .then(
                            if (BuildConfig.DEBUG) Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    val z = cards.getOrNull(pagerState.currentPage)
                                        ?: return@combinedClickable
                                    val cur = counts[z.id] ?: 0
                                    val step = z.target / 4
                                    val next = when {
                                        cur < step -> step
                                        cur < step * 2 -> step * 2
                                        cur < step * 3 -> step * 3
                                        cur < z.target -> z.target
                                        else -> 0
                                    }
                                    onCount(z.id, next)
                                }
                            ) else Modifier
                        )
                )
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(progress = progress, ink = ink)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(cards.size) { i ->
                Box(
                    Modifier
                        .size(if (i == currentPage) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (i == currentPage) ink else ink.copy(alpha = 0.40f))
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .clip(RoundedCornerShape(50))
                .background(ink.copy(alpha = 0.10f))
                .combinedClickable(onClick = {}, onLongClick = {
                    cards.getOrNull(pagerState.currentPage)?.let { onCount(it.id, 0) }
                })
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "Hold to reset", fontSize = 12.sp, color = ink.copy(alpha = 0.80f), style = textStyle
            )
        }
    }
}

@Composable
private fun HtmlPage(
    zikr: Zikr,
    store: ZikrStore,
    version: Int,
    progress: Float,
    tapSignal: Int,
    tap: Offset,
    paused: Boolean,
    onDarkScene: (Boolean) -> Unit,
    onSnapshot: (String) -> Unit
) {
    val id = zikr.sceneId.removePrefix(HTML_SCENE_PREFIX)
    val html by produceState<String?>(initialValue = null, id, version) {
        value = withContext(Dispatchers.IO) { store.readHtml(id) }
    }
    var failed by remember(id, version) { mutableStateOf(false) }

    val htmlNow = html
    if (htmlNow == null) {
        SceneFallback("This scene file is missing.")
    } else if (failed) {
        SceneFallback("This scene couldn't be loaded. Check that it follows the instructions.")
    } else {
        key(id, version) {
            HtmlSceneLayer(
                html = htmlNow,
                progress = progress,
                tapSignal = tapSignal,
                tapX = tap.x,
                tapY = tap.y,
                paused = paused,
                onDarkScene = onDarkScene,
                onError = { failed = true },
                onSnapshot = onSnapshot
            )
        }
    }
}

@Composable
private fun SceneFallback(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFD9E2E4), Color(0xFFF1EDE2)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message, fontSize = 13.sp, color = Color(0xFF4A4436),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float, ink: Color) {
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val y = size.height / 2
            drawLine(ink.copy(alpha = 0.20f), Offset(0f, y), Offset(size.width, y), 4f, StrokeCap.Round)
            drawLine(
                ink.copy(alpha = 0.75f), Offset(0f, y),
                Offset(size.width * progress, y), 4f, StrokeCap.Round
            )
            for (i in 0..3) {
                val x = size.width * i / 3f
                drawCircle(
                    if (progress >= i / 3f) ink else ink.copy(alpha = 0.30f),
                    7f, Offset(x, y)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Start", "Quarter", "Half", "Done").forEach {
                Text(it, fontSize = 10.sp, color = ink.copy(alpha = 0.65f))
            }
        }
    }
}
