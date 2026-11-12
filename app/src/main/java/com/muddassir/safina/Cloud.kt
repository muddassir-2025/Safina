package com.muddassir.safina

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/* ------------------------------------------------------------------ */
/* Small JSON/HTTP client for the Render backend.                      */
/* ------------------------------------------------------------------ */

class ApiException(
    val status: Int,
    message: String,
    val code: String? = null
) : Exception(message)

data class RemoteProfile(
    val userId: String,
    val email: String?,
    val channelName: String?,
    val isAdmin: Boolean = false
)

data class PublicScene(
    val id: String,
    val title: String,
    val author: String,
    val savedCount: Int,
    val size: Int = 0,
    val createdAt: Long = 0L,
    val isSaved: Boolean = false,
    val isOwn: Boolean = false,
    val removed: Boolean = false,
    val locked: Boolean = false,
    val ownerId: String? = null,
    val html: String? = null
)

data class CollectionInfo(val id: String?, val name: String, val count: Int = 0)

data class SavedScene(val scene: PublicScene, val collectionId: String?, val removed: Boolean)

data class AdminRemovedScene(val id: String, val title: String)

data class AdminBlockedUser(val userId: String, val channelName: String, val email: String?)

data class AdminOverview(
    val removed: List<AdminRemovedScene>,
    val blocked: List<AdminBlockedUser>,
    val reports: List<ReportedScene>
)

data class RemoteDefaultScene(
    val id: String,
    val title: String,
    val html: String,
    val size: Int,
    val sortOrder: Int,
    val version: Int
)

data class ReportedScene(
    val sceneId: String,
    val title: String,
    val author: String,
    val reportCount: Int,
    val reasons: String,
    val lastReportedAt: Long
)

data class DefaultsResult(val notModified: Boolean, val version: String?, val items: List<RemoteDefaultScene>)

object Api {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/health").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun execute(
        method: String,
        path: String,
        token: String? = null,
        adminToken: String? = null,
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        absoluteUrl: String? = null
    ): String {
        val url = absoluteUrl ?: "$baseUrl$path"
        val builder = Request.Builder().url(url)
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }
        token?.let { builder.header("Authorization", "Bearer $it") }
        adminToken?.let { builder.header("Authorization", "Bearer $it") }
        val requestBody = body?.toString()?.toRequestBody(jsonType)
            ?: if (method == "POST" || method == "PUT" || method == "PATCH") "{}".toRequestBody(jsonType) else null
        builder.method(method, requestBody)
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsed = runCatching { JSONObject(text) }.getOrNull()
                throw ApiException(
                    response.code,
                    parsed?.optString("error")?.ifBlank { null }
                        ?: "Server error (${response.code})",
                    parsed?.optString("code")?.ifBlank { null }
                )
            }
            return text
        }
    }

    /**
     * Render free instances sleep. Try the request, then retry twice with a short
     * backoff when the connection times out or the instance is still waking up.
     */
    private suspend fun <T> withColdStartRetry(block: () -> T): T = withContext(Dispatchers.IO) {
        var last: Throwable? = null
        repeat(3) { attempt ->
            try {
                return@withContext block()
            } catch (e: ApiException) {
                last = e
                if (e.status !in 500..599 || attempt == 2) throw e
            } catch (e: Exception) {
                last = e
                if (attempt == 2) throw e
            }
            delay(if (attempt == 0) 1200L else 3000L)
        }
        throw last ?: ApiException(0, "Couldn't reach the server")
    }

    private fun obj(method: String, path: String, token: String? = null, admin: String? = null, body: JSONObject? = null, headers: Map<String, String> = emptyMap()): JSONObject? = runCatching {
        val text = execute(method, path, token, admin, body, headers)
        if (text.isBlank()) null else JSONObject(text)
    }.getOrElse { if (it is ApiException) throw it else throw ApiException(0, it.message ?: "Network error") }

    /* ---------------- auth ---------------- */

    suspend fun me(token: String): RemoteProfile = withColdStartRetry {
        val root = obj("GET", "/auth/me", token = token) ?: throw ApiException(0, "Empty response")
        val user = root.optJSONObject("user")
        val profile = root.optJSONObject("profile")
        RemoteProfile(
            userId = user?.optString("id").orEmpty(),
            email = user?.optString("email")?.ifBlank { null },
            channelName = profile?.optString("channelName")?.ifBlank { null },
            isAdmin = root.optBoolean("isAdmin", false)
        )
    }

    suspend fun checkChannel(name: String, token: String): Boolean = withColdStartRetry {
        val root = obj("GET", "/profile/channel/available?name=${Uri.encode(name)}", token = token)
        root?.optBoolean("available", false) ?: false
    }

    suspend fun setChannel(name: String, token: String): RemoteProfile = withColdStartRetry {
        val root = obj("PUT", "/profile/channel", token = token, body = JSONObject().put("channelName", name))
        val profile = root?.optJSONObject("profile")
        RemoteProfile(
            userId = profile?.optString("userId").orEmpty(),
            email = profile?.optString("email")?.ifBlank { null },
            channelName = profile?.optString("channelName")
        )
    }

    /* ---------------- public scenes ---------------- */

    suspend fun listPublic(
        token: String?,
        page: Int,
        sort: String,
        query: String,
        excludeSaved: Boolean
    ): Pair<List<PublicScene>, Boolean> = withColdStartRetry {
        val path = buildString {
            append("/public/scenes?page=").append(page)
            append("&limit=20&sort=").append(Uri.encode(sort))
            if (query.isNotBlank()) append("&q=").append(Uri.encode(query))
            if (excludeSaved) append("&excludeSaved=true")
        }
        val root = obj("GET", path, token = token) ?: throw ApiException(0, "Empty response")
        val arr = root.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i -> publicScene(arr.getJSONObject(i)) } to root.optBoolean("hasMore", false)
    }

    suspend fun getPublicScene(id: String, token: String?): PublicScene = withColdStartRetry {
        val root = obj("GET", "/public/scenes/${Uri.encode(id)}", token = token)
            ?: throw ApiException(0, "Empty response")
        publicScene(root.getJSONObject("scene"))
    }

    suspend fun publish(title: String, html: String, token: String): PublicScene = withColdStartRetry {
        val root = obj(
            "POST", "/public/scenes", token = token,
            body = JSONObject().put("title", title).put("html", html)
        ) ?: throw ApiException(0, "Empty response")
        publicScene(root.getJSONObject("scene"))
    }

    suspend fun deleteScene(id: String, token: String) = withColdStartRetry {
        obj("DELETE", "/public/scenes/${Uri.encode(id)}", token = token)
        Unit
    }

    suspend fun saveScene(id: String, token: String, collectionId: String?): Int = withColdStartRetry {
        val root = obj(
            "POST", "/scenes/${Uri.encode(id)}/save", token = token,
            body = JSONObject().put("collectionId", collectionId ?: JSONObject.NULL)
        )
        root?.optInt("savedCount", 0) ?: 0
    }

    suspend fun unsaveScene(id: String, token: String): Int = withColdStartRetry {
        val root = obj("DELETE", "/scenes/${Uri.encode(id)}/save", token = token)
        root?.optInt("savedCount", 0) ?: 0
    }

    suspend fun mySaves(token: String): List<SavedScene> = withColdStartRetry {
        val root = obj("GET", "/me/saves", token = token) ?: throw ApiException(0, "Empty response")
        val arr = root.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val sceneObj = o.optJSONObject("scene")
            SavedScene(
                scene = sceneObj?.let { publicScene(it) } ?: PublicScene(
                    id = o.optString("sceneId"),
                    title = o.optString("title", "Removed scene"),
                    author = "",
                    savedCount = 0,
                    removed = true
                ),
                collectionId = o.optString("collectionId").ifBlank { null },
                removed = o.optBoolean("removed", false)
            )
        }
    }

    suspend fun collections(token: String): List<CollectionInfo> = withColdStartRetry {
        val root = obj("GET", "/collections", token = token) ?: throw ApiException(0, "Empty response")
        val arr = root.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CollectionInfo(o.optString("id"), o.optString("name"), o.optInt("count", 0))
        }
    }

    suspend fun createCollection(name: String, token: String): CollectionInfo = withColdStartRetry {
        val root = obj("POST", "/collections", token = token, body = JSONObject().put("name", name))
            ?: throw ApiException(0, "Empty response")
        val c = root.getJSONObject("collection")
        CollectionInfo(c.optString("id"), c.optString("name"), c.optInt("count", 0))
    }

    suspend fun renameCollection(id: String, name: String, token: String): CollectionInfo = withColdStartRetry {
        val root = obj(
            "PATCH", "/collections/${Uri.encode(id)}", token = token,
            body = JSONObject().put("name", name)
        ) ?: throw ApiException(0, "Empty response")
        val c = root.getJSONObject("collection")
        CollectionInfo(c.optString("id"), c.optString("name"), c.optInt("count", 0))
    }

    suspend fun deleteCollection(id: String, token: String) = withColdStartRetry {
        obj("DELETE", "/collections/${Uri.encode(id)}", token = token)
        Unit
    }

    suspend fun reportScene(id: String, reason: String, token: String) = withColdStartRetry {
        obj(
            "POST", "/scenes/${Uri.encode(id)}/report", token = token,
            body = JSONObject().put("reason", reason)
        )
        Unit
    }

    /* ---------------- default scene sync ---------------- */

    suspend fun defaultScenes(etag: String?): DefaultsResult = withColdStartRetry {
        val headers = if (etag.isNullOrBlank()) emptyMap() else mapOf("If-None-Match" to etag)
        try {
            val text = execute("GET", "/default-scenes", extraHeaders = headers)
            val root = JSONObject(text)
            val arr = root.optJSONArray("items") ?: JSONArray()
            val items = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                RemoteDefaultScene(
                    id = o.getString("id"),
                    title = o.optString("title", "Default scene"),
                    html = o.optString("html"),
                    size = o.optInt("size", 0),
                    sortOrder = o.optInt("sortOrder", 0),
                    version = o.optInt("version", 1)
                )
            }
            DefaultsResult(false, root.optString("version").ifBlank { null }, items)
        } catch (e: ApiException) {
            if (e.status == 304) DefaultsResult(true, null, emptyList()) else throw e
        }
    }

    /* ---------------- admin ---------------- */

    suspend fun reviewLogin(email: String): String = withColdStartRetry {
        val root = obj(
            "POST", "/auth/review",
            body = JSONObject().put("email", email)
        ) ?: throw ApiException(0, "Empty response")
        root.optString("token").ifBlank { throw ApiException(0, "Sign-in failed") }
    }

    suspend fun adminLogin(email: String, password: String): Pair<String, Long> = withColdStartRetry {
        val root = obj(
            "POST", "/admin/login",
            body = JSONObject().put("email", email).put("password", password)
        ) ?: throw ApiException(0, "Empty response")
        root.optString("token") to root.optLong("expiresAt", System.currentTimeMillis() + 30 * 60_000)
    }

    suspend fun adminListDefaults(token: String): List<RemoteDefaultScene> = withColdStartRetry {
        val root = obj("GET", "/default-scenes", admin = token) ?: throw ApiException(0, "Empty response")
        val arr = root.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RemoteDefaultScene(
                o.getString("id"), o.optString("title"), o.optString("html"),
                o.optInt("size"), o.optInt("sortOrder"), o.optInt("version", 1)
            )
        }
    }

    suspend fun adminAddDefault(token: String, title: String, html: String, sortOrder: Int): Unit = withColdStartRetry {
        obj(
            "POST", "/admin/default-scenes", admin = token,
            body = JSONObject().put("title", title).put("html", html).put("sortOrder", sortOrder)
        )
        Unit
    }

    suspend fun adminUpdateDefault(token: String, id: String, title: String, html: String, sortOrder: Int): Unit = withColdStartRetry {
        obj(
            "PUT", "/admin/default-scenes/${Uri.encode(id)}", admin = token,
            body = JSONObject().put("title", title).put("html", html).put("sortOrder", sortOrder)
        )
        Unit
    }

    suspend fun adminDeleteDefault(token: String, id: String): Unit = withColdStartRetry {
        obj("DELETE", "/admin/default-scenes/${Uri.encode(id)}", admin = token)
        Unit
    }

    suspend fun adminAddPublic(token: String, title: String, html: String, channelName: String?): Unit = withColdStartRetry {
        obj(
            "POST", "/admin/public-scenes", admin = token,
            body = JSONObject()
                .put("title", title)
                .put("html", html)
                .put("channelName", channelName ?: JSONObject.NULL)
        )
        Unit
    }

    suspend fun adminLockScene(id: String, locked: Boolean, token: String): Unit = withColdStartRetry {
        obj(
            "POST",
            "/admin/public-scenes/${Uri.encode(id)}/lock",
            admin = token,
            body = JSONObject().put("locked", locked)
        )
        Unit
    }

    suspend fun adminDeletePublic(token: String, id: String, reason: String?): Unit = withColdStartRetry {
        obj(
            "DELETE", "/admin/public-scenes/${Uri.encode(id)}", admin = token,
            body = JSONObject().put("reason", reason ?: "Removed by moderator")
        )
        Unit
    }

    suspend fun adminDashboard(token: String): AdminOverview = withColdStartRetry {
        val root = obj("GET", "/admin/dashboard", admin = token)
            ?: throw ApiException(0, "Empty response")
        val removedArr = root.optJSONArray("removed") ?: JSONArray()
        val blockedArr = root.optJSONArray("blocked") ?: JSONArray()
        val reportsArr = root.optJSONArray("reports") ?: JSONArray()
        AdminOverview(
            removed = (0 until removedArr.length()).map { i ->
                val o = removedArr.getJSONObject(i)
                AdminRemovedScene(o.optString("id"), o.optString("title"))
            },
            blocked = (0 until blockedArr.length()).map { i ->
                val o = blockedArr.getJSONObject(i)
                AdminBlockedUser(
                    o.optString("userId"),
                    o.optString("channelName"),
                    o.optString("email").ifBlank { null }
                )
            },
            reports = (0 until reportsArr.length()).map { i ->
                val o = reportsArr.getJSONObject(i)
                ReportedScene(
                    o.optString("sceneId"), o.optString("title"), "",
                    o.optInt("reportCount"), o.optString("reasons"), 0L
                )
            }
        )
    }

    suspend fun adminBlockUser(userId: String, blocked: Boolean, token: String): Unit = withColdStartRetry {
        obj(
            "POST",
            "/admin/users/${Uri.encode(userId)}/block",
            admin = token,
            body = JSONObject().put("blocked", blocked)
        )
        Unit
    }

    suspend fun adminRestoreScene(id: String, token: String): Unit = withColdStartRetry {
        obj("POST", "/admin/public-scenes/${Uri.encode(id)}/restore", admin = token)
        Unit
    }

    suspend fun adminReports(token: String): List<ReportedScene> = withColdStartRetry {
        val root = obj("GET", "/admin/reports", admin = token) ?: throw ApiException(0, "Empty response")
        val arr = root.optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ReportedScene(
                o.optString("sceneId"), o.optString("title"), o.optString("author"),
                o.optInt("reportCount"), o.optString("reasons"), o.optLong("lastReportedAt")
            )
        }
    }

    suspend fun adminDismissReports(token: String, sceneId: String): Unit = withColdStartRetry {
        obj("POST", "/admin/reports/${Uri.encode(sceneId)}/dismiss", admin = token)
        Unit
    }

    private fun publicScene(o: JSONObject) = PublicScene(
        id = o.optString("id"),
        title = o.optString("title", "Scene"),
        author = o.optString("author", ""),
        savedCount = o.optInt("savedCount", 0),
        size = o.optInt("size", 0),
        createdAt = o.optLong("createdAt", 0),
        isSaved = o.optBoolean("isSaved", false),
        isOwn = o.optBoolean("isOwn", false),
        removed = o.optBoolean("removed", false),
        locked = o.optBoolean("locked", false),
        ownerId = o.optString("ownerId").ifBlank { null },
        html = o.optString("html").ifBlank { null }
    )
}

/* ------------------------------------------------------------------ */
/* Auth: Neon Auth JWT handed back through a Custom Tab deep link.     */
/* ------------------------------------------------------------------ */

object CloudSession {
    private const val PREFS = "safina_secure_auth"
    private const val KEY_TOKEN = "neon_jwt"

    var token by mutableStateOf<String?>(null)
        private set
    var profile by mutableStateOf<RemoteProfile?>(null)
        private set
    var busy by mutableStateOf(false)

    /** Drives the in-app sign-in sheet (Google + email/password) for Play review. */
    var signInSheetOpen by mutableStateOf(false)

    /** Email/password (Firebase) alternative sign-in for Play review. */
    var emailSignedIn by mutableStateOf(false)
        private set
    var emailUser by mutableStateOf<String?>(null)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = runCatching {
            val key = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as android.content.SharedPreferences
        }.getOrElse {
            context.getSharedPreferences(PREFS + "_fallback", Context.MODE_PRIVATE)
        }
        val stored = prefs?.getString(KEY_TOKEN, null)
        if (!stored.isNullOrBlank() && !jwtExpired(stored)) token = stored
        else prefs?.edit()?.remove(KEY_TOKEN)?.apply()
    }

    val signedIn: Boolean get() = token != null || emailSignedIn

    val isAdmin: Boolean get() = profile?.isAdmin == true

    fun signIn(context: Context) {
        val url = "${Api.baseUrl}/auth/google/start?redirect=${Uri.encode(BuildConfig.AUTH_REDIRECT)}"
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(url))
        }
    }

    /** Email-only sign-in for Play review (no password). Calls the backend to obtain a JWT. */
    suspend fun signInWithEmail(email: String) {
        if (email.isBlank() || !email.contains('@')) {
            throw ApiException(400, "Enter a valid email")
        }
        val jwt = Api.reviewLogin(email.trim())
        if (jwt.isBlank()) throw ApiException(0, "Sign-in failed")
        token = jwt
        emailUser = email.trim()
        emailSignedIn = true
        prefs?.edit()?.putString(KEY_TOKEN, jwt)?.apply()
        refreshProfile()
    }

    /** Called by MainActivity when Chrome hands `safina://auth?token=...` back. */
    fun handleDeepLink(uri: Uri): Boolean {
        if (uri.scheme != "safina" || uri.host != "auth") return false
        val value = uri.getQueryParameter("token") ?: return false
        if (jwtExpired(value)) return false
        token = value
        prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        return true
    }

    suspend fun refreshProfile() {
        val current = token ?: return
        busy = true
        try {
            profile = Api.me(current)
        } catch (_: Exception) {
            if (jwtExpired(current)) signOut()
        } finally {
            busy = false
        }
    }

    fun signOut() {
        token = null
        profile = null
        emailSignedIn = false
        emailUser = null
        runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() }
        prefs?.edit()?.remove(KEY_TOKEN)?.apply()
    }
}

/* ------------------------------------------------------------------ */
/* Admin session: separate JWT, kept in memory with a hard expiry.     */
/* ------------------------------------------------------------------ */

object AdminSession {
    var token by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf<String?>(null)
        private set
    var expiresAt by mutableStateOf(0L)
        private set

    /** A Google account whose email is ADMIN_EMAIL is an admin without a password login. */
    val active: Boolean
        get() = (token != null && System.currentTimeMillis() < expiresAt) || CloudSession.isAdmin

    /** Token to send on /admin routes: the admin JWT, else the admin user's JWT. */
    val authToken: String?
        get() = if (token != null && System.currentTimeMillis() < expiresAt) token
        else CloudSession.token.takeIf { CloudSession.isAdmin }

    fun setToken(value: String, expires: Long, adminEmail: String) {
        token = value
        expiresAt = expires
        email = adminEmail
        if (jwtExpired(value)) signOut()
    }

    fun signOut() {
        token = null
        email = null
        expiresAt = 0
    }
}

/* ------------------------------------------------------------------ */
/* JWT expiry helper (no signature check on device; server is the      */
/* authority — this is only used to avoid sending a stale token).      */
/* ------------------------------------------------------------------ */

fun jwtExpired(jwt: String): Boolean {
    val payload = jwt.split('.').getOrNull(1) ?: return true
    return runCatching {
        val bytes = android.util.Base64.decode(
            payload + "=".repeat((4 - payload.length % 4) % 4),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        val exp = JSONObject(bytes.decodeToString()).optLong("exp", 0L)
        exp <= (System.currentTimeMillis() / 1000) + 5
    }.getOrDefault(true)
}

fun jwtExpiryMillis(jwt: String): Long {
    val payload = jwt.split('.').getOrNull(1) ?: return 0L
    return runCatching {
        val bytes = android.util.Base64.decode(
            payload + "=".repeat((4 - payload.length % 4) % 4),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        JSONObject(bytes.decodeToString()).optLong("exp", 0L) * 1000L
    }.getOrDefault(0L)
}
