package com.muddassir.safina

/**
 * The six scenes that ship inside the APK.
 *
 * Four were recovered from the user's own uploads (the WebView copies stored in
 * `/data/data/com.muddassir.safina/files/scenes/*.html`); two are the HTML files
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

    val all: List<BundledScene> = listOf(
        BundledScene("dawn_garden", "Dawn Garden", "defaults/dawn_garden.html", 0),
        BundledScene("deep_ocean", "Deep Ocean", "defaults/deep_ocean.html", 1),
        BundledScene("snow", "Snow", "defaults/snow.html", 2),
        BundledScene("dark_night", "Dark Night", "defaults/dark_night.html", 3),
        BundledScene("meadow", "Meadow", "defaults/garden.html", 4),
        BundledScene("starlight", "Starlight", "defaults/starlight.html", 5)
    )

    fun byKey(key: String?): BundledScene? = all.firstOrNull { it.key == key }

    fun idFor(key: String): String = "default_$key"
}
