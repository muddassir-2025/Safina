package com.muddassir.safina

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scene studio: upload, preview and manage the HTML scenes that can be attached to cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SceneStudioScreen(
    time: Float,
    scenes: List<HtmlSceneMeta>,
    settings: SettingsActions,
    thumbVersion: Int,
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onCopyInstructions: () -> Unit,
    onSaveExample: () -> Unit,
    onReplace: (HtmlSceneMeta) -> Unit,
    onRename: (HtmlSceneMeta) -> Unit,
    onDelete: (HtmlSceneMeta) -> Unit,
    onPreview: (HtmlSceneMeta) -> Unit
) {
    BackHandler { onBack() }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val visible = remember(scenes, query) {
        val needle = query.trim()
        if (needle.isEmpty()) scenes
        else scenes.filter { it.name.contains(needle, ignoreCase = true) }
    }

    AppBackdrop(time) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SafinaField)
                        .border(1.dp, SafinaBorder, CircleShape)
                        .combinedClickable(onClick = onBack, onLongClick = onBack),
                    contentAlignment = Alignment.Center
                ) { ChevronLeftGlyph(SafinaInk, 19.dp) }

                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "SCENES",
                        fontSize = 8.5.sp, color = SafinaMuted, letterSpacing = 3.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("Scene studio", fontSize = 21.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
                }

                HeaderIcon(onClick = {
                    searching = !searching
                    if (!searching) query = ""
                }) { SearchGlyph(SafinaInk.copy(alpha = 0.8f), 18.dp) }

                Spacer(Modifier.width(6.dp))

                HeaderIcon(onClick = { showInfo = true }) { InfoGlyph(SafinaInk.copy(alpha = 0.8f)) }

                Spacer(Modifier.width(6.dp))

                SettingsMenu(
                    actions = settings.copy(onHome = onBack),
                    darkIcon = false
                )
            }

            AnimatedVisibility(
                visible = searching,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    Modifier
                        .padding(start = 24.dp, end = 24.dp, top = 14.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(SafinaField)
                        .border(1.dp, SafinaBorder, RoundedCornerShape(15.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchGlyph(SafinaMuted.copy(alpha = 0.8f), 16.dp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search scenes", fontSize = 13.sp, color = SafinaMuted.copy(alpha = 0.7f))
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            cursorBrush = SolidColor(SafinaAccent),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp, color = SafinaInk
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioAction("Upload", primary = true, onClick = onUpload, modifier = Modifier.weight(1f))
                StudioAction("Instructions", primary = false, onClick = onCopyInstructions, modifier = Modifier.weight(1.2f))
                StudioAction("Example", primary = false, onClick = onSaveExample, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(22.dp))
            Text(
                if (query.isBlank()) "YOUR SCENES" else "RESULTS",
                fontSize = 8.5.sp, color = SafinaMuted.copy(alpha = 0.85f), letterSpacing = 2.4.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
            )
            Spacer(Modifier.height(10.dp))

            if (scenes.isEmpty()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No scenes yet", fontSize = 18.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Upload a single self-contained HTML file. It becomes a scene you can pick for any card.",
                        fontSize = 12.sp, color = SafinaMuted, textAlign = TextAlign.Center, lineHeight = 18.sp
                    )
                }
            } else if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nothing matches “$query”", fontSize = 13.sp, color = SafinaMuted)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.id }) { meta ->
                        SceneRow(
                            meta = meta,
                            thumbVersion = thumbVersion,
                            onReplace = { onReplace(meta) },
                            onRename = { onRename(meta) },
                            onDelete = { onDelete(meta) },
                            onOpen = { onPreview(meta) }
                        )
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding().height(6.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Got it") } },
            title = { Text("Your own scenes") },
            text = {
                Text(
                    "A scene is a single self-contained HTML file (under 2 MB) with an animated canvas " +
                        "background — no text, no network.\n\n" +
                        "Upload puts it in your scene list and in every card's scene picker. " +
                        "Instructions copies the full brief to paste into an AI chat. " +
                        "Example saves a working reference file you can give the AI.",
                    fontSize = 12.5.sp, lineHeight = 19.sp
                )
            }
        )
    }
}

@Composable
private fun HeaderIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SafinaField)
            .border(1.dp, SafinaBorder, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun StudioAction(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (primary) SafinaInk else SafinaField)
            .border(
                1.dp,
                if (primary) Color.Transparent else SafinaBorder,
                RoundedCornerShape(15.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (primary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlusGlyph(SafinaOnInk, 14.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    label, fontSize = 11.5.sp, color = SafinaOnInk, fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        } else {
            Text(
                label, fontSize = 10.5.sp, color = SafinaInk, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneRow(
    meta: HtmlSceneMeta,
    thumbVersion: Int,
    onReplace: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val date = remember(meta.addedAt) {
        if (meta.addedAt <= 0) "" else SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(Date(meta.addedAt))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SafinaSurface)
            .border(1.dp, SafinaBorder, RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onOpen)
    ) {
        SceneThumb(
            sceneId = "$HTML_SCENE_PREFIX${meta.id}",
            version = thumbVersion,
            modifier = Modifier.fillMaxSize(),
            corner = 22.dp
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B100C).copy(alpha = 0.12f),
                            Color(0xFF0B100C).copy(alpha = 0.52f)
                        )
                    )
                )
        )
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                meta.name,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                color = Color(0xFFFDFBF3),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (date.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    date,
                    fontSize = 9.5.sp,
                    color = Color(0xFFFDFBF3).copy(alpha = 0.7f),
                    letterSpacing = 0.6.sp
                )
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B100C).copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .combinedClickable(
                        onClick = { menuOpen = true },
                        onLongClick = { menuOpen = true }
                    ),
                contentAlignment = Alignment.Center
            ) { MoreGlyph(Color(0xFFFDFBF3), 18.dp) }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = SafinaSurface
            ) {
                DropdownMenuItem(
                    text = { Text("Replace", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onReplace() }
                )
                DropdownMenuItem(
                    text = { Text("Rename", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFB06A5C)) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

/** Full-screen preview with a native slider and a tap test, before the scene is added. */
@Composable
fun HtmlPreviewOverlay(
    html: String,
    confirmLabel: String,
    onSnapshot: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var sliderProgress by remember { mutableFloatStateOf(0.25f) }
    var tapSignal by remember { mutableIntStateOf(0) }
    var tapPoint by remember { mutableStateOf(Offset(0.5f, 0.65f)) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0F14).copy(alpha = 0.30f))) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        tapPoint = Offset(point.x / size.width, point.y / size.height)
                        tapSignal++
                    }
                }
        ) {
            HtmlSceneLayer(
                html = html,
                progress = sliderProgress,
                tapSignal = tapSignal,
                tapX = tapPoint.x,
                tapY = tapPoint.y,
                paused = false,
                onReady = { ready = true },
                onError = { failed = true },
                onSnapshot = onSnapshot
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFBF8F0).copy(alpha = 0.96f))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (failed) {
                Text(
                    "This scene couldn't be loaded. Check that it follows the instructions.",
                    fontSize = 13.sp, color = Color(0xFF8A4034), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Text("Preview", fontSize = 12.sp, color = Color(0xFF4A4436).copy(alpha = 0.6f))
                Slider(
                    value = sliderProgress,
                    onValueChange = { sliderProgress = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (ready && !failed) Color(0xFF35423A) else Color(0xFFE2DED2))
                        .combinedClickable(
                            onClick = { if (ready && !failed) onConfirm() },
                            onLongClick = {}
                        )
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        confirmLabel, fontSize = 12.5.sp,
                        color = if (ready && !failed) Color(0xFFF6F2E6) else Color(0xFF6C756D),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RenameSceneDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) onRename(trimmed) else onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Rename scene") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                singleLine = true
            )
        }
    )
}

@Composable
fun DeleteSceneDialog(
    name: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFF9A4A3E)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Delete scene?") },
        text = {
            Text("“$name” will be removed. Cards using it will switch to the Garden scene.")
        }
    )
}

/** Full-screen live preview of an uploaded scene, opened by tapping its studio card. */
@Composable
fun SceneViewerOverlay(
    meta: HtmlSceneMeta,
    store: ZikrStore,
    onDismiss: () -> Unit
) {
    val html by produceState<String?>(initialValue = null, meta.id) {
        value = withContext(Dispatchers.IO) { store.readHtml(meta.id) }
    }
    var sliderProgress by remember { mutableFloatStateOf(0.25f) }
    var tapSignal by remember { mutableIntStateOf(0) }
    var tapPoint by remember { mutableStateOf(Offset(0.5f, 0.65f)) }
    var failed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F0B).copy(alpha = 0.45f))) {
        val document = html
        if (document == null) {
            Text(
                "Loading scene…",
                fontSize = 13.sp, color = Color(0xFFFDFBF3),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(document) {
                        detectTapGestures { point ->
                            tapPoint = Offset(point.x / size.width, point.y / size.height)
                            tapSignal++
                        }
                    }
            ) {
                HtmlSceneLayer(
                    html = document,
                    progress = sliderProgress,
                    tapSignal = tapSignal,
                    tapX = tapPoint.x,
                    tapY = tapPoint.y,
                    paused = false,
                    onError = { failed = true }
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFBF8F0).copy(alpha = 0.97f))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        meta.name, fontSize = 14.sp, color = Color(0xFF3D453E),
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text("Swipe the slider to preview", fontSize = 10.sp, color = Color(0xFF6C756D))
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9E4D7))
                        .combinedClickable(onClick = onDismiss, onLongClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { CloseGlyph(Color(0xFF3D453E), 13.dp) }
            }
            if (failed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "This scene couldn't be loaded.",
                    fontSize = 12.sp, color = Color(0xFF8A4034)
                )
            } else {
                Slider(
                    value = sliderProgress,
                    onValueChange = { sliderProgress = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
