package com.muddassir.safina

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper over Firebase Analytics so screens never touch Firebase directly
 * and a missing configuration (no google-services.json) simply logs nothing.
 */
object Analytics {
    private var fa: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (fa != null) return
        if (FirebaseApp.getApps(context).isEmpty()) return
        fa = FirebaseAnalytics.getInstance(context)
    }

    fun sessionStarted(zikrId: String, sceneId: String, target: Int) {
        send("zikr_session_started") {
            putString("zikr_id", zikrId)
            putString("scene_id", sceneId)
            putInt("target", target)
        }
    }

    fun sessionCompleted(zikrId: String, sceneId: String, target: Int, elapsedSeconds: Float) {
        send("zikr_session_completed") {
            putString("zikr_id", zikrId)
            putString("scene_id", sceneId)
            putInt("target", target)
            putInt("minutes", (elapsedSeconds / 60f).toInt().coerceIn(0, 600))
        }
    }

    fun signedIn(method: String) {
        send("sign_in") {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun signInFailed(reason: String) {
        send("sign_in_failed") {
            putString("reason", reason)
        }
    }

    private inline fun send(event: String, block: Bundle.() -> Unit) {
        val analytics = fa ?: return
        val params = Bundle().apply(block)
        analytics.logEvent(event, params)
        if (BuildConfig.DEBUG) android.util.Log.d("SafinaAnalytics", "$event $params")
    }
}
