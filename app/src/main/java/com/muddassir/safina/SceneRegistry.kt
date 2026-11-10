package com.muddassir.safina

import android.content.Context

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

/** Single storage entry point, so a later swap to DataStore/Room touches only this class. */
class ZikrStore(context: Context) {
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
}
