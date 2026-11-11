package com.muddassir.safina

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

/** Bottom sheet for creating or editing one dhikr card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZikrEditorScreen(
    existing: Zikr?,
    cards: List<Zikr>,
    uploaded: List<HtmlSceneMeta>,
    thumbVersion: Int,
    onDismiss: () -> Unit,
    onSave: (Zikr) -> Unit,
    onDelete: (Zikr) -> Unit
) {
    var arabic by remember { mutableStateOf(existing?.arabic.orEmpty()) }
    var translation by remember { mutableStateOf(existing?.translation.orEmpty()) }
    var targetText by remember { mutableStateOf((existing?.target ?: 100).toString()) }
    var sceneId by remember { mutableStateOf(existing?.sceneId ?: Scenes.all.first().id) }
    var confirmDelete by remember { mutableStateOf(false) }

    val target = targetText.toIntOrNull()
    val valid = arabic.isNotBlank() && target != null && target in 1..100_000

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F0B).copy(alpha = 0.42f))
            .combinedClickable(onClick = onDismiss, onLongClick = {}),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SafinaSurface)
                .combinedClickable(onClick = {}, onLongClick = {})
                .imePadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 10.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(SafinaMuted.copy(alpha = 0.20f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (existing == null) "New dhikr" else "Edit dhikr",
                            fontSize = 30.sp,
                            color = SafinaInk,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 34.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "The words, the count, and the scene behind them.",
                            fontSize = 11.5.sp,
                            color = SafinaMuted.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SafinaField)
                            .border(1.dp, SafinaBorder, CircleShape)
                            .combinedClickable(onClick = onDismiss, onLongClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) { CloseGlyph(SafinaMuted, 13.dp) }
                }
                Spacer(Modifier.height(26.dp))

                EditorField(
                    label = "Arabic",
                    value = arabic,
                    onChange = { arabic = it.take(240) },
                    placeholder = "أَسْتَغْفِرُ اللَّهَ",
                    fontSize = 25,
                    lineHeight = 38,
                    minHeight = 104.dp
                )
                Spacer(Modifier.height(18.dp))

                EditorField(
                    label = "English",
                    value = translation,
                    onChange = { translation = it.take(160) },
                    placeholder = "Astaghfirullah · I seek Allah's forgiveness",
                    fontSize = 13,
                    lineHeight = 20,
                    minHeight = 64.dp
                )
                Spacer(Modifier.height(24.dp))

                EditorLabel("Target")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SafinaField)
                        .border(1.dp, SafinaInk.copy(alpha = 0.11f), RoundedCornerShape(20.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepButton("\u2212") {
                        val current = target ?: 0
                        targetText = (current - 1).coerceAtLeast(1).toString()
                    }
                    Box(
                        Modifier.weight(1f).height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicTextField(
                                value = targetText,
                                onValueChange = { new ->
                                    targetText = new.filter { it.isDigit() }.take(6)
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(SafinaAccent),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 26.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = if (target in 1..100_000) SafinaInk else Color(0xFFB06A5C),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "TIMES",
                                fontSize = 7.5.sp,
                                color = SafinaMuted.copy(alpha = 0.8f),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    StepButton("+") {
                        val current = target ?: 0
                        targetText = (current + 1).coerceAtMost(100_000).toString()
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(33, 99, 100, 1000).forEach { preset ->
                        val selected = target == preset
                        Box(
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (selected) SafinaInk else SafinaField)
                                .border(
                                    1.dp,
                                    if (selected) Color.Transparent else SafinaInk.copy(alpha = 0.11f),
                                    RoundedCornerShape(13.dp)
                                )
                                .combinedClickable(
                                    onClick = { targetText = preset.toString() },
                                    onLongClick = { targetText = preset.toString() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                preset.toString(),
                                fontSize = 12.sp,
                                color = if (selected) SafinaOnInk else SafinaInk,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                EditorLabel("Scene")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    items(Scenes.all, key = { it.id }) { scene ->
                        SceneChoice(
                            sceneId = scene.id,
                            title = sceneTitle(scene.id, uploaded),
                            selected = sceneId == scene.id,
                            thumbVersion = thumbVersion,
                            onClick = { sceneId = scene.id }
                        )
                    }
                    items(uploaded, key = { "html:${it.id}" }) { meta ->
                        val id = "$HTML_SCENE_PREFIX${meta.id}"
                        SceneChoice(
                            sceneId = id,
                            title = meta.name,
                            selected = sceneId == id,
                            thumbVersion = thumbVersion,
                            onClick = { sceneId = id }
                        )
                    }
                }
                Spacer(Modifier.height(26.dp))
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SafinaBorder.copy(alpha = 0.7f))
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(SafinaSurface)
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (existing != null) {
                    Text(
                        "Delete",
                        fontSize = 12.sp,
                        color = Color(0xFFB06A5C),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { confirmDelete = true },
                                onLongClick = { confirmDelete = true }
                            )
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Cancel",
                    fontSize = 12.5.sp,
                    color = SafinaMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(onClick = onDismiss, onLongClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (valid) SafinaInk else SafinaField)
                        .border(
                            1.dp,
                            if (valid) Color.Transparent else SafinaBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .combinedClickable(
                            onClick = {
                                if (!valid) return@combinedClickable
                                val id = existing?.id
                                    ?: "card_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                                onSave(
                                    Zikr(
                                        id = id,
                                        arabic = arabic.trim(),
                                        translation = translation.trim(),
                                        target = target!!,
                                        sceneId = sceneId,
                                        count = existing?.let { z ->
                                            cards.firstOrNull { it.id == z.id }?.count
                                        } ?: 0
                                    )
                                )
                            },
                            onLongClick = {}
                        )
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save",
                        fontSize = 13.sp,
                        color = if (valid) SafinaOnInk else SafinaMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(existing)
                }) { Text("Delete", color = Color(0xFFB06A5C)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            title = { Text("Delete this dhikr?") },
            text = { Text("It will be removed from your dashboard, along with its saved count.") }
        )
    }
}

@Composable
private fun EditorLabel(label: String) {
    Text(
        label.uppercase(),
        fontSize = 8.5.sp,
        color = SafinaMuted,
        letterSpacing = 2.2.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    fontSize: Int,
    lineHeight: Int,
    minHeight: Dp
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        EditorLabel(label)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(SafinaField)
                .border(
                    if (focused) 1.5.dp else 1.dp,
                    if (focused) SafinaAccent.copy(alpha = 0.75f)
                    else SafinaInk.copy(alpha = 0.11f),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp,
                    color = SafinaMuted.copy(alpha = 0.40f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp,
                    color = SafinaInk
                ),
                cursorBrush = SolidColor(SafinaAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
            )
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SafinaSurface.copy(alpha = 0.9f))
            .border(1.dp, SafinaInk.copy(alpha = 0.11f), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 19.sp, color = SafinaInk)
    }
}

@Composable
private fun SceneChoice(
    sceneId: String,
    title: String,
    selected: Boolean,
    thumbVersion: Int,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (selected) SafinaAccent else SafinaBorder,
        label = "sceneBorder"
    )
    Column(
        Modifier
            .width(102.dp)
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(19.dp))
                .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(19.dp))
                .padding(if (selected) 2.dp else 0.dp)
        ) {
            SceneThumb(
                sceneId = sceneId,
                version = thumbVersion,
                modifier = Modifier.fillMaxSize(),
                corner = if (selected) 16.dp else 18.dp
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDFBF3))
                        .border(1.dp, SafinaAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(SafinaAccent))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontSize = 9.5.sp,
            color = if (selected) SafinaInk else SafinaMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
