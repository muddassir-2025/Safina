package com.muddassir.safina

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ------------------------------------------------------------------ */
/* Shared bits                                                        */
/* ------------------------------------------------------------------ */

private fun sceneLocalId(remoteId: String) = "pub_$remoteId"

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Safina", text))
}

private fun shortDate(epoch: Long): String =
    if (epoch <= 0) "" else java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(epoch))

/** Full-width bottom sheet on a dimmed backdrop. */
@Composable
private fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B100C).copy(alpha = 0.38f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(SafinaSurface)
                .navigationBarsPadding()
                .clickable(enabled = false) {}
                .padding(horizontal = 22.dp, vertical = 20.dp),
            content = content
        )
    }
}

/** Full-screen reading surface, used by the "how to create" screen. */
@Composable
private fun FullScreenSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AppBackdrop(0f) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SafinaField)
                        .border(1.dp, SafinaBorder, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { CloseGlyph(SafinaInk, 14.dp) }
                Spacer(Modifier.width(14.dp))
                Text(title, fontSize = 18.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                content = content
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetTitle(text: String, subtitle: String? = null) {
    Text(text, fontSize = 18.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = SafinaMuted)
    }
}

@Composable
private fun Field(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (singleLine) 46.dp else 110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SafinaField)
            .border(1.dp, SafinaBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = if (singleLine) 13.dp else 10.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = SafinaMuted.copy(alpha = 0.7f))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            cursorBrush = SolidColor(SafinaAccent),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SafinaInk),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) SafinaInk else SafinaMuted.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, color = SafinaOnInk, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(SafinaField)
            .border(1.dp, SafinaBorder, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.5.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
    }
}

/* ------------------------------------------------------------------ */
/* Upload                                                             */
/* ------------------------------------------------------------------ */

@Composable
internal fun UploadSheet(
    initialTitle: String,
    allowPublic: Boolean,
    adminMode: Boolean,
    adminOnlyPublic: Boolean,
    signedIn: Boolean,
    channelName: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSave: (UploadTarget, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle.take(40)) }
    var target by remember {
        mutableStateOf(if (adminOnlyPublic) UploadTarget.AdminPublic else UploadTarget.Private)
    }

    val options = buildList {
        if (!adminOnlyPublic) add(UploadTarget.Private to "Private")
        if (allowPublic) add(UploadTarget.Public to "Public")
        if (adminMode) {
            add(UploadTarget.AdminDefault to "Add as default")
            add(UploadTarget.AdminPublic to "Add as public")
        }
    }

    val needsSignIn = target == UploadTarget.Public && !signedIn
    val ready = title.isNotBlank() && !needsSignIn

    Sheet(onDismiss) {
        SheetTitle("Save scene", "Give it a name. Private scenes never leave your device.")
        Spacer(Modifier.height(14.dp))
        Field(title, "Scene name (max 40)", { title = it.take(40) })
        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val active = target == value
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) SafinaField else Color.Transparent)
                        .border(1.dp, if (active) SafinaAccent.copy(alpha = 0.6f) else SafinaBorder, RoundedCornerShape(14.dp))
                        .clickable { target = value }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontSize = 13.sp, color = SafinaInk, modifier = Modifier.weight(1f))
                    if (active) Text("●", fontSize = 12.sp, color = SafinaAccent)
                }
            }
        }

        if (target == UploadTarget.Public) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (signedIn) "Published as ${channelName ?: "your channel"}"
                else "Sign in with Google to publish",
                fontSize = 11.5.sp,
                color = SafinaMuted
            )
            if (needsSignIn) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Sign in with Google", onSignIn)
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (needsSignIn) "Sign in to publish" else "Save", enabled = ready || needsSignIn) {
            if (needsSignIn) onSignIn() else onSave(target, title.trim())
        }
    }
}

/* ------------------------------------------------------------------ */
/* How to create a scene                                              */
/* ------------------------------------------------------------------ */

private const val SCENE_AI_PROMPT = """You are writing one self-contained HTML file that will be rendered inside a mobile app's WebView as a full-screen animated background scene.

Rules:
- Return ONE complete HTML document, from <!doctype html> to </html>. No markdown, no explanation.
- All CSS and JavaScript must be inline. No external files, fonts, images or CDNs.
- No network requests at all: no fetch(), XMLHttpRequest, WebSocket, EventSource, or external src/href URLs.
- If you need imagery, draw it with CSS, canvas or inline SVG, or inline it as a base64 data: URI.
- Fill 100% of the viewport with no scrollbars. Be responsive to any aspect ratio; handle resize.
- Animate smoothly at 60fps on a mid-range phone. Prefer transform/opacity, avoid layout thrash, cap devicePixelRatio at 2, and keep the total under 1 MB.
- Keep it calm and background-friendly: no text the user must read, no controls, nothing that steals taps.
- Honour prefers-reduced-motion by slowing or stopping animation.

Optional host hooks (use them if they help; all are guarded):
- window.ZikrHost.ready() tells the app the scene has painted.
- window.ZikrHost.setTextTheme('dark' or 'light') reports whether the scene is dark, so the app can match its text colour.
- window.ZikrHost.milestone(0.0 - 1.0) reports dhikr progress if you want the scene to react to it.
- The app calls window.zikrScene.init(progress), setProgress(progress), tap(x, y) and setPaused(bool) if you define that object.

My scene: [DESCRIBE YOUR SCENE HERE]"""

@Composable
fun SceneInfoScreen(
    onDismiss: () -> Unit,
    onDownloadExample: () -> Unit,
    onCopied: () -> Unit
) {
    val context = LocalContext.current
    FullScreenSheet("How to create a custom scene", onDismiss) {
        SectionText(
            "A scene is one self-contained .html file. Everything it needs — styling, animation, " +
                "imagery — lives inside that single file."
        )

        InfoHeading("1. One file, no network")
        InfoBullets(
            listOf(
                "Inline all CSS and JavaScript. External fonts, scripts and stylesheets are blocked.",
                "Make no network requests: no fetch(), XMLHttpRequest, WebSocket or EventSource.",
                "No external images. Draw with CSS, canvas or SVG, or inline as base64 data: URIs.",
                "Maximum size is 1 MB."
            )
        )

        InfoHeading("2. Fill the viewport, stay responsive")
        InfoBullets(
            listOf(
                "Cover 100% of the screen with no scrollbars: html, body { margin:0; height:100% }.",
                "Use relative units and handle resize/orientation so it looks right on any phone.",
                "Keep it calm — it sits behind the app's text."
            )
        )

        InfoHeading("3. Performance on mobile")
        InfoBullets(
            listOf(
                "Animate transform and opacity; avoid animating layout properties.",
                "Cap devicePixelRatio at 2 and keep canvas resolution modest.",
                "Prefer requestAnimationFrame over many timers, and pause when the page is hidden."
            )
        )

        InfoHeading("4. Hooks your scene may use")
        InfoBullets(
            listOf(
                "window.ZikrHost.ready() — the scene has painted.",
                "window.ZikrHost.setTextTheme('dark' | 'light') — match app text colour.",
                "window.ZikrHost.milestone(0.0–1.0) — report dhikr progress.",
                "The app calls window.zikrScene.init(p), setProgress(p), tap(x, y), setPaused(b) if defined."
            )
        )
        Text(
            "Any other native bridge is unavailable, file access is off, and every network request " +
                "is blocked by the WebView.",
            fontSize = 11.5.sp,
            color = SafinaMuted,
            modifier = Modifier.padding(top = 6.dp)
        )

        InfoHeading("5. Upload it")
        InfoBullets(
            listOf(
                "Open Scene studio → Default → + Upload, or Private for a private scene.",
                "Pick your .html file (under 1 MB). Invalid or oversized files are rejected.",
                "Name it and save. Choose Public to publish it for everyone."
            )
        )

        Spacer(Modifier.height(20.dp))
        PrimaryButton("Copy AI prompt") {
            copyText(context, SCENE_AI_PROMPT)
            onCopied()
        }
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Download example file", onDownloadExample)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun InfoHeading(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, fontSize = 13.sp, color = SafinaInk, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SectionText(text: String) {
    Text(text, fontSize = 12.5.sp, color = SafinaMuted, lineHeight = 19.sp)
}

@Composable
private fun InfoBullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.forEach { line ->
            Row {
                Text("·", fontSize = 12.5.sp, color = SafinaAccent, modifier = Modifier.padding(end = 8.dp))
                Text(line, fontSize = 12.sp, color = SafinaMuted, lineHeight = 18.sp)
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Channel name                                                       */
/* ------------------------------------------------------------------ */

@Composable
fun ChannelSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var available by remember { mutableStateOf<Boolean?>(null) }
    var checking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(name) {
        val token = CloudSession.token
        val candidate = name.trim()
        available = null
        error = null
        if (token == null || candidate.length < 3) return@LaunchedEffect
        checking = true
        delay(350)
        available = runCatching { Api.checkChannel(candidate, token) }.getOrNull()
        checking = false
    }

    val valid = Regex("^[A-Za-z0-9_]{3,20}$").matches(name.trim())
    val canSave = valid && available == true && !saving

    Sheet(onDismiss) {
        SheetTitle("Choose a channel name", "This is the author name on your public scenes.")
        Spacer(Modifier.height(14.dp))
        Field(name, "3–20 letters, numbers or _", { name = it.take(20) })
        Spacer(Modifier.height(8.dp))
        when {
            checking -> Text("Checking availability…", fontSize = 11.5.sp, color = SafinaMuted)
            error != null -> Text(error!!, fontSize = 11.5.sp, color = Color(0xFFB06A5C))
            !valid && name.isNotEmpty() -> Text(
                "3–20 characters: letters, numbers or underscore",
                fontSize = 11.5.sp,
                color = Color(0xFFB06A5C)
            )
            available == true -> Text("Available", fontSize = 11.5.sp, color = SafinaAccent)
            available == false -> Text("That name is taken", fontSize = 11.5.sp, color = Color(0xFFB06A5C))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (saving) "Saving…" else "Save channel name", enabled = canSave) {
            val token = CloudSession.token ?: return@PrimaryButton
            saving = true
            scope.launch {
                try {
                    Api.setChannel(name.trim(), token)
                    CloudSession.refreshProfile()
                    saving = false
                    onSaved()
                } catch (e: Exception) {
                    saving = false
                    error = e.message ?: "Couldn't save that name"
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Admin login + reports                                              */
/* ------------------------------------------------------------------ */

@Composable
fun AdminLoginSheet(onDismiss: () -> Unit, onSignedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Sheet(onDismiss) {
        SheetTitle("Admin sign in", "Separate from your Safina account.")
        Spacer(Modifier.height(14.dp))
        Field(email, "Admin email", { email = it })
        Spacer(Modifier.height(10.dp))
        Field(password, "Password", { password = it })
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, fontSize = 11.5.sp, color = Color(0xFFB06A5C))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (busy) "Signing in…" else "Sign in", enabled = !busy && email.isNotBlank() && password.isNotEmpty()) {
            busy = true
            error = null
            scope.launch {
                try {
                    val (token, expiresAt) = Api.adminLogin(email.trim(), password)
                    AdminSession.setToken(token, expiresAt, email.trim())
                    password = ""
                    busy = false
                    onSignedIn()
                } catch (_: Exception) {
                    busy = false
                    error = "Invalid credentials"
                }
            }
        }
    }
}

@Composable
fun AdminReportsSheet(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ReportedScene>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        val token = AdminSession.token
        if (token == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        val result = runCatching { Api.adminReports(token) }
        items = result.getOrDefault(emptyList())
        failed = result.isFailure
        loading = false
    }

    FullScreenSheet("Reports", onDismiss) {
        if (loading) {
            Text("Loading reports…", fontSize = 12.sp, color = SafinaMuted)
        } else if (failed) {
            Text("Couldn't load reports.", fontSize = 12.sp, color = SafinaMuted)
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Retry") { reload++ }
        } else if (items.isEmpty()) {
            Text("No reports yet.", fontSize = 12.sp, color = SafinaMuted)
        } else {
            items.forEach { report ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SafinaField)
                        .border(1.dp, SafinaBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(report.title, fontSize = 13.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
                    Text(
                        "by ${report.author.ifBlank { "unknown" }} · ${report.reportCount} report(s)",
                        fontSize = 10.5.sp,
                        color = SafinaMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(report.reasons, fontSize = 11.sp, color = SafinaMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallAction("Dismiss") {
                            val token = AdminSession.token ?: return@SmallAction
                            scope.launch {
                                runCatching { Api.adminDismissReports(token, report.sceneId) }
                                reload++
                            }
                        }
                        SmallAction("Remove scene", danger = true) {
                            val token = AdminSession.token ?: return@SmallAction
                            scope.launch {
                                runCatching { Api.adminDeletePublic(token, report.sceneId, "Removed by moderator") }
                                reload++
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SmallAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(SafinaSurface)
            .border(1.dp, SafinaBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = if (danger) Color(0xFFB06A5C) else SafinaInk,
            fontWeight = FontWeight.Medium
        )
    }
}

/* ------------------------------------------------------------------ */
/* Report                                                             */
/* ------------------------------------------------------------------ */

@Composable
fun ReportDialog(sceneTitle: String, onDismiss: () -> Unit, onReport: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    val presets = listOf("Inappropriate content", "Broken or blank", "Network requests", "Spam")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafinaSurface,
        title = { Text("Report scene", color = SafinaInk) },
        text = {
            Column {
                Text("Why are you reporting “$sceneTitle”?", fontSize = 12.sp, color = SafinaMuted)
                Spacer(Modifier.height(10.dp))
                presets.forEach { preset ->
                    Text(
                        preset,
                        fontSize = 12.sp,
                        color = SafinaInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { reason = preset }
                            .padding(vertical = 7.dp, horizontal = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Field(reason, "Describe the problem", { reason = it.take(300) }, singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (reason.isNotBlank()) onReport(reason.trim()) }) {
                Text("Report", color = Color(0xFFB06A5C))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* ------------------------------------------------------------------ */
/* Move to collection                                                 */
/* ------------------------------------------------------------------ */

@Composable
fun MoveToCollectionDialog(saved: SavedScene, onDismiss: () -> Unit, onMove: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var collections by remember { mutableStateOf<List<CollectionInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val token = CloudSession.token
        if (token != null) collections = runCatching { Api.collections(token) }.getOrDefault(emptyList())
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafinaSurface,
        title = { Text("Move to collection", color = SafinaInk) },
        text = {
            Column {
                Text(saved.scene.title, fontSize = 12.sp, color = SafinaMuted, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                if (loading) {
                    Text("Loading…", fontSize = 12.sp, color = SafinaMuted)
                } else {
                    CollectionRow("Uncategorized", saved.collectionId == null) { onMove(null) }
                    collections.forEach { collection ->
                        CollectionRow(collection.name, saved.collectionId == collection.id) {
                            onMove(collection.id)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("New collection") {
                        val token = CloudSession.token ?: return@SecondaryButton
                        scope.launch {
                            val name = "Collection ${collections.size + 1}"
                            val created = runCatching { Api.createCollection(name, token) }.getOrNull()
                            if (created?.id != null) {
                                collections = collections + created
                                onMove(created.id)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun CollectionRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 12.5.sp, color = SafinaInk, modifier = Modifier.weight(1f))
        if (selected) Text("✓", fontSize = 12.sp, color = SafinaAccent)
    }
}

/* ------------------------------------------------------------------ */
/* Public tab                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun PublicSceneCard(
    scene: PublicScene,
    thumbVersion: Int,
    signedIn: Boolean,
    admin: Boolean,
    saved: Boolean,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onReport: () -> Unit,
    onAdminDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SafinaSurface)
            .border(1.dp, SafinaBorder, RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onPreview, onLongClick = onPreview)
    ) {
        SceneThumb(
            sceneId = "$HTML_SCENE_PREFIX${sceneLocalId(scene.id)}",
            version = thumbVersion,
            modifier = Modifier.fillMaxSize(),
            corner = 20.dp
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B100C).copy(alpha = 0.10f), Color(0xFF0B100C).copy(alpha = 0.66f))
                )
            )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                scene.title,
                fontSize = 17.sp,
                color = Color(0xFFFDFBF3),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${scene.author.ifBlank { "Unknown" }} · ${scene.savedCount} saves",
                fontSize = 9.5.sp,
                color = Color(0xFFFDFBF3).copy(alpha = 0.75f)
            )
        }
        Row(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF0B100C).copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(11.dp))
                    .clickable(enabled = signedIn) { if (saved) onUnsave() else onSave() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (saved) "Saved" else "Save",
                    fontSize = 10.5.sp,
                    color = Color(0xFFFDFBF3),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B100C).copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center
            ) { MoreGlyph(Color(0xFFFDFBF3), 16.dp) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SafinaSurface) {
                DropdownMenuItem(
                    text = { Text("Preview", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onPreview() }
                )
                DropdownMenuItem(
                    text = { Text("Report", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onReport() }
                )
                if (admin) {
                    DropdownMenuItem(
                        text = { Text("Admin delete", fontSize = 13.sp, color = Color(0xFFB06A5C)) },
                        onClick = { menuOpen = false; onAdminDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 8.5.sp, color = SafinaMuted, letterSpacing = 2.5.sp, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 10.sp, color = SafinaMuted)
    }
}

@Composable
fun PublicTab(
    modifier: Modifier = Modifier,
    thumbVersion: Int,
    refreshKey: Int,
    query: String,
    savedLocal: List<HtmlSceneMeta>,
    signedIn: Boolean,
    admin: Boolean,
    onRequestSignIn: () -> Unit,
    onPreview: (PublicScene) -> Unit,
    onSave: (PublicScene) -> Unit,
    onUnsave: (PublicScene) -> Unit,
    onMove: (SavedScene) -> Unit,
    onReport: (PublicScene) -> Unit,
    onAdminDelete: (PublicScene) -> Unit,
    onAdminReports: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var saves by remember { mutableStateOf<List<SavedScene>>(emptyList()) }
    var collections by remember { mutableStateOf<List<CollectionInfo>>(emptyList()) }
    var filter by remember { mutableStateOf("all") }
    var explore by remember { mutableStateOf<List<PublicScene>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var sortTop by remember { mutableStateOf(false) }
    var loadingSaved by remember { mutableStateOf(false) }
    var loadingExplore by remember { mutableStateOf(true) }
    var exploreFailed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }

    val token = CloudSession.token

    LaunchedEffect(refreshKey, signedIn, filter) {
        if (!signedIn) {
            saves = emptyList()
            collections = emptyList()
            return@LaunchedEffect
        }
        val auth = CloudSession.token ?: return@LaunchedEffect
        loadingSaved = true
        saves = runCatching { Api.mySaves(auth) }.getOrDefault(emptyList())
        collections = runCatching { Api.collections(auth) }.getOrDefault(emptyList())
        loadingSaved = false
    }

    LaunchedEffect(refreshKey, sortTop, query, reload) {
        loadingExplore = true
        exploreFailed = false
        page = 1
        val result = runCatching {
            Api.listPublic(
                token = CloudSession.token,
                page = 1,
                sort = if (sortTop) "top" else "new",
                query = query.trim(),
                excludeSaved = false
            )
        }
        if (result.isSuccess) {
            val (items, more) = result.getOrThrow()
            explore = items
            hasMore = more
        } else {
            exploreFailed = true
            explore = emptyList()
            hasMore = false
        }
        loadingExplore = false
    }

    fun loadMore() {
        val next = page + 1
        scope.launch {
            val result = runCatching {
                Api.listPublic(CloudSession.token, next, if (sortTop) "top" else "new", query.trim(), false)
            }
            result.getOrNull()?.let { (items, more) ->
                explore = explore + items
                hasMore = more
                page = next
            }
        }
    }

    val visibleSaves = when (filter) {
        "all" -> saves
        "uncat" -> saves.filter { it.collectionId == null }
        else -> saves.filter { it.collectionId == filter }
    }
    val lowerQuery = query.trim().lowercase()
    val exploreVisible = if (lowerQuery.isEmpty()) explore
    else explore.filter { it.title.lowercase().contains(lowerQuery) || it.author.lowercase().contains(lowerQuery) }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionLabel(if (signedIn) "SAVED" else "SAVED · SIGN IN TO SAVE")
        }

        if (!signedIn) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SafinaField)
                        .border(1.dp, SafinaBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text("Browse freely. Sign in to save scenes.", fontSize = 12.sp, color = SafinaMuted)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("Sign in with Google", onRequestSignIn)
                }
            }
        } else {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip("All", filter == "all") { filter = "all" }
                    FilterChip("Uncategorized", filter == "uncat") { filter = "uncat" }
                    collections.take(2).forEach { collection ->
                        FilterChip(collection.name, filter == collection.id) { filter = collection.id ?: "all" }
                    }
                }
            }
            if (loadingSaved) {
                item { Text("Loading saved scenes…", fontSize = 11.5.sp, color = SafinaMuted) }
            } else if (visibleSaves.isEmpty()) {
                item { Text("Nothing saved here yet.", fontSize = 11.5.sp, color = SafinaMuted) }
            } else {
                items(visibleSaves, key = { "s-${it.scene.id}" }) { saved ->
                    if (saved.removed) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SafinaField)
                                .border(1.dp, SafinaBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                                .combinedClickable(onClick = { onMove(saved) }, onLongClick = { onMove(saved) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "This scene was removed",
                                fontSize = 12.sp,
                                color = SafinaMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text("Move", fontSize = 11.sp, color = SafinaAccent)
                        }
                    } else {
                        PublicSceneCard(
                            scene = saved.scene,
                            thumbVersion = thumbVersion,
                            signedIn = signedIn,
                            admin = admin,
                            saved = true,
                            onPreview = { onPreview(saved.scene) },
                            onSave = { onSave(saved.scene) },
                            onUnsave = { onUnsave(saved.scene) },
                            onReport = { onReport(saved.scene) },
                            onAdminDelete = { onAdminDelete(saved.scene) }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            SmallAction("Move to…") { onMove(saved) }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("EXPLORE", if (admin) "ADMIN" else null)
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip("Most saved", sortTop) { sortTop = true }
                FilterChip("Newest", !sortTop) { sortTop = false }
                if (admin) {
                    Spacer(Modifier.width(2.dp))
                    SmallAction("Reports") { onAdminReports() }
                }
            }
        }

        if (loadingExplore) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = SafinaAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
            }
        } else if (exploreFailed) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SafinaField)
                        .padding(16.dp)
                ) {
                    Text("Couldn't reach the community scenes.", fontSize = 12.sp, color = SafinaMuted)
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Retry") { reload++ }
                }
            }
        } else if (exploreVisible.isEmpty()) {
            item { Text("No public scenes match your search.", fontSize = 11.5.sp, color = SafinaMuted) }
        } else {
            items(exploreVisible, key = { "e-${it.id}" }) { scene ->
                PublicSceneCard(
                    scene = scene,
                    thumbVersion = thumbVersion,
                    signedIn = signedIn,
                    admin = admin,
                    saved = scene.isSaved || saves.any { it.scene.id == scene.id },
                    onPreview = { onPreview(scene) },
                    onSave = {
                        if (!signedIn) onRequestSignIn() else onSave(scene)
                    },
                    onUnsave = { onUnsave(scene) },
                    onReport = {
                        if (!signedIn) onRequestSignIn() else onReport(scene)
                    },
                    onAdminDelete = { onAdminDelete(scene) }
                )
            }
            if (hasMore) {
                item {
                    SecondaryButton("Load more") { loadMore() }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) SafinaInk else SafinaField)
            .border(1.dp, if (selected) Color.Transparent else SafinaBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 10.5.sp,
            color = if (selected) SafinaOnInk else SafinaInk,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}
