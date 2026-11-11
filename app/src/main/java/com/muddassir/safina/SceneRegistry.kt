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

/**
 * The six dhikr that ship with the app. They are ordinary cards: editable,
 * reorderable and deletable exactly like ones the user adds.
 */
fun defaultZikrs(): List<Zikr> = listOf(
    Zikr(
        id = "zikr_garden",
        arabic = "سُبْحَانَ اللّٰهِ",
        translation = "SubhanAllah · Glory be to Allah.",
        target = DEFAULT_TARGET,
        sceneId = "garden",
        count = 0
    ),
    Zikr(
        id = "zikr_winter",
        arabic = "الْحَمْدُ لِلّٰهِ",
        translation = "Alhamdulillah · All praise is for Allah.",
        target = DEFAULT_TARGET,
        sceneId = "winter",
        count = 0
    ),
    Zikr(
        id = "zikr_ocean",
        arabic = "اللّٰهُ أَكْبَرُ",
        translation = "Allahu Akbar · Allah is the Greatest.",
        target = DEFAULT_TARGET,
        sceneId = "ocean",
        count = 0
    ),
    Zikr(
        id = "zikr_night",
        arabic = ASTAGHFIRULLAH,
        translation = ASTAGHFIRULLAH_EN,
        target = DEFAULT_TARGET,
        sceneId = "night",
        count = 0
    ),
    Zikr(
        id = "zikr_tahlil",
        arabic = "لَا إِلَٰهَ إِلَّا اللّٰهُ",
        translation = "La ilaha illallah · There is no god worthy of worship except Allah.",
        target = DEFAULT_TARGET,
        sceneId = "night",
        count = 0
    ),
    Zikr(
        id = "zikr_salawat",
        arabic = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
        translation = "O Allah, send blessings upon Muhammad ﷺ.",
        target = DEFAULT_TARGET,
        sceneId = "winter",
        count = 0
    )
)

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

    /* ---------- dhikr cards (the dashboard list) ---------- */

    /**
     * The user's editable dhikr cards, in display order. On first run the previous
     * scene-per-page list is adopted as cards, so saved counts keep working.
     */
    fun loadCards(): List<Zikr> {
        val raw = prefs.getString("cards", null)
        if (raw == null) {
            val initial = load()
            writeCards(initial)
            return initial
        }
        val parsed = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val id = o.getString("id")
                Zikr(
                    id = id,
                    arabic = o.optString("arabic", ASTAGHFIRULLAH),
                    translation = o.optString("translation", ASTAGHFIRULLAH_EN),
                    target = o.optInt("target", DEFAULT_TARGET).coerceAtLeast(1),
                    sceneId = o.optString("sceneId", Scenes.all.first().id),
                    count = countFor(id)
                )
            }
        }.getOrNull()
        return if (parsed == null) {
            val initial = load()
            writeCards(initial)
            initial
        } else parsed
    }

    fun saveCards(cards: List<Zikr>) = writeCards(cards)

    fun addCard(card: Zikr) {
        writeCards(loadCards() + card)
        saveCount(card.id, card.count)
    }

    fun updateCard(card: Zikr) {
        writeCards(loadCards().map { if (it.id == card.id) card else it })
    }

    fun deleteCard(id: String) {
        writeCards(loadCards().filterNot { it.id == id })
        prefs.edit().remove("count_$id").apply()
    }

    fun moveCard(from: Int, to: Int) {
        val list = loadCards().toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        list.add(to, list.removeAt(from))
        writeCards(list)
    }

    private fun writeCards(cards: List<Zikr>) {
        val arr = JSONArray()
        cards.forEach { z ->
            arr.put(
                JSONObject()
                    .put("id", z.id)
                    .put("arabic", z.arabic)
                    .put("translation", z.translation)
                    .put("target", z.target)
                    .put("sceneId", z.sceneId)
            )
        }
        prefs.edit().putString("cards", arr.toString()).apply()
    }

    /* ---------- preferences ---------- */

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_on", true)
        set(value) { prefs.edit().putBoolean("sound_on", value).apply() }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibrate_on", true)
        set(value) { prefs.edit().putBoolean("vibrate_on", value).apply() }

    /** null means "follow the system"; a value is the user's explicit choice. */
    var darkModeOverride: Boolean?
        get() = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove("dark_mode") else editor.putBoolean("dark_mode", value)
            editor.apply()
        }

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
        // Cards using this scene fall back to the first built-in scene instead of breaking.
        val fallback = Scenes.all.first().id
        val updated = loadCards().map {
            if (it.sceneId == "$HTML_SCENE_PREFIX$id") it.copy(sceneId = fallback) else it
        }
        writeCards(updated)
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
        runCatching { thumbnailFile(id).delete() }
    }

    /* ---------- scene thumbnails (a real snapshot of the scene) ---------- */

    fun thumbnailFile(id: String): File = File(sceneDir(), "$id.thumb.png")

    fun hasThumbnail(id: String): Boolean = thumbnailFile(id).length() > 0L

    fun saveThumbnail(id: String, bytes: ByteArray): Boolean = runCatching {
        require(bytes.isNotEmpty())
        thumbnailFile(id).writeBytes(bytes)
        true
    }.getOrDefault(false)

    fun deleteThumbnail(id: String) {
        runCatching { thumbnailFile(id).delete() }
    }
}
