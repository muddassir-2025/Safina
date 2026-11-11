package com.muddassir.safina

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Decodes the `data:image/png;base64,...` string a scene sends back. */
fun decodePngDataUrl(dataUrl: String): ByteArray? = runCatching {
    val base64 = dataUrl.substringAfter("base64,", "")
    if (base64.isBlank()) null else Base64.decode(base64, Base64.DEFAULT)
}.getOrNull()

@Composable
private fun rememberThumbnail(sceneId: String, version: Int): ImageBitmap? {
    if (!sceneId.startsWith(HTML_SCENE_PREFIX)) return null
    val context = LocalContext.current
    val store = remember { ZikrStore(context) }
    val id = sceneId.removePrefix(HTML_SCENE_PREFIX)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, id, version) {
        val file = store.thumbnailFile(id)
        value = if (file.length() > 0L) {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }
        } else null
    }
    return bitmap
}

/**
 * A real picture of the scene: the native scenes are drawn live at a small size,
 * uploaded scenes show the snapshot captured the first time they were rendered.
 */
@Composable
fun SceneThumb(
    sceneId: String,
    version: Int,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp
) {
    val isHtml = sceneId.startsWith(HTML_SCENE_PREFIX)
    val bitmap = rememberThumbnail(sceneId, version)
    Box(modifier.clip(RoundedCornerShape(corner))) {
        if (isHtml) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                HtmlScenePlaceholder(sceneId)
            }
        } else {
            NativeSceneThumb(sceneId)
        }
    }
}

@Composable
private fun NativeSceneThumb(sceneId: String) {
    val scene = Scenes.byId(sceneId)
    Canvas(Modifier.fillMaxSize()) {
        with(scene) {
            draw(
                SceneFrame(
                    progress = 0.35f,
                    time = 0.18f,
                    pulse = 1f,
                    ripple = 1f,
                    calm = true
                )
            )
        }
    }
    // Slight vignette for depth, the same treatment as the photo thumbnails.
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFF101410).copy(alpha = 0.16f))
            )
        )
    )
}

/** Shown only until the snapshot of an uploaded scene has been captured. */
@Composable
private fun HtmlScenePlaceholder(sceneId: String) {
    val colors = sceneColors(sceneId)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(colors))
        drawCircle(
            Color.White.copy(alpha = 0.22f), size.width * 0.38f,
            Offset(size.width * 0.72f, size.height * 0.24f)
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFF0E1311).copy(alpha = 0.30f))
            ),
            size = Size(size.width, size.height)
        )
    }
}

/**
 * Silently renders each uploaded scene once, off-screen, to capture its artwork.
 * Runs one WebView at a time and only while some thumbnail is still missing.
 */
@Composable
fun ThumbnailHarvester(
    scenes: List<HtmlSceneMeta>,
    store: ZikrStore,
    version: Int,
    onCaptured: () -> Unit
) {
    val missing = remember(scenes, version) {
        scenes.filter { !store.hasThumbnail(it.id) }
    }
    var cursor by remember(version) { mutableIntStateOf(0) }
    val meta = missing.getOrNull(cursor)
    if (meta == null) return
    val html by produceState<String?>(null, meta.id) {
        value = withContext(Dispatchers.IO) { store.readHtml(meta.id) }
    }
    val document = html ?: return
    var handled by remember(meta.id) { mutableStateOf(false) }

    LaunchedEffect(meta.id, cursor) {
        delay(7_000)
        if (!handled) {
            handled = true
            cursor++
        }
    }

    Box(
        Modifier
            .offset(x = 0.dp, y = 0.dp)
            .alpha(0f)
            .size(300.dp, 460.dp)
    ) {
        HtmlSceneLayer(
            html = document,
            progress = 0.3f,
            tapSignal = 0,
            tapX = 0.5f,
            tapY = 0.65f,
            paused = false,
            onSnapshot = { url ->
                if (!handled) {
                    handled = true
                    decodePngDataUrl(url)?.let { store.saveThumbnail(meta.id, it) }
                    onCaptured()
                    cursor++
                }
            }
        )
    }
}
