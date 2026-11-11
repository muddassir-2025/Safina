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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        enableEdgeToEdge()
        setContent { ZikrApp() }
    }
}

private const val CLOCK_SECONDS = 8f

private enum class AppScreen { Dashboard, Session, Studio }

@Composable
fun ZikrApp() {
    val context = LocalContext.current
    val store = remember { ZikrStore(context) }
    val scope = rememberCoroutineScope()

    var cards by remember { mutableStateOf(store.loadCards()) }
    val counts = remember {
        mutableStateMapOf<String, Int>().apply { cards.forEach { put(it.id, it.count) } }
    }
    var uploaded by remember { mutableStateOf(store.customScenes()) }
    var screen by remember { mutableStateOf(AppScreen.Dashboard) }
    var sessionCardId by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<Zikr?>(null) }
    var htmlVersion by remember { mutableIntStateOf(0) }
    var thumbVersion by remember { mutableIntStateOf(0) }

    var soundEnabled by remember { mutableStateOf(store.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(store.vibrationEnabled) }
    val systemDark = isSystemInDarkTheme()
    var darkMode by remember { mutableStateOf(store.darkModeOverride ?: systemDark) }

    var clockTime by remember { mutableFloatStateOf(0f) }
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }
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
                    elapsedSeconds += dt
                }
                previous = now
            }
        }
    }

    fun refreshCards() {
        cards = store.loadCards()
        cards.forEach { if (counts[it.id] == null) counts[it.id] = it.count }
    }

    fun setCount(zikrId: String, value: Int) {
        counts[zikrId] = value
        store.saveCount(zikrId, value)
    }

    fun saveCard(card: Zikr) {
        if (cards.any { it.id == card.id }) {
            store.updateCard(card)
        } else {
            store.addCard(card)
            counts[card.id] = card.count
        }
        refreshCards()
        editorOpen = false
        editorTarget = null
    }

    fun deleteCard(card: Zikr) {
        store.deleteCard(card.id)
        counts.remove(card.id)
        refreshCards()
        editorOpen = false
        editorTarget = null
    }

    /* ---------- upload flow ---------- */
    val instructions = remember { readAsset(context, "instructions.txt")?.decodeToString().orEmpty() }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var pendingReplaceId by remember { mutableStateOf<String?>(null) }
    var replaceRequestId by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var deleteSceneTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var previewScene by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var pendingThumb by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = editorOpen || pendingBytes != null || previewScene != null) {
        when {
            editorOpen -> {
                editorOpen = false
                editorTarget = null
            }
            previewScene != null -> previewScene = null
            else -> {
                pendingBytes = null
                pendingReplaceId = null
                pendingThumb = null
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            replaceRequestId = null
            return@rememberLauncherForActivityResult
        }
        val replacing = replaceRequestId
        pendingThumb = null
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

    fun openUpload() {
        pendingReplaceId = null
        pickLauncher.launch(arrayOf("text/html", "*/*"))
    }

    val baseSettings = SettingsActions(
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        onToggleSound = {
            soundEnabled = !soundEnabled
            store.soundEnabled = soundEnabled
        },
        onToggleVibration = {
            vibrationEnabled = !vibrationEnabled
            store.vibrationEnabled = vibrationEnabled
        },
        darkMode = darkMode,
        onToggleDarkMode = {
            darkMode = !darkMode
            store.darkModeOverride = darkMode
        },
        onStudio = { screen = AppScreen.Studio }
    )

    CompositionLocalProvider(
        LocalSafinaPalette provides if (darkMode) DarkPalette else LightPalette
    ) {
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = screen, animationSpec = tween(420), label = "screen") { current ->
            when (current) {
                AppScreen.Dashboard -> DashboardScreen(
                    cards = cards,
                    counts = counts,
                    uploaded = uploaded,
                    time = clockTime,
                    onOpen = { z ->
                        sessionCardId = z.id
                        screen = AppScreen.Session
                    },
                    onNew = {
                        editorTarget = null
                        editorOpen = true
                    },
                    onEdit = { z ->
                        editorTarget = z
                        editorOpen = true
                    },
                    onDelete = { z -> deleteCard(z) },
                    onReorder = { from, to ->
                        if (from in cards.indices && to in cards.indices) {
                            cards = cards.toMutableList().apply { add(to, removeAt(from)) }
                            store.moveCard(from, to)
                        }
                    },
                    onOpenScenes = { screen = AppScreen.Studio },
                    settings = baseSettings,
                    thumbVersion = thumbVersion
                )

                AppScreen.Session -> SessionScreen(
                    cards = cards,
                    counts = counts,
                    store = store,
                    htmlVersion = htmlVersion,
                    time = clockTime,
                    elapsed = elapsedSeconds,
                    paused = appPaused,
                    settings = baseSettings,
                    startCardId = sessionCardId,
                    onCount = { id, value -> setCount(id, value) },
                    onSnapshot = { zikr, dataUrl ->
                        if (zikr.sceneId.startsWith(HTML_SCENE_PREFIX)) {
                            val id = zikr.sceneId.removePrefix(HTML_SCENE_PREFIX)
                            if (!store.hasThumbnail(id)) {
                                decodePngDataUrl(dataUrl)?.let { bytes ->
                                    if (store.saveThumbnail(id, bytes)) thumbVersion++
                                }
                            }
                        }
                    },
                    onHome = { screen = AppScreen.Dashboard },
                    onStudio = { screen = AppScreen.Studio }
                )

                AppScreen.Studio -> SceneStudioScreen(
                    time = clockTime,
                    scenes = uploaded,
                    settings = baseSettings,
                    thumbVersion = thumbVersion,
                    onBack = { screen = AppScreen.Dashboard },
                    onUpload = { openUpload() },
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
                    onDelete = { deleteSceneTarget = it },
                    onPreview = { previewScene = it }
                )
            }
        }

        if (editorOpen) {
            ZikrEditorScreen(
                existing = editorTarget,
                cards = cards,
                uploaded = uploaded,
                thumbVersion = thumbVersion,
                onDismiss = {
                    editorOpen = false
                    editorTarget = null
                },
                onSave = { saveCard(it) },
                onDelete = { deleteCard(it) }
            )
        }

        pendingBytes?.let { bytes ->
            HtmlPreviewOverlay(
                html = bytes.decodeToString(),
                confirmLabel = if (pendingReplaceId == null) "Add scene" else "Replace",
                onSnapshot = { pendingThumb = it },
                onCancel = {
                    pendingBytes = null
                    pendingReplaceId = null
                    pendingThumb = null
                },
                onConfirm = {
                    val replaceId = pendingReplaceId
                    val snapshot = pendingThumb?.let { decodePngDataUrl(it) }
                    if (replaceId != null) {
                        if (store.saveHtml(replaceId, bytes)) {
                            store.deleteThumbnail(replaceId)
                            snapshot?.let { store.saveThumbnail(replaceId, it) }
                            htmlVersion++
                            thumbVersion++
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
                            snapshot?.let { store.saveThumbnail(newId, it) }
                            uploaded = store.customScenes()
                            thumbVersion++
                            toast(context, "Scene added · pick it in a dhikr card")
                        } else {
                            toast(context, "Couldn't save the scene")
                        }
                    }
                    pendingBytes = null
                    pendingReplaceId = null
                    pendingThumb = null
                }
            )
        }

        ThumbnailHarvester(
            scenes = uploaded,
            store = store,
            version = thumbVersion,
            onCaptured = { thumbVersion++ }
        )

        previewScene?.let { meta ->
            SceneViewerOverlay(
                meta = meta,
                store = store,
                onDismiss = { previewScene = null }
            )
        }

        renameTarget?.let { meta ->
            RenameSceneDialog(
                currentName = meta.name,
                onDismiss = { renameTarget = null },
                onRename = { newName ->
                    store.renameCustomScene(meta.id, newName)
                    renameTarget = null
                    uploaded = store.customScenes()
                    refreshCards()
                }
            )
        }

        deleteSceneTarget?.let { meta ->
            DeleteSceneDialog(
                name = meta.name,
                onDismiss = { deleteSceneTarget = null },
                onDelete = {
                    store.removeCustomScene(meta.id)
                    deleteSceneTarget = null
                    uploaded = store.customScenes()
                    refreshCards()
                }
            )
        }
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
