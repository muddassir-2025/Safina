package com.muddassir.safina

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val CreateInk = Color(0xFF4A4436)

@Composable
fun CreatePage(
    time: Float,
    scenes: List<HtmlSceneMeta>,
    onUpload: () -> Unit,
    onCopyInstructions: () -> Unit,
    onSaveExample: () -> Unit,
    onReplace: (HtmlSceneMeta) -> Unit,
    onRename: (HtmlSceneMeta) -> Unit,
    onDelete: (HtmlSceneMeta) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val shift = 0.5f + 0.5f * sin(time * TWO_PI * 0.5f)
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        lerp(Color(0xFFE8F1E6), Color(0xFFE6EEF7), shift),
                        lerp(Color(0xFFF6F0E2), Color(0xFFEDEAF4), 1f - shift)
                    )
                )
            )
            repeat(16) { i ->
                val a = (i * 1.7f + time * 2f) % 1f
                val x = ((i * 0.113f) % 1f) * size.width + sin((time + i) * TWO_PI) * size.width * 0.02f
                val y = size.height * (1f - a * 0.8f)
                drawCircle(
                    Color.White.copy(alpha = 0.35f * (1f - a)),
                    size.width * (0.002f + (i % 3) * 0.0015f),
                    Offset(x, y)
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 26.dp, end = 26.dp, top = 56.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create your own scene", fontSize = 26.sp, color = CreateInk, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload a single .html file made with the instructions below. " +
                    "It becomes its own page, driven by your taps.",
                fontSize = 13.sp, color = CreateInk.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Text("Upload scene")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCopyInstructions, modifier = Modifier.fillMaxWidth()) {
                Text("Copy instructions for your AI")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSaveExample, modifier = Modifier.fillMaxWidth()) {
                Text("Save example file")
            }

            Spacer(Modifier.height(28.dp))
            if (scenes.isEmpty()) {
                Text(
                    "No uploaded scenes yet.",
                    fontSize = 12.sp, color = CreateInk.copy(alpha = 0.55f)
                )
            } else {
                Text(
                    "Your scenes", fontSize = 13.sp,
                    color = CreateInk.copy(alpha = 0.7f), fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                scenes.forEach { meta ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(meta.name, fontSize = 14.sp, color = CreateInk, maxLines = 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onReplace(meta) }) { Text("Replace", fontSize = 12.sp) }
                            TextButton(onClick = { onRename(meta) }) { Text("Rename", fontSize = 12.sp) }
                            TextButton(onClick = { onDelete(meta) }) {
                                Text("Delete", fontSize = 12.sp, color = Color(0xFF9A4A3E))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** Full-screen preview with a native slider and a tap test, before the scene is added. */
@Composable
fun HtmlPreviewOverlay(
    html: String,
    confirmLabel: String,
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
                onError = { failed = true }
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
                Text("Preview", fontSize = 12.sp, color = CreateInk.copy(alpha = 0.6f))
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
                Button(onClick = onConfirm, enabled = ready && !failed) { Text(confirmLabel) }
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
        text = { Text("\"$name\" and its saved count will be removed.") }
    )
}
