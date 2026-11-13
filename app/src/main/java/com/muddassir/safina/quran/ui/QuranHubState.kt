package com.muddassir.safina.quran.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.muddassir.safina.quran.data.IslamicDateStore
import com.muddassir.safina.quran.data.QuranRepository
import com.muddassir.safina.quran.data.QuranStore
import com.muddassir.safina.quran.model.QuranVerse
import com.muddassir.safina.quran.worker.QuranNotifier
import com.muddassir.safina.quran.worker.QuranWorkScheduler
import com.muddassir.safina.quran.widget.QuranReminderWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The three views of the Quran screen. */
enum class QuranSheetTab { SURAH, BOOKMARKS, SEARCH }

/** The refresh presets offered in Quran settings. */
val VERSE_INTERVAL_OPTIONS = listOf(1, 3, 6, 12, 24)

/** Slider notch for the Arabic text size. */

/**
 * The Quran state Safina's card and reader share.
 *
 * This is a thin Compose state holder over the same [QuranRepository] the
 * original app used — no verse logic lives here. It mirrors the subset of the
 * original hub state that the ported search/reader UI reads, so those screens
 * are unchanged apart from the type name.
 */
class QuranHubState(context: Context) {

    private val appContext = context.applicationContext
    private val quranRepository = QuranRepository(appContext)
    private val islamicDateStore = IslamicDateStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val surahVerseCache = mutableMapOf<Int, List<QuranVerse>>()

    var verse by mutableStateOf<QuranVerse?>(null)
        private set
    var verseLoading by mutableStateOf(true)
        private set
    var isBookmarked by mutableStateOf(false)
        private set

    var canGoPrevious by mutableStateOf(false)
        private set
    var canGoNext by mutableStateOf(false)
        private set

    var quranSheetTab by mutableStateOf(QuranSheetTab.SURAH)
    var openSurahNumber by mutableStateOf<Int?>(null)

    var bookmarkKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var surahBookmarkKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    var refreshIntervalHours by mutableStateOf(QuranStore.DEFAULT_REFRESH_INTERVAL_HOURS)
        private set
    var arabicTextScale by mutableStateOf(QuranStore.DEFAULT_ARABIC_TEXT_SCALE)
        private set
    var themeWarmth by mutableStateOf(QuranStore.DEFAULT_THEME_WARMTH)
        private set
    var quranNotificationsEnabled by mutableStateOf(true)
        private set
    var islamicDateAdjustment by mutableStateOf(0)
        private set

    fun start() {
        refreshIntervalHours = quranRepository.getRefreshIntervalHours()
        arabicTextScale = quranRepository.getArabicTextScale()
        themeWarmth = quranRepository.getThemeWarmth()
        quranNotificationsEnabled = quranRepository.getQuranNotificationsEnabled()
        islamicDateAdjustment = islamicDateStore.adjustmentDays()
        bookmarkKeys = quranRepository.getBookmarks()
        surahBookmarkKeys = quranRepository.getSurahBookmarks()
        QuranWorkScheduler.ensureScheduled(appContext)

        scope.launch {
            // The current verse is instant; a first run has none yet, so the
            // initial download happens here rather than waiting for the worker.
            var loaded = withContext(Dispatchers.IO) {
                quranRepository.getCurrentVerse()
            }
            if (loaded == null) {
                val downloaded = withContext(Dispatchers.IO) {
                    quranRepository.ensureEnglishAndArabic()
                }
                if (downloaded) {
                    loaded = withContext(Dispatchers.IO) { quranRepository.pickRandomVerse() }
                }
            } else {
                loaded = withContext(Dispatchers.IO) {
                    quranRepository.backfillCurrentVerseTotals() ?: loaded
                }
            }
            verse = loaded
            verseLoading = false
            refreshBookmarkState()
            refreshNavAvailability()
        }

        // The WorkManager refresh writes the new verse to prefs while Safina may
        // already be open. Watch for that write so the collapsed card follows it.
        scope.launch {
            while (true) {
                delay(60_000)
                val stored = withContext(Dispatchers.IO) { quranRepository.getCurrentVerse() }
                val shown = verse
                if (stored != null && (shown == null ||
                        stored.surahNumber != shown.surahNumber ||
                        stored.ayahNumber != shown.ayahNumber ||
                        stored.text != shown.text)
                ) {
                    verse = stored
                    refreshBookmarkState()
                    refreshNavAvailability()
                }
            }
        }
    }

    /** Re-reads the persisted verse, e.g. after a background worker refresh. */
    fun reloadCurrentVerse() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { quranRepository.getCurrentVerse() }
                ?: return@launch
            verse = loaded
            refreshBookmarkState()
            refreshNavAvailability()
        }
    }

    fun cancel() = scope.cancel()

    fun refreshBookmarkState() {
        isBookmarked = verse?.let {
            quranRepository.isBookmarked(it.surahNumber, it.ayahNumber)
        } == true
    }

    private fun refreshNavAvailability() {
        val v = verse ?: run {
            canGoPrevious = false
            canGoNext = false
            return
        }
        scope.launch {
            val flags = withContext(Dispatchers.IO) {
                quranRepository.hasAdjacentVerse(v.surahNumber, v.ayahNumber, -1) to
                    quranRepository.hasAdjacentVerse(v.surahNumber, v.ayahNumber, +1)
            }
            canGoPrevious = flags.first
            canGoNext = flags.second
        }
    }

    fun pickNewVerse() {
        scope.launch {
            verseLoading = true
            val next = withContext(Dispatchers.IO) { quranRepository.pickRandomVerse() }
            if (next != null) {
                verse = next
                refreshBookmarkState()
                refreshNavAvailability()
                QuranReminderWidgetProvider.refreshAllWidgets(appContext)
                QuranNotifier.notifyNewVerse(appContext, next)
            }
            verseLoading = false
        }
    }

    fun goToAdjacentVerse(step: Int) {
        val current = verse ?: return
        scope.launch {
            val next = withContext(Dispatchers.IO) {
                quranRepository.getAdjacentVerse(
                    current.surahNumber,
                    current.ayahNumber,
                    step
                )
            }
            if (next != null) {
                verse = next
                refreshBookmarkState()
                refreshNavAvailability()
                QuranReminderWidgetProvider.refreshAllWidgets(appContext)
            }
        }
    }

    /** Opens [v] as the current verse (persisted, widget synced). */
    fun goToVerse(v: QuranVerse) {
        verse = v
        scope.launch {
            withContext(Dispatchers.IO) { quranRepository.saveCurrentVerse(v) }
            QuranReminderWidgetProvider.refreshAllWidgets(appContext)
            refreshNavAvailability()
            refreshBookmarkState()
        }
    }

    fun toggleBookmark() {
        val v = verse ?: return
        isBookmarked = quranRepository.toggleBookmark(v.surahNumber, v.ayahNumber)
        bookmarkKeys = quranRepository.getBookmarks()
    }

    fun changeArabicTextScale(scale: Float) {
        val clamped = scale.takeIf { it.isFinite() }
            ?.coerceIn(QuranStore.ARABIC_TEXT_SCALE_MIN, QuranStore.ARABIC_TEXT_SCALE_MAX)
            ?: return
        arabicTextScale = clamped
        quranRepository.setArabicTextScale(clamped)
    }

    fun changeThemeWarmth(warmth: Float) {
        val clamped = warmth.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: return
        themeWarmth = clamped
        quranRepository.setThemeWarmth(clamped)
    }

    fun changeInterval(context: Context, hours: Int) {
        refreshIntervalHours = hours
        quranRepository.setRefreshIntervalHours(hours)
        QuranWorkScheduler.reschedule(context)
    }

    fun setQuranNotifications(enabled: Boolean) {
        quranNotificationsEnabled = enabled
        quranRepository.setQuranNotificationsEnabled(enabled)
    }

    fun changeIslamicDateAdjustment(days: Int) {
        islamicDateAdjustment = days.coerceIn(-1, 1)
        islamicDateStore.setAdjustmentDays(islamicDateAdjustment)
    }

    // ── Quran search / reader / bookmarks ───────────────────────────

    suspend fun searchQuran(query: String): List<QuranVerse> =
        quranRepository.searchVerses(query)

    suspend fun bookmarkedVerses(): List<QuranVerse> = quranRepository.getBookmarkedVerses()

    suspend fun surahVerses(surahNumber: Int): List<QuranVerse> {
        surahVerseCache[surahNumber]?.let { return it }
        val verses = quranRepository.getSurahVerses(surahNumber)
        if (verses.isNotEmpty()) surahVerseCache[surahNumber] = verses
        return verses
    }

    fun openSurah(surahNumber: Int) {
        openSurahNumber = surahNumber
    }

    fun closeSurah() {
        openSurahNumber = null
    }

    fun readingPosition(surahNumber: Int): Int? =
        quranRepository.getReadingPosition(surahNumber)

    fun saveReadingPosition(surahNumber: Int, ayahNumber: Int) {
        quranRepository.setReadingPosition(surahNumber, ayahNumber)
    }

    fun toggleBookmarkAt(surahNumber: Int, ayahNumber: Int): Boolean {
        val added = quranRepository.toggleBookmark(surahNumber, ayahNumber)
        bookmarkKeys = quranRepository.getBookmarks()
        verse?.let { current ->
            if (current.surahNumber == surahNumber && current.ayahNumber == ayahNumber) {
                isBookmarked = added
            }
        }
        return added
    }

    fun toggleSurahBookmarkAt(surahNumber: Int): Boolean {
        val added = quranRepository.toggleSurahBookmark(surahNumber)
        surahBookmarkKeys = quranRepository.getSurahBookmarks()
        return added
    }

    fun removeSurahBookmark(surahNumber: Int) {
        quranRepository.removeSurahBookmark(surahNumber)
        surahBookmarkKeys = quranRepository.getSurahBookmarks()
    }

    fun isSurahStarred(surahNumber: Int): Boolean =
        surahNumber.toString() in surahBookmarkKeys

    val starredSurahs: List<Int>
        get() = surahBookmarkKeys.mapNotNull { it.toIntOrNull() }.sorted()

    fun removeBookmark(surahNumber: Int, ayahNumber: Int) {
        quranRepository.removeBookmark(surahNumber, ayahNumber)
        bookmarkKeys = quranRepository.getBookmarks()
        refreshBookmarkState()
    }

    fun isVerseBookmarked(surahNumber: Int, ayahNumber: Int): Boolean =
        "$surahNumber:$ayahNumber" in bookmarkKeys
}

/** One hub state per composition; loading starts and stops with it. */
@Composable
fun rememberQuranHubState(): QuranHubState {
    val context = LocalContext.current
    val state = remember { QuranHubState(context.applicationContext) }
    DisposableEffect(Unit) {
        state.start()
        onDispose { state.cancel() }
    }
    return state
}
