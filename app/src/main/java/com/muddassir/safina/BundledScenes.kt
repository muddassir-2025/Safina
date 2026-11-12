package com.muddassir.safina

/**
 * The six scenes that ship inside the APK.
 *
 * Four were recovered from the user's own uploads (the WebView copies stored in
 * `/data/data/com.muddassir.safina/files/scenes/` as `<id>.html`); two are the HTML files
 * that were already in `app/src/main/assets` (`garden_reference.html` and
 * `test_dots.html`). They are copied into local storage on first launch so they
 * can be renamed, replaced, and reset like any other scene.
 */
data class BundledScene(
    val key: String,
    val title: String,
    val asset: String,
    val sortOrder: Int
)

object BundledScenes {
    const val EXAMPLE_ASSET = "defaults/garden.html"

    /**
     * The two HTML scenes that ship in the Default tab. Garden, Winter, Ocean and
     * Night are drawn natively (see [Scenes]) and are added by the studio itself.
     */
    val all: List<BundledScene> = listOf(
        BundledScene("deep_ocean", "Deep Ocean", "defaults/deep_ocean.html", 0),
        BundledScene("dark_night", "Dark Night", "defaults/dark_night.html", 1)
    )

    /** Bundled HTML scenes from older builds, cleared out of the Default tab. */
    val retiredKeys: Set<String> = setOf("meadow", "snow", "dawn_garden", "starlight")

    fun byKey(key: String?): BundledScene? = all.firstOrNull { it.key == key }

    fun idFor(key: String): String = "default_$key"
}
