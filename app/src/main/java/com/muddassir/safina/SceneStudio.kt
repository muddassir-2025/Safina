package com.muddassir.safina

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class StudioTab { Default, Private, Public }

data class PreviewTarget(
    val id: String,
    val title: String,
    val author: String? = null,
    val savedCount: Int = 0,
    val publicScene: PublicScene? = null,
    val isDefault: Boolean = false
)

internal enum class UploadTarget { Private, Public, AdminDefault, AdminPublic }

/**
 * Owns the file pickers, sheets and cloud calls for Scene studio. Keeping this state
 * above the tab UI means a file and typed name survive a Google sign-in round trip.
 */
@Composable
fun SceneStudioHost(
    store: ZikrStore,
    settings: SettingsActions,
    thumbVersion: Int,
    time: Float,
    onBack: () -> Unit,
    onLibraryChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var libraryVersion by remember { mutableIntStateOf(0) }
    var publicRefresh by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(StudioTab.Default) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }
    var adminLoginOpen by remember { mutableStateOf(false) }
    var reportsOpen by remember { mutableStateOf(false) }

    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploadTarget by remember { mutableStateOf<UploadTarget?>(null) }
    var uploadTitle by remember { mutableStateOf("") }
    var pendingExisting by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var pendingPublish by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var channelSheetOpen by remember { mutableStateOf(false) }

    var renameTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var deleteTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var resetTarget by remember { mutableStateOf<HtmlSceneMeta?>(null) }
    var reportTarget by remember { mutableStateOf<PublicScene?>(null) }
    var moveTarget by remember { mutableStateOf<SavedScene?>(null) }
    var preview by remember { mutableStateOf<PreviewTarget?>(null) }
    var deleteDefaultTarget by remember { mutableStateOf<RemoteDefaultScene?>(null) }

    val scenes = remember(libraryVersion, thumbVersion) { store.customScenes() }
    val defaults = remember(scenes) {
        scenes.filter { it.isDefault && it.source == SCENE_SOURCE_DEFAULT }
            .sortedBy { meta -> BundledScenes.byKey(meta.defaultKey)?.sortOrder ?: 100 }
    }
    val privateScenes = remember(scenes) {
        scenes.filter { it.source == SCENE_SOURCE_PRIVATE }.sortedByDescending { it.addedAt }
    }
    val savedPublic = remember(scenes) { scenes.filter { it.source == SCENE_SOURCE_PUBLIC } }

    fun refresh() {
        libraryVersion++
        onLibraryChanged()
    }

    LaunchedEffect(CloudSession.token) {
        if (CloudSession.signedIn) CloudSession.refreshProfile()
    }

    /* ---------------- default scene sync (fails silently offline) ---------------- */
    LaunchedEffect(Unit) {
        try {
            val etag = store.defaultsEtag
            val result = Api.defaultScenes(etag)
            if (!result.notModified) {
                store.applyRemoteDefaults(result.items)
                store.defaultsEtag = result.version
                refresh()
            }
        } catch (_: Exception) {
            // Render free tier may be asleep. Bundled scenes remain untouched.
        }
    }

    /* ---------------- file picker ---------------- */
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            pendingBytes = null
            pendingExisting = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            when {
                bytes == null -> toast(context, "Couldn't read that file")
                bytes.isEmpty() -> toast(context, "That file is empty")
                bytes.size > MAX_HTML_BYTES -> toast(context, "Please choose a file under 1 MB")
                !looksLikeHtml(bytes) -> toast(context, "That doesn't look like an HTML file")
                else -> {
                    pendingBytes = bytes
                    if (pendingExisting == null) uploadTitle = nameFromHtml(bytes, uri)
                    uploadTarget = when {
                        pendingExisting != null -> UploadTarget.Private
                        else -> UploadTarget.Private
                    }
                }
            }
        }
    }

    val exampleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) scope.launch {
            val bytes = withContext(Dispatchers.IO) { store.readAsset(BundledScenes.EXAMPLE_ASSET) }
            if (bytes != null) {
                withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                }
                toast(context, "Example scene saved")
            } else {
                toast(context, "Example file is unavailable")
            }
        }
    }

    fun startUpload(target: UploadTarget = UploadTarget.Private) {
        pendingExisting = null
        pendingBytes = null
        uploadTitle = ""
        uploadTarget = target
        pickLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "*/*"))
    }

    fun replaceScene(meta: HtmlSceneMeta) {
        pendingExisting = meta
        pickLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "*/*"))
    }

    fun savePrivate(bytes: ByteArray, title: String) {
        val id = UUID.randomUUID().toString().replace("-", "").take(16)
        if (store.saveHtml(id, bytes)) {
            store.addCustomScene(
                HtmlSceneMeta(id, title.take(40), "$id.html", System.currentTimeMillis())
            )
            store.deleteThumbnail(id)
            refresh()
            toast(context, "Scene saved to Private")
        } else toast(context, "Couldn't save the scene")
    }

    fun saveReplacement(meta: HtmlSceneMeta, bytes: ByteArray) {
        if (store.saveHtml(meta.id, bytes)) {
            store.deleteThumbnail(meta.id)
            if (meta.isDefault) store.updateSceneMeta(meta.id) { it.copy(modified = true) }
            refresh()
            toast(context, "Scene updated")
        } else toast(context, "Couldn't save the scene")
    }

    fun publish(bytes: ByteArray, title: String) {
        val token = CloudSession.token
        if (token == null) {
            pendingPublish = bytes to title
            CloudSession.signIn(context)
            return
        }
        if (CloudSession.profile?.channelName.isNullOrBlank()) {
            pendingPublish = bytes to title
            channelSheetOpen = true
            return
        }
        scope.launch {
            try {
                val scene = Api.publish(title.take(40), bytes.decodeToString(), token)
                cachePublicScene(scene, token, store, socket = null)
                refresh()
                publicRefresh++
                tab = StudioTab.Public
                toast(context, "Published to Public")
            } catch (e: Exception) {
                toast(context, readableError(e))
            }
        }
    }

    fun publishExisting(meta: HtmlSceneMeta) {
        val bytes = store.htmlFile(meta.id).let { if (it.exists()) it.readBytes() else null }
        if (bytes == null) {
            toast(context, "This scene file is missing")
            return
        }
        publish(bytes, meta.name)
    }

    fun cachePublicScene(scene: PublicScene, token: String?, store: ZikrStore, socket: Any?) {
        scope.launch {
            try {
                val full = scene.html?.let { scene } ?: Api.getPublicScene(scene.id, token)
                val html = full.html ?: return@launch
                val localId = publicLocalId(scene.id)
                store.upsertSavedPublic(
                    HtmlSceneMeta(
                        id = localId,
                        name = scene.title,
                        fileName = "$localId.html",
                        addedAt = System.currentTimeMillis(),
                        source = SCENE_SOURCE_PUBLIC,
                        remoteId = scene.id,
                        author = scene.author,
                        savedCount = scene.savedCount
                    ),
                    html.toByteArray()
                )
                refresh()
            } catch (_: Exception) {
                toast(context, "Couldn't cache that scene")
            }
        }
    }

    fun savePublic(scene: PublicScene, collectionId: String?) {
        val token = CloudSession.token
        if (token == null) {
            CloudSession.signIn(context)
            toast(context, "Sign in with Google to save scenes")
            return
        }
        scope.launch {
            try {
                val count = Api.saveScene(scene.id, token, collectionId)
                cachePublicScene(scene.copy(savedCount = count), token, store, socket = null)
                publicRefresh++
                toast(context, "Saved")
            } catch (e: Exception) {
                toast(context, readableError(e))
            }
        }
    }

    fun unsavePublic(scene: PublicScene) {
        val token = CloudSession.token ?: return
        scope.launch {
            runCatching { Api.unsaveScene(scene.id, token) }
            store.removePublicScene(publicLocalId(scene.id))
            refresh()
            publicRefresh++
            toast(context, "Removed from Saved")
        }
    }

    BackHandler(enabled = preview != null || infoOpen || adminLoginOpen || reportsOpen) {
        when {
            preview != null -> preview = null
            infoOpen -> infoOpen = false
            adminLoginOpen -> adminLoginOpen = false
            reportsOpen -> reportsOpen = false
        }
    }

    SceneStudioScreen(
        time = time,
        settings = settings,
        tab = tab,
        onTab = { tab = it; query = "" },
        query = query,
        onQuery = { query = it },
        searching = searching,
        onSearch = {
            searching = !searching
            if (!searching) query = ""
        },
        defaults = defaults,
        privateScenes = privateScenes,
        savedPublic = savedPublic,
        thumbVersion = thumbVersion,
        publicRefresh = publicRefresh,
        onBack = onBack,
        onInfo = { infoOpen = true },
        onUpload = { startUpload(if (AdminSession.active) UploadTarget.AdminDefault else UploadTarget.Private) },
        onUploadPrivate = { startUpload(UploadTarget.Private) },
        onUploadAdminPublic = { startUpload(UploadTarget.AdminPublic) },
        onPreviewDefault = { meta ->
            preview = PreviewTarget(meta.id, meta.name, isDefault = true)
        },
        onPreviewPrivate = { meta ->
            preview = PreviewTarget(meta.id, meta.name)
        },
        onPreviewPublic = { scene ->
            preview = PreviewTarget(publicLocalId(scene.id), scene.title, scene.author, scene.savedCount, scene)
        },
        onReplace = { replaceScene(it) },
        onRename = { renameTarget = it },
        onDeletePrivate = { deleteTarget = it },
        onResetDefault = { resetTarget = it },
        onAdminDeleteDefault = { remote -> deleteDefaultTarget = remote },
        onReport = { reportTarget = it },
        onSavePublic = { scene -> savePublic(scene, null) },
        onUnsavePublic = { unsavePublic(it) },
        onMove = { moveTarget = it },
        onAdminDeletePublic = { scene ->
            scope.launch {
                val token = AdminSession.token ?: return@launch
                runCatching { Api.adminDeletePublic(token, scene.id, "Removed by moderator") }
                store.removePublicScene(publicLocalId(scene.id))
                refresh()
                publicRefresh++
                toast(context, "Public scene removed")
            }
        },
        onAdminReports = { reportsOpen = true },
        onAdminLogin = { adminLoginOpen = true },
        onPublishExisting = { publishExisting(it) },
        onRequestSignIn = { CloudSession.signIn(context) },
        signedIn = CloudSession.signedIn
    )

    /* ---------------- upload sheet ---------------- */
    val bytes = pendingBytes
    if (bytes != null && uploadTarget != null) {
        val existing = pendingExisting
        UploadSheet(
            initialTitle = if (existing != null) existing.name else uploadTitle,
            allowPublic = existing == null,
            adminMode = AdminSession.active,
            adminOnlyPublic = uploadTarget == UploadTarget.AdminPublic,
            signedIn = CloudSession.signedIn,
            channelName = CloudSession.profile?.channelName,
            onDismiss = {
                pendingBytes = null
                pendingExisting = null
                uploadTarget = null
            },
            onSignIn = { CloudSession.signIn(context) },
            onSave = { target, title ->
                val data = bytes
                when {
                    existing != null -> {
                        saveReplacement(existing, data)
                        pendingBytes = null
                        pendingExisting = null
                    }
                    target == UploadTarget.Private -> {
                        savePrivate(data, title)
                        pendingBytes = null
                        uploadTarget = null
                    }
                    target == UploadTarget.AdminDefault -> scope.launch {
                        val token = AdminSession.token ?: return@launch
                        runCatching {
                            Api.adminAddDefault(token, title, data.decodeToString(), 100 + defaults.size)
                        }
                        pendingBytes = null
                        uploadTarget = null
                        toast(context, "Added to Default scenes")
                    }
                    target == UploadTarget.AdminPublic -> scope.launch {
                        val token = AdminSession.token ?: return@launch
                        runCatching {
                            Api.adminAddPublic(token, title, data.decodeToString(), null)
                        }
                        pendingBytes = null
                        uploadTarget = null
                        publicRefresh++
                        toast(context, "Added to Public scenes")
                    }
                    else -> {
                        publish(data, title)
                        pendingBytes = null
                        uploadTarget = null
                    }
                }
            }
        )
    }

    /* ---------------- info ---------------- */
    if (infoOpen) {
        SceneInfoScreen(
            onDismiss = { infoOpen = false },
            onDownloadExample = {
                exampleLauncher.launch("safina-example-scene.html")
            },
            onCopied = { toast(context, "Prompt copied") }
        )
    }

    /* ---------------- admin ---------------- */
    if (adminLoginOpen) {
        AdminLoginSheet(
            onDismiss = { adminLoginOpen = false },
            onSignedIn = {
                adminLoginOpen = false
                toast(context, "Admin signed in")
            }
        )
    }
    if (reportsOpen) {
        AdminReportsSheet(onDismiss = { reportsOpen = false })
    }

    /* ---------------- channel ---------------- */
    if (channelSheetOpen) {
        ChannelSheet(
            onDismiss = { channelSheetOpen = false; pendingPublish = null },
            onSaved = {
                channelSheetOpen = false
                val pending = pendingPublish
                pendingPublish = null
                if (pending != null) publish(pending.first, pending.second)
            }
        )
    }

    /* ---------------- preview ---------------- */
    preview?.let { target ->
        ScenePreviewOverlay(
            target = target,
            store = store,
            publicRefresh = publicRefresh,
            onDismiss = { preview = null },
            onSave = { scene -> savePublic(scene, null) },
            onUnsave = { scene -> unsavePublic(scene) },
            onReport = { scene -> reportTarget = scene },
            onRefresh = { refresh(); publicRefresh++ }
        )
    }

    /* ---------------- rename / delete / reset / report / move ---------------- */
    renameTarget?.let { meta ->
        RenameSceneDialog(
            currentName = meta.name,
            onDismiss = { renameTarget = null },
            onRename = { newName ->
                store.updateSceneMeta(meta.id) {
                    it.copy(
                        name = newName.take(40),
                        modified = it.modified || it.isDefault
                    )
                }
                renameTarget = null
                refresh()
            }
        )
    }
    deleteTarget?.let { meta ->
        DeleteSceneDialog(
            name = meta.name,
            onDismiss = { deleteTarget = null },
            onDelete = {
                store.removeCustomScene(meta.id)
                deleteTarget = null
                refresh()
            }
        )
    }
    resetTarget?.let { meta ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    if (store.resetBundledDefault(meta.id)) {
                        toast(context, "Restored from the bundled scene")
                        refresh()
                    } else toast(context, "Couldn't reset this scene")
                    resetTarget = null
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { resetTarget = null }) { Text("Cancel") } },
            title = { Text("Reset to default?") },
            text = { Text("Your edits to “${meta.name}” will be replaced by the original bundled copy.") }
        )
    }
    deleteDefaultTarget?.let { remote ->
        AlertDialog(
            onDismissRequest = { deleteDefaultTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val token = AdminSession.token ?: return@launch
                        runCatching { Api.adminDeleteDefault(token, remote.id) }
                        deleteDefaultTarget = null
                        toast(context, "Default scene deleted")
                    }
                }) { Text("Delete", color = Color(0xFF9A4A3E)) }
            },
            dismissButton = { TextButton(onClick = { deleteDefaultTarget = null }) { Text("Cancel") } },
            title = { Text("Delete this default scene?") },
            text = { Text("Users who edited it keep their copy in Private; everyone else loses it on the next sync.") }
        )
    }
    reportTarget?.let { scene ->
        ReportDialog(
            sceneTitle = scene.title,
            onDismiss = { reportTarget = null },
            onReport = { reason ->
                val token = CloudSession.token
                if (token == null) {
                    CloudSession.signIn(context)
                    toast(context, "Sign in to report a scene")
                } else scope.launch {
                    runCatching { Api.reportScene(scene.id, reason, token) }
                    toast(context, "Report received")
                }
                reportTarget = null
            }
        )
    }
    moveTarget?.let { saved ->
        MoveToCollectionDialog(
            saved = saved,
            onDismiss = { moveTarget = null },
            onMove = { collectionId ->
                val token = CloudSession.token ?: return@MoveToCollectionDialog
                scope.launch {
                    runCatching { Api.saveScene(saved.scene.id, token, collectionId) }
                    store.updateSceneMeta(publicLocalId(saved.scene.id)) { it.copy(collectionId = collectionId) }
                    moveTarget = null
                    publicRefresh++
                    refresh()
                }
            }
        )
    }
}

private fun publicLocalId(remoteId: String) = "pub_$remoteId"

private fun readableError(e: Exception): String = when {
    e is ApiException && e.status == 401 -> "Please sign in again"
    e is ApiException && e.status == 429 -> "Too many requests. Try again shortly."
    e is ApiException -> e.message ?: "Server error"
    else -> e.message ?: "Network error"
}

/* ------------------------------------------------------------------ */
/* Main studio UI                                                     */
/* ------------------------------------------------------------------ */

@Composable
fun SceneStudioScreen(
    time: Float,
    settings: SettingsActions,
    tab: StudioTab,
    onTab: (StudioTab) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    searching: Boolean,
    onSearch: () -> Unit,
    defaults: List<HtmlSceneMeta>,
    privateScenes: List<HtmlSceneMeta>,
    savedPublic: List<HtmlSceneMeta>,
    thumbVersion: Int,
    publicRefresh: Int,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onUpload: () -> Unit,
    onUploadPrivate: () -> Unit,
    onUploadAdminPublic: () -> Unit,
    onPreviewDefault: (HtmlSceneMeta) -> Unit,
    onPreviewPrivate: (HtmlSceneMeta) -> Unit,
    onPreviewPublic: (PublicScene) -> Unit,
    onReplace: (HtmlSceneMeta) -> Unit,
    onRename: (HtmlSceneMeta) -> Unit,
    onDeletePrivate: (HtmlSceneMeta) -> Unit,
    onResetDefault: (HtmlSceneMeta) -> Unit,
    onAdminDeleteDefault: (RemoteDefaultScene) -> Unit,
    onAdminDeleteLocalDefault: (HtmlSceneMeta) -> Unit,
    onReport: (PublicScene) -> Unit,
    onSavePublic: (PublicScene) -> Unit,
    onUnsavePublic: (PublicScene) -> Unit,
    onMove: (SavedScene) -> Unit,
    onAdminDeletePublic: (PublicScene) -> Unit,
    onAdminReports: () -> Unit,
    onAdminLogin: () -> Unit,
    onPublishExisting: (HtmlSceneMeta) -> Unit,
    onRequestSignIn: () -> Unit,
    signedIn: Boolean
) {
    BackHandler { onBack() }
    val needle = query.trim()
    val visibleDefaults = remember(defaults, needle) {
        if (needle.isEmpty()) defaults else defaults.filter { it.name.contains(needle, true) }
    }
    val visiblePrivate = remember(privateScenes, needle) {
        if (needle.isEmpty()) privateScenes else privateScenes.filter { it.name.contains(needle, true) }
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
                Column(
                    Modifier
                        .weight(1f)
                        .longPressMillis(2000) { if (!AdminSession.active) onAdminLogin() else onAdminReports() }
                ) {
                    Text(
                        if (AdminSession.active) "SCENES · ADMIN" else "SCENES",
                        fontSize = 8.5.sp,
                        color = if (AdminSession.active) SafinaAccent else SafinaMuted,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("Scene studio", fontSize = 21.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
                }

                HeaderIcon(onClick = onSearch) { SearchGlyph(SafinaInk.copy(alpha = 0.8f), 18.dp) }
                Spacer(Modifier.width(6.dp))
                HeaderIcon(onClick = onInfo) { InfoGlyph(SafinaInk.copy(alpha = 0.8f)) }
                Spacer(Modifier.width(6.dp))
                SettingsMenu(actions = settings.copy(onHome = onBack))
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
                            Text(
                                "Search ${tab.name.lowercase()} scenes",
                                fontSize = 13.sp,
                                color = SafinaMuted.copy(alpha = 0.7f)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQuery,
                            singleLine = true,
                            cursorBrush = SolidColor(SafinaAccent),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = SafinaInk),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            StudioTabs(tab, onTab)
            Spacer(Modifier.height(16.dp))

            when (tab) {
                StudioTab.Default -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioAction(
                                label = if (AdminSession.active) "Upload" else "Upload",
                                primary = true,
                                onClick = onUpload,
                                modifier = Modifier.weight(1f)
                            )
                            if (AdminSession.active) {
                                StudioAction(
                                    label = "Add as public",
                                    primary = false,
                                    onClick = onUploadAdminPublic,
                                    modifier = Modifier.weight(1.1f)
                                )
                                StudioAction(
                                    label = "Reports",
                                    primary = false,
                                    onClick = onAdminReports,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                    if (visibleDefaults.isEmpty()) {
                        item { EmptyStudio("No default scenes match your search.") }
                    } else {
                        items(visibleDefaults, key = { it.id }) { meta ->
                            LocalSceneCard(
                                meta = meta,
                                thumbVersion = thumbVersion,
                                badge = "DEFAULT",
                                date = "Bundled scene",
                                isDefault = true,
                                admin = AdminSession.active,
                                onPreview = { onPreviewDefault(meta) },
                                onReplace = { onReplace(meta) },
                                onRename = { onRename(meta) },
                                onReset = if (meta.modified) ({ onResetDefault(meta) }) else null,
                                onDelete = null,
                                onAdminDelete = if (AdminSession.active && meta.defaultKey != null && BundledScenes.byKey(meta.defaultKey) == null) {
                                    {
                                        scope.launch {
                                            val token = AdminSession.token ?: return@launch
                                            runCatching { Api.adminDeleteDefault(token, meta.defaultKey) }
                                            store.removeCustomScene(meta.id)
                                            refresh()
                                            toast(context, "Default scene deleted")
                                        }
                                    }
                                } else null,
                                onPublish = null
                            )
                        }
                    }
                }

                StudioTab.Private -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (visiblePrivate.isEmpty()) {
                        item {
                            EmptyStudio(
                                if (needle.isEmpty()) "No private scenes yet. Upload an HTML file to begin."
                                else "No private scenes match your search."
                            )
                        }
                    } else {
                        items(visiblePrivate, key = { it.id }) { meta ->
                            LocalSceneCard(
                                meta = meta,
                                thumbVersion = thumbVersion,
                                badge = "PRIVATE",
                                date = formatDate(meta.addedAt),
                                isDefault = false,
                                admin = AdminSession.active,
                                onPreview = { onPreviewPrivate(meta) },
                                onReplace = { onReplace(meta) },
                                onRename = { onRename(meta) },
                                onReset = null,
                                onDelete = { onDeletePrivate(meta) },
                                onAdminDelete = null,
                                onPublish = { onPublishExisting(meta) }
                            )
                        }
                    }
                }

                StudioTab.Public -> PublicTab(
                    modifier = Modifier.weight(1f),
                    thumbVersion = thumbVersion,
                    refreshKey = publicRefresh,
                    query = query,
                    savedLocal = savedPublic,
                    signedIn = signedIn,
                    admin = AdminSession.active,
                    onRequestSignIn = onRequestSignIn,
                    onPreview = onPreviewPublic,
                    onSave = onSavePublic,
                    onUnsave = onUnsavePublic,
                    onMove = onMove,
                    onReport = onReport,
                    onAdminDelete = onAdminDeletePublic,
                    onAdminReports = onAdminReports,
                    onOpenAdmin = onAdminLogin
                )
            }

            Spacer(Modifier.navigationBarsPadding().height(4.dp))
        }
    }
}

@Composable
private fun StudioTabs(selected: StudioTab, onSelect: (StudioTab) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SafinaField)
            .border(1.dp, SafinaBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StudioTab.entries.forEach { item ->
            val active = item == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) SafinaInk else Color.Transparent)
                    .combinedClickable(onClick = { onSelect(item) }, onLongClick = { onSelect(item) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.name,
                    fontSize = 12.sp,
                    color = if (active) SafinaOnInk else SafinaMuted,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    letterSpacing = 0.2.sp
                )
            }
        }
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
            .border(1.dp, if (primary) Color.Transparent else SafinaBorder, RoundedCornerShape(15.dp))
            .combinedClickable(onClick = onClick, onLongClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (primary) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlusGlyph(SafinaOnInk, 14.dp)
                Spacer(Modifier.width(5.dp))
                Text(label, fontSize = 11.5.sp, color = SafinaOnInk, fontWeight = FontWeight.Medium)
            }
        } else {
            Text(
                label,
                fontSize = 10.5.sp,
                color = SafinaInk,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyStudio(message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nothing here yet", fontSize = 18.sp, color = SafinaInk, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 12.sp, color = SafinaMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LocalSceneCard(
    meta: HtmlSceneMeta,
    thumbVersion: Int,
    badge: String,
    date: String,
    isDefault: Boolean,
    admin: Boolean,
    onPreview: () -> Unit,
    onReplace: () -> Unit,
    onRename: () -> Unit,
    onReset: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onAdminDelete: (() -> Unit)?,
    onPublish: (() -> Unit)?
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SafinaSurface)
            .border(1.dp, SafinaBorder, RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onPreview, onLongClick = onPreview)
    ) {
        SceneThumb(
            sceneId = "$HTML_SCENE_PREFIX${meta.id}",
            version = thumbVersion,
            modifier = Modifier.fillMaxSize(),
            corner = 22.dp
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B100C).copy(alpha = 0.12f), Color(0xFF0B100C).copy(alpha = 0.58f))
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                badge,
                fontSize = 7.5.sp,
                color = Color(0xFFFDFBF3).copy(alpha = 0.66f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(5.dp))
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
            Spacer(Modifier.height(4.dp))
            Text(
                if (meta.modified) "$date · edited" else date,
                fontSize = 9.5.sp,
                color = Color(0xFFFDFBF3).copy(alpha = 0.7f),
                letterSpacing = 0.6.sp
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B100C).copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .combinedClickable(onClick = { menuOpen = true }, onLongClick = { menuOpen = true }),
                contentAlignment = Alignment.Center
            ) { MoreGlyph(Color(0xFFFDFBF3), 18.dp) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = SafinaSurface) {
                DropdownMenuItem(
                    text = { Text("Preview", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onPreview() }
                )
                DropdownMenuItem(
                    text = { Text("Replace HTML", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onReplace() }
                )
                DropdownMenuItem(
                    text = { Text("Rename", fontSize = 13.sp) },
                    onClick = { menuOpen = false; onRename() }
                )
                onReset?.let {
                    DropdownMenuItem(
                        text = { Text("Reset to default", fontSize = 13.sp) },
                        onClick = { menuOpen = false; it() }
                    )
                }
                onPublish?.let {
                    DropdownMenuItem(
                        text = { Text("Publish", fontSize = 13.sp) },
                        onClick = { menuOpen = false; it() }
                    )
                }
                onDelete?.let {
                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFB06A5C)) },
                        onClick = { menuOpen = false; it() }
                    )
                }
                if (admin && onAdminDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Admin delete", fontSize = 13.sp, color = Color(0xFFB06A5C)) },
                        onClick = { menuOpen = false; onAdminDelete() }
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Full-screen preview                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun ScenePreviewOverlay(
    target: PreviewTarget,
    store: ZikrStore,
    publicRefresh: Int,
    onDismiss: () -> Unit,
    onSave: (PublicScene) -> Unit,
    onUnsave: (PublicScene) -> Unit,
    onReport: (PublicScene) -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var html by remember(target.id) { mutableStateOf<String?>(null) }
    var loading by remember(target.id) { mutableStateOf(true) }
    var failed by remember(target.id) { mutableStateOf(false) }

    LaunchedEffect(target.id, publicRefresh) {
        loading = true
        failed = false
        html = withContext(Dispatchers.IO) {
            if (target.publicScene == null) store.readHtml(target.id)
            else store.readHtml(publicLocalId(target.id.removePrefix("pub_")))
        }
        if (html == null && target.publicScene != null) {
            val token = CloudSession.token
            val full = runCatching { Api.getPublicScene(target.publicScene.id, token) }.getOrNull()
            if (full?.html != null) {
                html = full.html
                withContext(Dispatchers.IO) {
                    store.upsertSavedPublic(
                        HtmlSceneMeta(
                            id = publicLocalId(full.id),
                            name = full.title,
                            fileName = "${publicLocalId(full.id)}.html",
                            addedAt = System.currentTimeMillis(),
                            source = SCENE_SOURCE_PUBLIC,
                            remoteId = full.id,
                            author = full.author,
                            savedCount = full.savedCount
                        ),
                        full.html.toByteArray()
                    )
                }
            } else failed = true
        } else if (html == null) failed = true
        loading = false
    }

    BackHandler { onDismiss() }
    var progress by remember { mutableStateOf(0.25f) }
    var tapSignal by remember { mutableIntStateOf(0) }
    var tapPoint by remember { mutableStateOf(Offset(0.5f, 0.65f)) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F0B))) {
        val document = html
        if (document != null) {
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
                    progress = progress,
                    tapSignal = tapSignal,
                    tapX = tapPoint.x,
                    tapY = tapPoint.y,
                    paused = false,
                    onError = { failed = true }
                )
            }
        }
        if (loading) {
            Text(
                "Loading scene…",
                fontSize = 13.sp,
                color = Color(0xFFFDFBF3),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (failed) {
            Text(
                "This scene couldn't be loaded.",
                fontSize = 13.sp,
                color = Color(0xFFFDFBF3),
                modifier = Modifier.align(Alignment.Center)
            )
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
                        target.title,
                        fontSize = 14.sp,
                        color = Color(0xFF3D453E),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = buildString {
                        target.author?.takeIf { it.isNotBlank() }?.let { append("by $it · ") }
                        if (target.publicScene != null) append("${target.savedCount} saves · ")
                        append(if (target.isDefault) "Bundled scene" else "Tap the scene to test it")
                    }
                    Text(subtitle, fontSize = 10.sp, color = Color(0xFF6C756D))
                }
                if (target.publicScene != null) {
                    val scene = target.publicScene
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFFE5E0D3))
                            .combinedClickable(
                                onClick = {
                                    if (target.publicScene.isSaved) onUnsave(target.publicScene)
                                    else onSave(target.publicScene)
                                    onRefresh()
                                },
                                onLongClick = {}
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            if (target.publicScene.isSaved) "Saved" else "Save",
                            fontSize = 11.sp,
                            color = Color(0xFF35423A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE9E4D7))
                            .combinedClickable(onClick = { onReport(target.publicScene) }, onLongClick = {}),
                        contentAlignment = Alignment.Center
                    ) { MoreGlyph(Color(0xFF3D453E), 16.dp) }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9E4D7))
                        .combinedClickable(onClick = onDismiss, onLongClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { CloseGlyph(Color(0xFF3D453E), 13.dp) }
            }
            if (!failed && !loading) {
                androidx.compose.material3.Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Misc helpers                                                       */
/* ------------------------------------------------------------------ */

fun Modifier.longPressMillis(millis: Long, action: () -> Unit): Modifier = pointerInput(millis) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val released = kotlinx.coroutines.withTimeoutOrNull(millis) {
            waitForUpOrCancellation()
        }
        if (released == null) {
            action()
            waitForUpOrCancellation()
        }
    }
}

private fun formatDate(epoch: Long): String =
    if (epoch <= 0) "Bundled scene"
    else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epoch))
