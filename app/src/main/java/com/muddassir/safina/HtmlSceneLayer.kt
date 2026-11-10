package com.muddassir.safina

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.util.Locale

private const val SCENE_BASE_URL = "https://scenes.local/"
private const val READY_TIMEOUT_MS = 4000L
private const val CSP =
    "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; " +
        "img-src data: blob:; connect-src 'none'; frame-src 'none'; font-src 'none'; media-src 'none'"

/** Injects a strict CSP into the head, so an uploaded file cannot reach the network. */
fun withContentSecurityPolicy(html: String): String {
    if (html.contains("Content-Security-Policy", ignoreCase = true)) return html
    val meta = "<meta http-equiv=\"Content-Security-Policy\" content=\"$CSP\"/>"
    val headIndex = html.indexOf("<head", ignoreCase = true)
    if (headIndex >= 0) {
        val end = html.indexOf('>', headIndex)
        if (end >= 0) return html.substring(0, end + 1) + meta + html.substring(end + 1)
    }
    return meta + html
}

private fun jsFloat(value: Float): String = String.format(Locale.US, "%.4f", value)

/**
 * Samples the top 35% of the scene canvas and reports the real brightness back through
 * ZikrHost.setTextTheme. This keeps the counter readable even when a scene reports its
 * theme late, ambiguously, or not at all.
 */
private val BRIGHTNESS_PROBE_JS = """
(function () {
  var canvas = document.querySelector('canvas');
  if (!canvas) return;
  var probeCanvas = document.createElement('canvas');
  probeCanvas.width = 24;
  probeCanvas.height = 16;
  var probeCtx = null;
  try { probeCtx = probeCanvas.getContext('2d', { willReadFrequently: true }); } catch (e) { return; }
  if (!probeCtx) return;
  function probe() {
    try {
      var w = canvas.width, h = canvas.height;
      if (w > 0 && h > 0) {
        var top = Math.max(1, Math.floor(h * 0.35));
        // Shrink first, then read back only 24x16 pixels: cheap even at 60 fps.
        probeCtx.clearRect(0, 0, 24, 16);
        probeCtx.drawImage(canvas, 0, 0, w, top, 0, 0, 24, 16);
        var data = probeCtx.getImageData(0, 0, 24, 16).data;
        var sum = 0, n = 0;
        for (var i = 0; i < data.length; i += 4) {
          if (data[i + 3] < 200) continue;
          sum += 0.2126 * data[i] + 0.7152 * data[i + 1] + 0.0722 * data[i + 2];
          n++;
        }
        if (n > 0 && window.ZikrHost) {
          var lum = (sum / n) / 255;
          window.ZikrHost.setTextTheme(lum < 0.46 ? 'dark' : 'light');
        }
      }
    } catch (e) {}
    setTimeout(probe, 1200);
  }
  probe();
})();
""".trimIndent()

/** A WebView that never takes touches, so the Compose parent owns taps and swipes. */
private class PassiveWebView(context: Context) : WebView(context) {
    override fun onTouchEvent(event: MotionEvent?): Boolean = false
    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean = false
}

private class HostBridge(
    private val onReady: () -> Unit,
    private val onTheme: (String) -> Unit,
    private val onMilestone: (Float) -> Unit
) {
    @JavascriptInterface
    fun ready() = onReady()

    @JavascriptInterface
    fun setTextTheme(theme: String) {
        if (theme == "dark" || theme == "light") onTheme(theme)
    }

    @JavascriptInterface
    fun milestone(fraction: Float) {
        if (!fraction.isNaN()) onMilestone(fraction.coerceIn(0f, 1f))
    }
}

/**
 * Runs one uploaded HTML scene behind the native UI.
 * The page eases its own progress; this layer only pushes changes.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlSceneLayer(
    html: String,
    progress: Float,
    tapSignal: Int,
    tapX: Float,
    tapY: Float,
    paused: Boolean,
    modifier: Modifier = Modifier,
    placeholderTop: Color = Color(0xFFB9CFD6),
    placeholderBottom: Color = Color(0xFFE6EDE6),
    onDarkScene: (Boolean) -> Unit = {},
    onReady: () -> Unit = {},
    onError: () -> Unit = {}
) {
    val ready = remember { mutableStateOf(false) }
    val initialized = remember { mutableStateOf(false) }
    val webRef = remember { mutableStateOf<WebView?>(null) }
    val fade = remember { Animatable(0f) }
    val main = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(ready.value) {
        if (ready.value) {
            fade.animateTo(1f, tween(400))
            onReady()
        } else {
            kotlinx.coroutines.delay(READY_TIMEOUT_MS)
            if (!ready.value) onError()
        }
    }

    // init() once, then one setProgress() per count change — never per frame.
    LaunchedEffect(ready.value, progress) {
        if (!ready.value) return@LaunchedEffect
        val view = webRef.value ?: return@LaunchedEffect
        if (!initialized.value) {
            initialized.value = true
            view.evaluateJavascript(
                "window.zikrScene&&window.zikrScene.init(${jsFloat(progress)})", null
            )
            view.evaluateJavascript(BRIGHTNESS_PROBE_JS, null)
        } else {
            view.evaluateJavascript(
                "window.zikrScene&&window.zikrScene.setProgress(${jsFloat(progress)})", null
            )
        }
    }

    LaunchedEffect(tapSignal) {
        if (tapSignal <= 0 || !ready.value) return@LaunchedEffect
        webRef.value?.evaluateJavascript(
            "window.zikrScene&&window.zikrScene.tap(${jsFloat(tapX)},${jsFloat(tapY)})", null
        )
    }

    LaunchedEffect(ready.value, paused) {
        if (!ready.value) return@LaunchedEffect
        val view = webRef.value ?: return@LaunchedEffect
        view.evaluateJavascript(
            "window.zikrScene&&window.zikrScene.setPaused(${paused})", null
        )
        if (paused) view.onPause() else view.onResume()
    }

    DisposableEffect(Unit) {
        onDispose {
            webRef.value?.let { view ->
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                view.destroy()
            }
            webRef.value = null
        }
    }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(placeholderTop, placeholderBottom)))
        )
        AndroidView(
            factory = { context ->
                createSceneWebView(
                    context = context,
                    rawHtml = html,
                    main = main,
                    ready = ready,
                    onDarkScene = onDarkScene,
                    onError = onError
                ).also { webRef.value = it }
            },
            update = { webRef.value = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fade.value }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createSceneWebView(
    context: Context,
    rawHtml: String,
    main: Handler,
    ready: MutableState<Boolean>,
    onDarkScene: (Boolean) -> Unit,
    onError: () -> Unit
): WebView {
    val bridge = HostBridge(
        onReady = { main.post { ready.value = true } },
        onTheme = { theme -> main.post { onDarkScene(theme == "dark") } },
        onMilestone = { }
    )
    val html = withContentSecurityPolicy(rawHtml)

    return PassiveWebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isFocusable = false
        isFocusableInTouchMode = false
        isClickable = false
        isLongClickable = false
        isHapticFeedbackEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setOnLongClickListener { true }
        setOnTouchListener { _, _ -> false }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            blockNetworkLoads = true
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadsImagesAutomatically = true
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
        }

        addJavascriptInterface(bridge, "ZikrHost")

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // Nothing but inline data: content may load.
                return if (request.url.toString().startsWith("data:")) {
                    null
                } else {
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                onError()
                return true
            }
        }

        loadDataWithBaseURL(SCENE_BASE_URL, html, "text/html", "utf-8", null)
    }
}
