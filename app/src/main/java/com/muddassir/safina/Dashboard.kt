package com.muddassir.safina

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.muddassir.safina.quran.ui.QuranCollapsedCard
import com.muddassir.safina.quran.ui.QuranExpandedOverlay
import com.muddassir.safina.quran.ui.rememberQuranHubState
import java.util.UUID
import kotlin.math.roundToInt

private val CardHeight = 172.dp
private val CardGap = 12.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    cards: List<Zikr>,
    counts: Map<String, Int>,
    uploaded: List<HtmlSceneMeta>,
    time: Float,
    onOpen: (Zikr) -> Unit,
    onNew: () -> Unit,
    onEdit: (Zikr) -> Unit,
    onDelete: (Zikr) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onOpenScenes: () -> Unit,
    settings: SettingsActions,
    thumbVersion: Int,
    quranOpenRequest: Boolean = false,
    onQuranRequestHandled: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Zikr?>(null) }

    val visible = remember(cards, query) {
        val needle = query.trim()
        if (needle.isEmpty()) cards
        else cards.filter { z ->
            z.arabic.contains(needle, ignoreCase = true) ||
                z.translation.contains(needle, ignoreCase = true) ||
                sceneTitle(z.sceneId, uploaded).contains(needle, ignoreCase = true)
        }
    }

    AppBackdrop(time) {
        val quranState = rememberQuranHubState()
        var quranExpanded by rememberSaveable { mutableStateOf(false) }

        // Safina's own settings sheet can ask for the Quran menu from anywhere.
        // The request is consumed so a later return to the dashboard does not
        // reopen the Quran on its own.
        LaunchedEffect(quranOpenRequest) {
            if (quranOpenRequest) {
                quranExpanded = true
                onQuranRequestHandled()
            }
        }
        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (quranExpanded) Modifier.blur(18.dp) else Modifier)
        ) {
            Column(Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Safina",
                            fontSize = 32.sp,
                            color = SafinaInk,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            "peace",
                            fontSize = 11.sp,
                            color = SafinaMuted.copy(alpha = 0.9f),
                            letterSpacing = 0.2.sp
                        )
                    }
                    DashboardIcon(label = "Quran", onClick = { quranExpanded = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = SafinaInk.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    DashboardIcon(label = "Search", onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }) { SearchGlyph(SafinaInk.copy(alpha = 0.8f), 17.dp) }
                    Spacer(Modifier.width(6.dp))
                    DashboardIcon(label = "Add dhikr", onClick = onNew) {
                        PlusGlyph(SafinaInk.copy(alpha = 0.9f), 16.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    DashboardIcon(label = "Scene studio", onClick = onOpenScenes) {
                        ImageGlyph(SafinaInk.copy(alpha = 0.8f), 18.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    SettingsMenu(
                        actions = settings.copy(onStudio = onOpenScenes, onHome = null)
                    )
                }
                AnimatedVisibility(
                    visible = searching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        SearchField(query, onChange = { query = it })
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (query.isBlank()) "YOUR DHIKR" else "RESULTS",
                        fontSize = 8.5.sp,
                        color = SafinaMuted.copy(alpha = 0.85f),
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${if (query.isBlank()) cards.size else visible.size}",
                        fontSize = 10.sp,
                        color = SafinaMuted.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (cards.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        QuranCollapsedCard(
                            state = quranState,
                            onExpand = { quranExpanded = true }
                        )
                        Box(Modifier.weight(1f)) {
                            EmptyDashboard(onNew = onNew)
                        }
                    }
                } else if (visible.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Nothing matches", fontSize = 18.sp, color = SafinaInk)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Try another word, or search by scene name.",
                            fontSize = 12.sp, color = SafinaMuted, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ReorderableCardList(
                        cards = visible,
                        counts = counts,
                        uploaded = uploaded,
                        thumbVersion = thumbVersion,
                        draggable = query.isBlank(),
                        quranCard = if (query.isBlank()) {
                            { QuranCollapsedCard(quranState, onExpand = { quranExpanded = true }) }
                        } else {
                            null
                        },
                        onOpen = onOpen,
                        onEdit = onEdit,
                        onDelete = { deleteTarget = it },
                        onReorder = onReorder
                    )
                }
            }

        }
        AnimatedVisibility(
            visible = quranExpanded,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(260)
            ),
            exit = fadeOut(tween(160)) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(200)
            )
        ) {
            QuranExpandedOverlay(quranState) { quranExpanded = false }
        }
        }

        deleteTarget?.let { z ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(z)
                        deleteTarget = null
                    }) { Text("Delete", color = Color(0xFF9A4A3E)) }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
                title = { Text("Delete this dhikr?") },
                text = {
                    Text(
                        "“${z.arabic}” will be removed from your dashboard, along with its saved count."
                    )
                }
            )
        }
    }
}

@Composable
private fun DashboardIcon(
    label: String,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SafinaField)
            .border(1.dp, SafinaBorder, CircleShape)
            .semantics { contentDescription = label }
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val focused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SafinaField)
            .border(
                1.dp,
                if (focused.value) SafinaAccent.copy(alpha = 0.55f)
                else SafinaBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchGlyph(SafinaMuted.copy(alpha = 0.8f))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search your dhikr or scenes", fontSize = 13.sp, color = SafinaMuted.copy(alpha = 0.7f))
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                cursorBrush = SolidColor(SafinaAccent),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = SafinaInk
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        if (query.isNotEmpty()) {
            Text(
                "Clear",
                fontSize = 11.sp,
                color = SafinaAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(onClick = { onChange("") }, onLongClick = { onChange("") })
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyDashboard(onNew: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(SafinaField)
                .border(1.dp, SafinaBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SceneSwatch("garden", size = 46.dp, radius = 16.dp)
        }
        Spacer(Modifier.height(20.dp))
        Text("A quiet place to begin", fontSize = 20.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your first dhikr card: choose the words, a target, and a scene.",
            fontSize = 12.5.sp, color = SafinaMuted, textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(Modifier.height(24.dp))
        PillButton("New dhikr", primary = true, onClick = onNew)
    }
}

@Composable
private fun BottomBar(onNew: () -> Unit, onOpenScenes: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SafinaField)
                .border(1.dp, SafinaBorder, RoundedCornerShape(18.dp))
                .combinedClickable(onClick = onOpenScenes, onLongClick = onOpenScenes),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Scene studio", fontSize = 12.5.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier
                .weight(1.35f)
                .height(50.dp)
                .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = SafinaAccent, spotColor = SafinaAccent)
                .clip(RoundedCornerShape(18.dp))
                .background(SafinaInk)
                .combinedClickable(onClick = onNew, onLongClick = onNew),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PlusGlyph(SafinaOnInk, 16.dp)
            Spacer(Modifier.width(7.dp))
            Text("New dhikr", fontSize = 13.sp, color = SafinaOnInk, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PillButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) SafinaInk else SafinaField)
            .border(
                1.dp,
                if (primary) Color.Transparent else SafinaBorder,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            color = if (primary) SafinaOnInk else SafinaInk,
            fontWeight = FontWeight.Medium
        )
    }
}

/* ---------------- card list with drag to reorder ---------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableCardList(
    cards: List<Zikr>,
    counts: Map<String, Int>,
    uploaded: List<HtmlSceneMeta>,
    thumbVersion: Int,
    draggable: Boolean,
    quranCard: (@Composable () -> Unit)? = null,
    onOpen: (Zikr) -> Unit,
    onEdit: (Zikr) -> Unit,
    onDelete: (Zikr) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val latestCards = rememberUpdatedState(cards)
    val density = LocalDensity.current
    val extentPx = with(density) { (CardHeight + CardGap).toPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp, end = 24.dp, top = 2.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        quranCard?.let { card ->
            item(key = "quran-card") {
                Box(Modifier.padding(top = 2.dp, bottom = 2.dp)) { card() }
            }
        }
        itemsIndexed(cards, key = { _, z -> z.id }) { index, z ->
            val isDragged = draggingId == z.id
            val count = counts[z.id] ?: z.count
            ZikrCard(
                zikr = z,
                count = count,
                sceneName = sceneTitle(z.sceneId, uploaded),
                thumbVersion = thumbVersion,
                isDragged = isDragged,
                onOpen = { onOpen(z) },
                onEdit = { onEdit(z) },
                onDelete = { onDelete(z) },                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffset else 0f
                        scaleX = if (isDragged) 1.015f else 1f
                        scaleY = if (isDragged) 1.015f else 1f
                    }
                    .then(if (isDragged) Modifier else Modifier.animateItem())
                    .then(
                        if (!draggable) Modifier else Modifier.pointerInput(z.id, draggable) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = z.id
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val current = latestCards.value
                                    val from = current.indexOfFirst { it.id == draggingId }
                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                    val shift = (dragOffset / extentPx).roundToInt()
                                    if (shift != 0) {
                                        val to = (from + shift).coerceIn(0, current.lastIndex)
                                        if (to != from) {
                                            onReorder(from, to)
                                            dragOffset -= (to - from) * extentPx
                                        }
                                    }
                                }
                            )
                        }
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZikrCard(
    zikr: Zikr,
    count: Int,
    sceneName: String,
    thumbVersion: Int,
    isDragged: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val progress = (count.toFloat() / zikr.target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val lift by animateFloatAsState(if (isDragged) 12f else 0f, label = "lift")

    Box(
        modifier
            .fillMaxWidth()
            .height(CardHeight)
            .shadow(
                lift.dp, RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF121712), spotColor = Color(0xFF121712)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SafinaSurface)
            .border(1.dp, SafinaBorder, RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onOpen, onLongClick = {})
    ) {
        SceneThumb(
            sceneId = zikr.sceneId,
            version = thumbVersion,
            modifier = Modifier.fillMaxSize(),
            corner = 24.dp
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0F0B).copy(alpha = 0.32f),
                        Color(0xFF0A0F0B).copy(alpha = 0.50f),
                        Color(0xFF0A0F0B).copy(alpha = 0.66f)
                    )
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                sceneName.uppercase(),
                fontSize = 8.5.sp,
                color = Color(0xFFFDFBF3).copy(alpha = 0.72f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(9.dp))
            Text(
                zikr.arabic,
                fontSize = 27.sp,
                lineHeight = 36.sp,
                color = Color(0xFFFDFBF3),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        blurRadius = 10f,
                        offset = Offset(0f, 1.5f)
                    )
                )
            )
            Spacer(Modifier.height(3.dp))
            Text(
                zikr.translation,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = Color(0xFFFDFBF3).copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniProgress(
                    progress = progress,
                    tint = Color(0xFFFDFBF3).copy(alpha = 0.9f),
                    modifier = Modifier.width(96.dp).height(3.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    if (progress >= 1f) "$count" else "$count / ${zikr.target}",
                    fontSize = 10.sp,
                    color = Color(0xFFFDFBF3).copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B100C).copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
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
                    text = { Text("Edit", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFB06A5C)) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}
