package com.muddassir.safina

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        enableEdgeToEdge()
        setContent { ZikrApp() }
    }
}

private const val CLOCK_SECONDS = 8f
private val MILESTONES = listOf(0.25f, 0.50f, 0.75f, 1.00f)
private val HtmlDarkInk = Color(0xFFF6F2E6)
private val HtmlLightInk = Color(0xFF35424E)
private val CreateInkColor = Color(0xFF4A4436)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZikrApp() {
    val context = LocalContext.current
    val store = remember { ZikrStore(context) }
    val scope = rememberCoroutineScope()

    val initialPages = remember { store.loadPages() }
    var pages by remember { mutableStateOf(initialPages) }
    val counts = remember {
        mutableStateMapOf<String, Int>().apply { initialPages.forEach { put(it.id, it.count) } }
    }
    val startPage = remember {
        initialPages.indexOfFirst { it.sceneId == store.lastSceneId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { pages.size + 1 })
    val htmlDark = remember { mutableStateMapOf<String, Boolean>() }

    val currentPage = pagerState.currentPage
    val createPageIndex = pages.size
    val onCreatePage = currentPage >= createPageIndex
    val zikr = pages.getOrNull(currentPage)
    val target = zikr?.target ?: 100
    val count = zikr?.let { counts[it.id] } ?: 0
    val targetProgress = if (target > 0) count.coerceAtMost(target).toFloat() / target else 0f
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val done = zikr != null && count >= target

    val pulse = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    val shimmer = remember { Animatable(0f) }
    val celebrate = remember { Animatable(if (done) 1f else 0f) }
    val golden = remember { Animatable(if (done) 1f else 0f) }
    var tapStrength by remember { mutableFloatStateOf(1f) }
    var tapSignal by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableStateOf(Offset(0.5f, 0.65f)) }
    var htmlVersion by remember { mutableIntStateOf(0) }

    val vibrate = remember { TapVibrate(context) }
    val tapSound = remember { TapSound(context) }
    DisposableEffect(tapSound) { onDispose { tapSound.release() } }

    val calm = remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }

    var clockTime by remember { mutableFloatStateOf(0f) }
    var appPaused by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> appPaused = false
                Lifecycle.Event.ON_PAUSE -> appPaused = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(appPaused) {
        if (appPaused) return@LaunchedEffect
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

    LaunchedEffect(currentPage, pages.size) {
        val z = pages.getOrNull(currentPage)
        if (z != null) {
            store.lastSceneId = z.sceneId
            val finished = (counts[z.id] ?: 0) >= z.target
            pulse.snapTo(1f)
            shimmer.snapTo(0f)
            celebrate.snapTo(if (finished) 1f else 0f)
            golden.snapTo(if (finished) 1f else 0f)
        } else {
            pulse.snapTo(1f)
            shimmer.snapTo(0f)
            celebrate.snapTo(0f)
            golden.snapTo(0f)
        }
    }

    fun refreshPages() {
        val updated = store.loadPages()
        updated.forEach { z -> if (counts[z.id] == null) counts[z.id] = z.count }
        pages = updated
    }

    fun onTapAt(nx: Float, ny: Float) {
        if (onCreatePage) return
        val z = pages.getOrNull(currentPage) ?: return
        val cur = counts[z.id] ?: 0
        val before = cur.coerceAtMost(z.target).toFloat() / z.target
        val next = cur + 1
        counts[z.id] = next
        store.saveCount(z.id, next)
        tapSound.play()
        if (z.sceneId.startsWith(HTML_SCENE_PREFIX)) {
            lastTap = Offset(nx, ny)
            tapSignal++
        }
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

    fun inkFor(z: Zikr?, p: Float): Color = when {
        z == null -> CreateInkColor
        z.sceneId.startsWith(HTML_SCENE_PREFIX) ->
            if (htmlDark[z.id] == true) HtmlDarkInk else HtmlLightInk
        else -> Scenes.byId(z.sceneId).textColor(p)
    }

    val offsetFraction = pagerState.currentPageOffsetFraction
    val baseInk = inkFor(zikr, progress)
    val neighborIndex = if (offsetFraction >= 0f) currentPage + 1 else currentPage - 1
    val neighbor = pages.getOrNull(neighborIndex)
    val neighborInk = if (neighborIndex in 0..pages.size) {
        val np = if (neighbor != null) {
            (counts[neighbor.id] ?: 0).coerceAtMost(neighbor.target).toFloat() / neighbor.target
        } else 0f
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

    /* ---------- upload flow ---------- */
    val instructions = remember { readAsset(context, "instructions.txt")?.decodeToString().orEmpty() }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var pendingReplaceId by remember { mutableStateOf<String?>(null) }
    var replaceRequestId by remember { mutableStateOf<String?>(null) }
    var scrollTarget by remember { mutableStateOf<Int?>(null) }
    var renameTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var deleteTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            replaceRequestId = null
            return@rememberLauncherForActivityResult
        }
        val replacing = replaceRequestId
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            when {
                bytes == null -> toast(context, "Couldn't read that file")
                bytes.isEmpty() -> toast(context, "That file is empty")
                bytes.size > MAX_HTML_BYTES -> toast(context, "Please choose a file under 2 MB")
                !looksLikeHtml(bytes) -> toast(context, "That doesn't look like an HTML file")
                else -> {
                    pendingName = nameFromHtml(bytes, uri)
                    pendingReplaceId = replacing
                    pendingBytes = bytes
                }
            }
            replaceRequestId = null
        }
    }

    val exampleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) scope.launch {
            val bytes = withContext(Dispatchers.IO) { readAsset(context, "garden_reference.html") }
            if (bytes != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                }
                toast(context, "Example scene saved")
            } else {
                toast(context, "Example file is unavailable")
            }
        }
    }

    LaunchedEffect(pages.size, scrollTarget) {
        val targetIndex = scrollTarget ?: return@LaunchedEffect
        if (targetIndex in 0 until pagerState.pageCount) {
            pagerState.scrollToPage(targetIndex)
            scrollTarget = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Re-key on the page: the tap handler must follow swipes and newly added pages.
            .pointerInput(currentPage, pages.size) {
                detectTapGestures { point ->
                    onTapAt(point.x / size.width, point.y / size.height)
                }
            }
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page < pages.size) {
                val pageZikr = pages[page]
                val active = page == currentPage
                val isHtml = pageZikr.sceneId.startsWith(HTML_SCENE_PREFIX)
                val pageRaw = (counts[pageZikr.id] ?: 0).coerceAtMost(pageZikr.target).toFloat() / pageZikr.target

                if (isHtml) {
                    val meta = store.customScenes().firstOrNull { pageZikr.sceneId == "$HTML_SCENE_PREFIX${it.id}" }
                    HtmlPage(
                        zikr = pageZikr,
                        store = store,
                        version = htmlVersion,
                        progress = targetProgress,
                        tapSignal = tapSignal,
                        tap = lastTap,
                        paused = !active || appPaused,
                        onDarkScene = { isDark -> htmlDark[pageZikr.id] = isDark },
                        onReplace = {
                            meta?.let {
                                replaceRequestId = it.id
                                pickLauncher.launch(arrayOf("text/html", "*/*"))
                            }
                        },
                        onDelete = { meta?.let { deleteTarget = it } }
                    )
                } else {
                    val scene = Scenes.byId(pageZikr.sceneId)
                    Canvas(Modifier.fillMaxSize()) {
                        with(scene) {
                            draw(
                                SceneFrame(
                                    progress = if (active) progress else pageRaw,
                                    time = if (active) clockTime else 0f,
                                    pulse = pulse.value,
                                    ripple = tapStrength,
                                    shimmer = shimmer.value,
                                    golden = golden.value,
                                    celebrate = celebrate.value,
                                    calm = calm
                                )
                            )
                        }
                    }
                }
            } else {
                CreatePage(
                    time = clockTime,
                    scenes = store.customScenes(),
                    onUpload = {
                        pendingReplaceId = null
                        pickLauncher.launch(arrayOf("text/html", "*/*"))
                    },
                    onCopyInstructions = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Scene instructions", instructions))
                        toast(context, "Copied")
                    },
                    onSaveExample = { exampleLauncher.launch("garden_reference.html") },
                    onReplace = {
                        replaceRequestId = it.id
                        pickLauncher.launch(arrayOf("text/html", "*/*"))
                    },
                    onRename = { renameTarget = it },
                    onDelete = { deleteTarget = it }
                )
            }
        }

        if (!onCreatePage) {
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
                            Text(zikr?.arabic.orEmpty(), fontSize = 44.sp, color = ink, style = textStyle)
                            Text(
                                zikr?.translation.orEmpty(), fontSize = 13.sp,
                                color = ink.copy(alpha = 0.75f), textAlign = TextAlign.Center,
                                style = textStyle
                            )
                        } else {
                            Text(zikr?.arabicDone.orEmpty(), fontSize = 36.sp, color = ink, style = textStyle)
                            Text(
                                zikr?.translationDone.orEmpty(), fontSize = 13.sp,
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
                ProgressTrack(
                    progress = progress,
                    ink = ink,
                    labels = zikr?.let { Scenes.byIdOrLabels(it.sceneId) } ?: emptyList()
                )
            }

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size + 1) { i ->
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
                    .combinedClickable(onClick = {}, onLongClick = {
                        val z = pages.getOrNull(currentPage)
                        if (z != null) {
                            counts[z.id] = 0
                            store.saveCount(z.id, 0)
                        }
                    })
                    .padding(14.dp)
            )

            if (BuildConfig.DEBUG) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .size(64.dp)
                        .combinedClickable(onClick = {}, onLongClick = {
                            val z = pages.getOrNull(currentPage) ?: return@combinedClickable
                            val cur = counts[z.id] ?: 0
                            val step = z.target / 4
                            val next = when {
                                cur < step -> step
                                cur < step * 2 -> step * 2
                                cur < step * 3 -> step * 3
                                cur < z.target -> z.target
                                else -> 0
                            }
                            counts[z.id] = next
                            store.saveCount(z.id, next)
                        })
                )
            }
        }

        pendingBytes?.let { bytes ->
            HtmlPreviewOverlay(
                html = bytes.decodeToString(),
                confirmLabel = if (pendingReplaceId == null) "Add" else "Replace",
                onCancel = {
                    pendingBytes = null
                    pendingReplaceId = null
                },
                onConfirm = {
                    val replaceId = pendingReplaceId
                    if (replaceId != null) {
                        if (store.saveHtml(replaceId, bytes)) {
                            htmlVersion++
                            htmlDark.remove("zikr_html_$replaceId")
                            toast(context, "Scene replaced")
                        } else {
                            toast(context, "Couldn't save the scene")
                        }
                    } else {
                        val newId = UUID.randomUUID().toString().replace("-", "").take(16)
                        val name = pendingName.ifBlank { "My scene" }
                        if (store.saveHtml(newId, bytes)) {
                            store.addCustomScene(
                                HtmlSceneMeta(newId, name, "$newId.html", System.currentTimeMillis())
                            )
                            htmlVersion++
                            refreshPages()
                            scrollTarget = pages.size - 1
                        } else {
                            toast(context, "Couldn't save the scene")
                        }
                    }
                    pendingBytes = null
                    pendingReplaceId = null
                }
            )
        }

        renameTarget?.let { meta ->
            RenameSceneDialog(
                currentName = meta.name,
                onDismiss = { renameTarget = null },
                onRename = { newName ->
                    store.renameCustomScene(meta.id, newName)
                    renameTarget = null
                    refreshPages()
                }
            )
        }

        deleteTarget?.let { meta ->
            DeleteSceneDialog(
                name = meta.name,
                onDismiss = { deleteTarget = null },
                onDelete = {
                    store.removeCustomScene(meta.id)
                    htmlDark.remove("zikr_html_${meta.id}")
                    counts.remove("zikr_html_${meta.id}")
                    deleteTarget = null
                    refreshPages()
                }
            )
        }
    }
}

/** Labels for the progress track: native scenes have their own, uploaded ones use the garden's. */
private fun Scenes.byIdOrLabels(sceneId: String): List<String> =
    if (sceneId.startsWith(HTML_SCENE_PREFIX)) listOf("Start", "Quarter", "Half", "Done")
    else byId(sceneId).labels

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
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    val id = zikr.sceneId.removePrefix(HTML_SCENE_PREFIX)
    val html by produceState<String?>(initialValue = null, id, version) {
        value = withContext(Dispatchers.IO) { store.readHtml(id) }
    }
    var failed by remember(id, version) { mutableStateOf(false) }

    val htmlNow = html
    if (htmlNow == null) {
        SceneFallback("This scene file is missing.", onReplace, onDelete)
    } else if (failed) {
        SceneFallback(
            "This scene couldn't be loaded. Check that it follows the instructions.",
            onReplace, onDelete
        )
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
                onError = { failed = true }
            )
        }
    }
}

@Composable
private fun SceneFallback(message: String, onReplace: () -> Unit, onDelete: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD9E2E4), Color(0xFFF1EDE2)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message, fontSize = 13.sp, color = Color(0xFF4A4436),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
            Row {
                TextButton(onClick = onReplace) { Text("Replace") }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFF9A4A3E))
                }
            }
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

/* ---------- upload helpers ---------- */

private fun looksLikeHtml(bytes: ByteArray): Boolean {
    val head = bytes.decodeToString(0, minOf(bytes.size, 1024)).lowercase()
    return "<html" in head || "<!doctype html" in head || "<canvas" in head
}

private fun nameFromHtml(bytes: ByteArray, uri: Uri): String {
    val head = bytes.decodeToString(0, minOf(bytes.size, 8192))
    val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(head)?.groupValues?.getOrNull(1)
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
    if (!title.isNullOrBlank()) return title.take(48)
    val segment = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: ""
    return segment.substringBeforeLast('.').take(48).ifBlank { "My scene" }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun readAsset(context: Context, name: String): ByteArray? =
    runCatching { context.assets.open(name).use { it.readBytes() } }.getOrNull()
