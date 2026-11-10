package com.muddassir.safina

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A dhikr entry: what is recited, on which scene, how far the user has got. */
data class Zikr(
    val id: String,
    val arabic: String,
    val translation: String,
    val target: Int,
    val sceneId: String,
    val count: Int,
    val arabicDone: String = "تَقَبَّلَ اللَّهُ مِنَّا وَمِنْكُمْ",
    val translationDone: String = "May Allah accept it from us and from you"
)

/** The one place scenes are registered. Adding a scene = one file + one line here. */
object Scenes {
    val all: List<Scene> = listOf(
        GardenScene,
        WinterScene,
        OceanScene,
        NightScene
    )

    fun byId(id: String): Scene = all.firstOrNull { it.id == id } ?: GardenScene
}

private const val ASTAGHFIRULLAH = "أَسْتَغْفِرُ اللَّهَ"
private const val ASTAGHFIRULLAH_EN = "Astaghfirullah · I seek Allah's forgiveness"
private const val DEFAULT_TARGET = 100

/** Default dhikr list: one Astaghfirullah per scene. A future dashboard can edit/reorder this. */
fun defaultZikrs(): List<Zikr> = Scenes.all.map { scene ->
    Zikr(
        id = "zikr_${scene.id}",
        arabic = ASTAGHFIRULLAH,
        translation = ASTAGHFIRULLAH_EN,
        target = DEFAULT_TARGET,
        sceneId = scene.id,
        count = 0
    )
}

const val MAX_HTML_BYTES = 2 * 1024 * 1024
const val HTML_SCENE_PREFIX = "html:"

/** An uploaded HTML scene file in internal storage. */
data class HtmlSceneMeta(
    val id: String,
    val name: String,
    val fileName: String,
    val addedAt: Long
)

private fun customZikrFor(meta: HtmlSceneMeta, count: Int) = Zikr(
    id = "zikr_html_${meta.id}",
    arabic = ASTAGHFIRULLAH,
    translation = ASTAGHFIRULLAH_EN,
    target = DEFAULT_TARGET,
    sceneId = "$HTML_SCENE_PREFIX${meta.id}",
    count = count
)

/** Single storage entry point, so a later swap to DataStore/Room touches only this class. */
class ZikrStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("zikr", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("tally", Context.MODE_PRIVATE)

    fun load(): List<Zikr> {
        val migrated = prefs.getBoolean("migrated_garden", false)
        val legacyCount = legacy.getInt("count", 0)
        val list = defaultZikrs().map { z ->
            val saved = prefs.getInt("count_${z.id}", -1)
            val count = when {
                saved >= 0 -> saved
                !migrated && z.sceneId == "garden" -> legacyCount
                else -> 0
            }
            z.copy(count = count)
        }
        if (!migrated) prefs.edit().putBoolean("migrated_garden", true).apply()
        return list
    }

    fun saveCount(zikrId: String, count: Int) {
        prefs.edit().putInt("count_$zikrId", count).apply()
    }

    var lastSceneId: String?
        get() = prefs.getString("last_scene", null)
        set(value) {
            prefs.edit().putString("last_scene", value).apply()
        }

    /* ---------- uploaded HTML scenes ---------- */

    /** Native scenes first, then the user's uploaded ones, in upload order. */
    fun loadPages(): List<Zikr> = load() + customZikrs()

    fun customZikrs(): List<Zikr> = customScenes().map { meta ->
        customZikrFor(meta, countFor("zikr_html_${meta.id}"))
    }

    fun countFor(zikrId: String): Int = prefs.getInt("count_$zikrId", 0)

    fun customScenes(): List<HtmlSceneMeta> = runCatching {
        val arr = JSONArray(prefs.getString("custom_scenes", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HtmlSceneMeta(
                id = o.getString("id"),
                name = o.getString("name"),
                fileName = o.getString("fileName"),
                addedAt = o.optLong("addedAt")
            )
        }
    }.getOrDefault(emptyList())

    private fun saveCustomScenes(list: List<HtmlSceneMeta>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("fileName", m.fileName)
                    .put("addedAt", m.addedAt)
            )
        }
        prefs.edit().putString("custom_scenes", arr.toString()).apply()
    }

    fun addCustomScene(meta: HtmlSceneMeta) = saveCustomScenes(customScenes() + meta)

    fun renameCustomScene(id: String, name: String) {
        saveCustomScenes(customScenes().map { if (it.id == id) it.copy(name = name) else it })
    }

    fun removeCustomScene(id: String) {
        saveCustomScenes(customScenes().filterNot { it.id == id })
        prefs.edit().remove("count_zikr_html_$id").apply()
        deleteHtml(id)
    }

    private fun sceneDir(): File = File(context.filesDir, "scenes").apply { mkdirs() }

    fun htmlFile(id: String): File = File(sceneDir(), "$id.html")

    fun saveHtml(id: String, bytes: ByteArray): Boolean = runCatching {
        require(bytes.isNotEmpty() && bytes.size <= MAX_HTML_BYTES)
        htmlFile(id).writeBytes(bytes)
        true
    }.getOrDefault(false)

    fun readHtml(id: String): String? = runCatching {
        val file = htmlFile(id)
        if (file.exists() && file.length() in 1..MAX_HTML_BYTES.toLong()) {
            file.readText(Charsets.UTF_8)
        } else null
    }.getOrNull()

    fun deleteHtml(id: String) {
        runCatching { htmlFile(id).delete() }
    }
}
