package com.muddassir.safina.quran.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.muddassir.safina.quran.data.QuranJsonParser
import com.muddassir.safina.quran.data.QuranStore
import com.muddassir.safina.quran.ui.QuranFontFamily
import com.muddassir.safina.quran.ui.quranFontSize
import com.muddassir.safina.quran.ui.quranLineHeight
import com.muddassir.safina.quran.ui.VerseAudioPlayer
import com.muddassir.safina.quran.ui.VerseAudioProgressBar
import com.muddassir.safina.quran.ui.VerseAudioStatus
import com.muddassir.safina.quran.ui.verseAudioUrl
import com.muddassir.safina.quran.data.verseReference
import com.muddassir.safina.quran.model.QuranVerse
import com.muddassir.safina.quran.util.copyVerseToClipboard
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.muddassir.safina.R


/**
 * The notch the Arabic text size snaps to: 5%, so the percentage on screen lands
 * on a round number while every size in the range stays reachable.
 *
 * Deliberately NOT a short list of presets: "bigger" is not four discrete
 * answers, and a reader who wants 115% should be able to have 115%.
 */
private const val QURAN_TEXT_SCALE_STEP = 0.05f

/**
 * The Arabic text controls: one setting, in one block.
 *
 * One composable for both places it appears — the surah reader (§2) and the
 * settings sheet — because a reader who set a size in one place and finds a
 * different number in the other has been told the app does not know its own
 * state.
 *
 * [onDone] is null where the surrounding screen already has a way out (the
 * settings sheet); the reader passes one so the panel can be closed without
 * hunting for the icon again.
 */
@Composable
private fun ArabicTextControls(
    state: QuranHubState,
    onDone: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.quran_settings_arabic_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // The number is the whole feedback for a control with no stops to
                // count: without it a reader cannot tell 115% from 120%.
                Text(
                    text = stringResource(
                        R.string.quran_text_size_percent,
                        (state.arabicTextScale * 100).roundToInt()
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onDone != null) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.quran_done))
                    }
                }
            }
            SleekSlider(
                value = state.arabicTextScale,
                valueRange = QuranStore.ARABIC_TEXT_SCALE_MIN..QuranStore.ARABIC_TEXT_SCALE_MAX,
                step = QURAN_TEXT_SCALE_STEP,
                onValueChange = { state.changeArabicTextScale(it) }
            )
        }
    }
}

/**
 * A slider drawn as a hairline, with a small dot for a thumb.
 *
 * The default Material slider is furniture: a 16dp track and a 20dp thumb, sized
 * for a settings page. This one sits in a reader's top bar over scripture, where
 * the point is the value, not the control — so the track is 3dp and the thumb
 * 12dp. The TOUCH target is still the slider's own 48dp, which is what keeps it
 * usable with a thumb; only the drawing is slim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleekSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit
) {
    val fraction = ((value - valueRange.start) /
        (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        // Whole notches above the start, so the values a reader passes through
        // are the same round percentages the label shows.
        steps = (((valueRange.endInclusive - valueRange.start) / step).roundToInt() - 1)
            .coerceAtLeast(0),
        thumb = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    )
}

/**
 * One tab of the Quran screen's top row: Search / Surah / Bookmarks.
 *
 * A text segment rather than a Material `FilterChip`, for two reasons that both
 rather than a filter: a chip reserves
 * space for a leading check mark (so the row jitters as the selection moves and
 * the three never line up), and chips size themselves to their labels, which
 * makes "Search" narrow and "Bookmarks" wide. Equal thirds that re-colour when
 * picked read as one control, which is what it is.
 */
@Composable
private fun QuranSegment(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            // No fillMaxWidth: a weighted tab already constrains the Surface,
            // while a wrap-content action chip must stay its own width. Filling
            // here made every chip as wide as the whole row, pushing Bookmark
            // off the right edge of the screen.
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The Quran screen's three views (§1–§3), held in [QuranHubState.quranSheetTab]
 * so switching between them — or opening a surah and coming back — never
 * re-parses anything, and so the screen returns where the reader left it.
 *
 * Full-screen search (opened from the Quran tab's search icon): one search field
 * above three tabs.
 *
 * * **Search** — verses matching the field, debounced, matches highlighted.
 * * **Surah** — all 114 surahs, filtered by the same field; tapping one opens the
 *   *continuous* reader for that surah, where the field filters within the surah
 *   instead. The old behaviour jumped to a single verse and left the reader
 *   stepping through the surah one screen at a time; a surah is read by
 *   scrolling it (§2).
 * * **Bookmarks** — every verse this device has saved, filtered by the same
 *   field, with per-item removal.
 *
 * The single field is the point: the three tabs are three questions about the
 * same text, and a reader who types "mercy" then taps Surah expects the filter to
 * follow them rather than start over.
 *
 * Implemented as a full-screen dialog so the lists get the whole screen instead
 * of fighting a bottom sheet for space with the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSearchScreen(state: QuranHubState, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<QuranVerse>?>(null) }
    var bookmarks by remember { mutableStateOf<List<QuranVerse>?>(null) }
    // Verse awaiting removal confirmation (null = no dialog). Removing a bookmark
    // is destructive, so the list always asks Remove / Cancel first.
    var pendingRemove by remember { mutableStateOf<QuranVerse?>(null) }
    // Surah awaiting removal confirmation, from either its star or the bookmarks
    // tab's Surah list (null = no dialog).
    var pendingSurahRemove by remember { mutableStateOf<Int?>(null) }
    // A verse opened from a result or bookmark card. Held here rather than in
    // the list so Back returns to the very same list, query and scroll intact.
    var selectedVerse by remember { mutableStateOf<QuranVerse?>(null) }
    var searching by remember { mutableStateOf(false) }
    // The reader's field is behind its search icon, and starts closed (§2).
    //
    // The three tabs are three QUESTIONS, so a field on screen is what they are:
    // the reader arrives and types. A surah is a piece of scripture, and opening
    // it behind a keyboard with the text pushed up out of the way is not reading
    // it — so the icon is there when it is wanted, and the Quran is what is on
    // screen when it is not.
    var readerSearchOpen by remember { mutableStateOf(false) }
    // Hands-free reading (§1): how fast the open surah scrolls itself, and whether
    // its speed menu is showing. Owned here rather than inside the reader because
    // the ICON lives in this row of chrome — one owner for a control and its
    // state, or the check mark and the page can disagree.
    var readerSpeed by remember { mutableStateOf(QuranScrollSpeed.OFF) }
    var readerSpeedMenu by remember { mutableStateOf(false) }
    // A signal rather than a hoisted list state: the list belongs to the open
    // surah, and bumping this asks that reader to animate back to its first row.
    var readerScrollToTop by remember { mutableStateOf(0) }
    // §2: the Arabic text-size menu in the reader's chrome, beside the search
    // icon. Owned here for the same reason the speed menu is: one owner for a
    // control and its state.
    var readerTextPanel by remember { mutableStateOf(false) }
    // §2: the surah's identity block and the Arabic|English switch are PINNED
    // above the text, so they spend their height on every screenful of a read
    // that may be a hundred of them. This folds them away. The surah's name stays
    // in the top bar above, so folding gives up height, not the sense of where
    // you are.
    var readerHeaderOpen by remember { mutableStateOf(true) }
    val tab = state.quranSheetTab
    val openSurah = state.openSurahNumber
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Debounced search; the other two tabs (and a blank query) show their own
    // content and need no request.
    LaunchedEffect(query, tab) {
        if (tab != QuranSheetTab.SEARCH || query.isBlank()) {
            searching = false
            results = null
            return@LaunchedEffect
        }
        searching = true
        delay(250)
        results = withContext(Dispatchers.IO) { state.searchQuran(query) }
        searching = false
    }

    // The bookmarks, re-read whenever the tab is opened and whenever a bookmark
    // changes anywhere in the app. [QuranHubState.bookmarkKeys] is the single
    // source for "what is bookmarked", so this list and the top-bar icon cannot
    // disagree (§3, §10).
    LaunchedEffect(tab, state.bookmarkKeys) {
        if (tab == QuranSheetTab.BOOKMARKS) {
            bookmarks = withContext(Dispatchers.IO) { state.bookmarkedVerses() }
        }
    }

    // Autofocus + open the keyboard wherever there IS a field: on entry to the
    // tabbed view, and when the reader's search icon opens one. The short delay
    // lets the dialog window attach before the IME is asked to show — calling
    // show() in the same frame is silently dropped on some devices.
    //
    // Guarded rather than unconditional because the reader opens with no field at
    // all, and requesting focus on a requester that is not attached to anything
    // is an error rather than a no-op.
    // Focus only follows an explicit request: the reader's search icon inside a
    // surah. Opening the Quran screen must not raise the keyboard over the tabs;
    // the field is there when they tap it.
    LaunchedEffect(readerSearchOpen) {
        if (!readerSearchOpen) return@LaunchedEffect
        focusRequester.requestFocus()
        delay(150)
        keyboard?.show()
    }

    // Opening a different surah starts with the text, not with the last surah's
    // search term still narrowing it (§10).
    LaunchedEffect(openSurah) {
        readerSearchOpen = false
        // A new surah starts still. Resuming a scroll the reader started in the
        // LAST surah would move text they have not read a word of yet.
        readerSpeed = QuranScrollSpeed.OFF
        readerSpeedMenu = false
        readerTextPanel = false
        // And it starts expanded: opening a surah you have not opened before with
        // its name and translation already folded away is being dropped into the
        // middle of something.
        readerHeaderOpen = true
        // And the term itself goes with it. Leaving it behind meant the NEXT time
        // the field was opened it came back pre-filled, narrowing a surah the
        // reader had not typed anything into.
        if (openSurah == null) query = ""
    }

    // A term belongs to the tab it was typed on (§10).
    //
    // Switching tabs used to carry it across, on the theory that a reader who
    // typed "mercy" and then tapped Surah meant to filter those too. In practice
    // the three tabs answer three different questions — words in the Quran, a
    // surah by name, what I saved — and a term from one of them arrives as a
    // filter the reader never asked for: an empty Surah list, or a Bookmarks tab
    // insisting there is nothing to show. Each tab opens clean.
    LaunchedEffect(tab) { query = "" }

    Dialog(
        onDismissRequest = onDismiss,
        // Full width + edge-to-edge so imePadding() below actually receives the
        // IME insets and the results list shrinks above the keyboard.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = selectedVerse == null && openSurah == null
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(Modifier.fillMaxSize()) {
            // Registered inside the dialog's own window, so Back leaves one
            // layer at a time: verse detail, then an open surah, then the screen.
            BackHandler(enabled = selectedVerse != null || openSurah != null) {
                if (selectedVerse != null) selectedVerse = null else state.closeSurah()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // ── Top bar: the back arrow, then the three tabs (§1) ──
                //
                // The arrow gets its own line's worth of height but shares the row
                // with the tabs, so the tabs sit where a reader's eye already is
                // and the field below gets the full width instead of being squeezed
                // between two controls. The tabs are equal thirds of the space the
                // arrow leaves — "centred" on a phone is this, and three unequal
                // chips would fit badly as soon as the names changed length.
                //
                // While a surah is open the row carries the surah's name instead:
                // there is nothing to switch to from inside a surah (the back
                // arrow is how you leave, §10), and the name is what the reader
                // needs to know at that point.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back leaves one layer at a time: out of a surah first, into
                    // the tabs it was opened from (§10), then out of the screen.
                    IconButton(onClick = {
                        if (openSurah != null) state.closeSurah() else onDismiss()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.quran_search_back)
                        )
                    }
                    if (openSurah == null) {
                        Row(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuranSegment(
                                selected = tab == QuranSheetTab.SURAH,
                                label = stringResource(R.string.quran_search_mode_surah),
                                modifier = Modifier.weight(1f),
                                onClick = { state.quranSheetTab = QuranSheetTab.SURAH }
                            )
                            QuranSegment(
                                selected = tab == QuranSheetTab.BOOKMARKS,
                                label = stringResource(R.string.quran_search_mode_bookmarks),
                                modifier = Modifier.weight(1f),
                                onClick = { state.quranSheetTab = QuranSheetTab.BOOKMARKS }
                            )
                            QuranSegment(
                                selected = tab == QuranSheetTab.SEARCH,
                                label = stringResource(R.string.quran_search_mode_search),
                                modifier = Modifier.weight(1f),
                                onClick = { state.quranSheetTab = QuranSheetTab.SEARCH }
                            )
                        }
                    } else {
                        Text(
                            text = QuranJsonParser.surahName(openSurah),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        // In a surah that can be hundreds of rows long, getting
                        // back to its beginning should not be a long fling.
                        IconButton(onClick = { readerScrollToTop++ }) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = stringResource(
                                    R.string.quran_reader_scroll_top
                                )
                            )
                        }

                        // §2: fold the block above the text away. Before the
                        // search icon because it is the FIRST thing a reader wants
                        // in a long surah — a larger page — and search is what they
                        // want in a specific one.
                        IconButton(onClick = { readerHeaderOpen = !readerHeaderOpen }) {
                            Icon(
                                if (readerHeaderOpen) Icons.Filled.UnfoldLess
                                else Icons.Filled.UnfoldMore,
                                contentDescription = stringResource(
                                    if (readerHeaderOpen) R.string.quran_reader_header_hide
                                    else R.string.quran_reader_header_show
                                ),
                                tint = if (readerHeaderOpen) {
                                    LocalContentColor.current
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }

                        // §10: searching INSIDE the surah, kept — as an icon that
                        // opens the field over the text rather than a field that
                        // holds a line of the screen open the whole time.
                        IconButton(onClick = {
                            readerSearchOpen = !readerSearchOpen
                            if (!readerSearchOpen) {
                                query = ""
                                keyboard?.hide()
                            }
                            // Typing inside a surah is not reading it: the page
                            // must not keep sliding along under a keyboard.
                            if (readerSearchOpen) readerSpeed = QuranScrollSpeed.OFF
                        }) {
                            Icon(
                                if (readerSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = stringResource(R.string.quran_reader_search)
                            )
                        }

                        // §2: the Arabic text size and spacing, immediately beside
                        // the search icon — the other things a reader reaches for
                        // while inside a surah. A panel of sliders, not a list of
                        // steps: "bigger" and "more space" are continuous
                        // quantities, and a slider shows where the reader is and
                        // how much room is left in one glance, which a menu of
                        // Larger / Smaller entries never does. It costs the row one
                        // icon, next to the name, the fold, search and speed.
                        IconButton(onClick = { readerTextPanel = !readerTextPanel }) {
                            Icon(
                                Icons.Filled.FormatSize,
                                contentDescription = stringResource(
                                    R.string.quran_arabic_text_appearance
                                ),
                                // Accent-tinted while the controls are showing, like
                                // the speed icon: the row says what is open.
                                tint = if (readerTextPanel) MaterialTheme.colorScheme.primary
                                else LocalContentColor.current
                            )
                        }

                        // §1: hands-free reading. Beside the search icon, in the
                        // same row the reader is already looking at, and tinted
                        // while it is running — a page that drifts on its own with
                        // nothing on screen saying why is a page that looks broken.
                        Box {
                            IconButton(onClick = { readerSpeedMenu = true }) {
                                Icon(
                                    Icons.Filled.Speed,
                                    contentDescription = stringResource(
                                        R.string.quran_reader_auto_scroll
                                    ),
                                    tint = if (readerSpeed == QuranScrollSpeed.OFF) {
                                        LocalContentColor.current
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = readerSpeedMenu,
                                onDismissRequest = { readerSpeedMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.quran_scroll_speed),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    // A heading, not a choice: the paces below are
                                    // the menu, and Off is one of them rather than
                                    // a separate switch.
                                    enabled = false,
                                    onClick = {}
                                )
                                QuranScrollSpeed.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.label)) },
                                        trailingIcon = {
                                            if (option == readerSpeed) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            readerSpeedMenu = false
                                            readerSpeed = option
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── The field, on its own line under them ──
                //
                // Hidden in the reader until its search icon is tapped, and
                // cleared on the way out so the next surah opens whole.
                if (openSurah == null || readerSearchOpen) {
                OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                stringResource(
                                    when {
                                        // In the reader the field finds verses
                                        // inside the open surah, which is the only
                                        // thing it can usefully do there.
                                        openSurah != null -> R.string.quran_search_hint_in_surah
                                        tab == QuranSheetTab.SURAH -> R.string.quran_search_hint_surah
                                        tab == QuranSheetTab.BOOKMARKS -> R.string.quran_bookmarks_search_hint
                                        else -> R.string.quran_search_hint
                                    }
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.quran_search_clear)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
                    )
                }

                // §2: the Arabic text controls, in the page rather than over it —
                // the same place the search field appears, and for the same
                // reason: a control that opens over the scripture is a control
                // that hides the thing it is changing, and a floating panel also
                // reads as something the app forgot to close. Opening these pushes
                // the surah down, so the reader sees each change on the verse.
                if (openSurah != null && readerTextPanel) {
                    ArabicTextControls(
                        state = state,
                        onDone = { readerTextPanel = false }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(4.dp))

                // A surah is open: the reader takes the whole screen below the
                // field, whichever tab it was opened from.
                if (openSurah != null) {
                    SurahReader(
                        state = state,
                        surahNumber = openSurah,
                        query = query,
                        speed = readerSpeed,
                        onSpeedChange = { readerSpeed = it },
                        headerOpen = readerHeaderOpen,
                        scrollToTop = readerScrollToTop
                    )
                    return@Column
                }

                when (tab) {
                    QuranSheetTab.SURAH -> SurahBrowseList(
                        state = state,
                        query = query
                    )

                    QuranSheetTab.BOOKMARKS -> BookmarksTab(
                        state = state,
                        query = query,
                        bookmarks = bookmarks,
                        onOpenVerse = { selectedVerse = it },
                        onRemove = { pendingRemove = it },
                        onRemoveSurah = { pendingSurahRemove = it }
                    )

                    QuranSheetTab.SEARCH -> {
                        // Capture the state once into an immutable local: `results`
                        // is a mutable state that becomes null when the query is
                        // cleared, and the lazy list content below is evaluated
                        // AFTER this `when` is chosen — reading `results!!` there
                        // would NPE on that transition.
                        val list = results
                        when {
                            searching && list == null -> Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.quran_search_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            list == null -> Text(
                                text = stringResource(R.string.quran_search_idle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            )
                            list.isEmpty() -> Text(
                                text = stringResource(R.string.quran_search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            )
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.quran_search_results_count,
                                            list.size,
                                            list.size
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Refining the query keeps previous results
                                    // visible; a small spinner shows the refresh.
                                    if (searching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(list, key = { "${it.surahNumber}:${it.ayahNumber}" }) { verse ->
                                        VerseSearchRow(
                                            verse = verse,
                                            highlight = query,
                                            onClick = {
                                                keyboard?.hide()
                                                selectedVerse = verse
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            selectedVerse?.let { detail ->
                QuranVerseDetailOverlay(
                    verse = detail,
                    state = state,
                    onBack = { selectedVerse = null },
                    onSetCurrent = {
                        state.goToVerse(detail)
                        selectedVerse = null
                        onDismiss()
                    }
                )
            }
            }
        }
    }

    // ── Remove confirmation (Remove / Cancel) ──────────────────────
    //
    // Lives on the screen rather than in the bookmarks list so the dialog is not
    // thrown away with the row it belongs to: the removed row is recomposed out
    // of the list the moment the bookmark changes, and the confirmation must
    // outlive that recomposition to be answered.
    pendingRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text(stringResource(R.string.quran_bookmarks_remove_confirm_title)) },
            text = { Text(stringResource(R.string.quran_bookmarks_remove_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    state.removeBookmark(target.surahNumber, target.ayahNumber)
                    bookmarks = bookmarks?.filterNot {
                        it.surahNumber == target.surahNumber &&
                            it.ayahNumber == target.ayahNumber
                    } ?: emptyList()
                    pendingRemove = null
                }) {
                    Text(
                        text = stringResource(R.string.quran_bookmarks_remove_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text(stringResource(R.string.quran_cancel))
                }
            }
        )
    }

    // The same question for a starred surah, and the same two answers. Only the
    // bookmarks list asks it: the star on a browse card is a toggle the reader
    // just pressed on purpose, and asking there would put a dialog between a tap
    // and the thing it did.
    pendingSurahRemove?.let { number ->
        AlertDialog(
            onDismissRequest = { pendingSurahRemove = null },
            title = { Text(stringResource(R.string.quran_surah_bookmarks_remove_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.quran_surah_bookmarks_remove_confirm_text,
                        QuranJsonParser.surahName(number)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.removeSurahBookmark(number)
                    pendingSurahRemove = null
                }) {
                    Text(
                        text = stringResource(R.string.quran_bookmarks_remove_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSurahRemove = null }) {
                    Text(stringResource(R.string.quran_cancel))
                }
            }
        )
    }
}

/**
 * The continuous surah reader (§2): one surah, one scroll, all of it.
 *
 * ## Why it replaces the verse-by-verse flow
 *
 * The old path opened a surah's *first verse* and then moved through the rest
 * with Previous / Next — so reading Al-Kahf was 110 screens and 110 taps, each
 * one a fresh lookup, and the reader could never see where they were in the
 * surah. A surah is a text you read by scrolling, which is what this is: the
 * whole surah in one lazy list, each verse carrying the same bookmark and copy
 * actions it has everywhere else.
 *
 * ## The field still does something
 *
 * Typing filters WITHIN the surah — matching the English text or the reference
 * ("18:10") — rather than searching the whole Quran under a heading that says
 * otherwise. A reader who wants the whole Quran taps back to the Search tab.
 */
/** The two ways a surah is read: the Arabic, or the translation (§3). */
private enum class QuranReadMode { ARABIC, ENGLISH }

/**
 * How fast the reader scrolls itself, once the reader has asked it to.
 *
 * A rate rather than a "slow / normal / fast" multiplier: what a person judges
 * is how long a line takes to pass, and only a rate says that. The three paces
 * are about a line a second at the bottom end and roughly two at the top, which
 * is already faster than anybody reads — the fast end is for finding a place,
 * not for following the text.
 *
 * [OFF] is a member rather than a separate flag so that stopping and choosing
 * are ONE control: the icon opens a list, the list is where "stop" lives, and
 * there is no second button to explain.
 */
private enum class QuranScrollSpeed(val dpPerSecond: Float, @param:StringRes val label: Int) {
    OFF(0f, R.string.quran_scroll_off),
    SLOW(15f, R.string.quran_scroll_slow),
    NORMAL(28f, R.string.quran_scroll_normal),
    FAST(48f, R.string.quran_scroll_fast),
}

/**
 * Rows the reader's list emits before the first verse.
 *
 * One — the basmala. Named rather than written as a literal in two places because
 * the restore and the save both convert between a verse's place in the surah and
 * its index in the list, and two `1`s are how those two would come to disagree.
 */
private const val SURAH_HEADER_ITEMS = 1

/**
 * How long the list must sit still before the reading position is written.
 *
 * Long enough that a fling through forty verses is one position and one prefs
 * commit; short enough that a reader who stops and puts the phone down has
 * already had it saved.
 */
private const val POSITION_SETTLE_MS = 700L

/**
 * The row a saved reading position sits at, or null when there is nothing to
 * restore (§2).
 *
 * Resolved by AYAH, not by counting: the saved value is a verse number, the list
 * is addressed by row index, and the two differ by the basmala above the first
 * ayah. An ayah the surah does not have — a position saved against a different
 * edition, or a cache that has since been replaced — returns null rather than
 * landing somewhere arbitrary.
 */
internal fun readingPositionIndex(verses: List<QuranVerse>, savedAyah: Int?): Int? {
    val ayah = savedAyah ?: return null
    val index = verses.indexOfFirst { it.ayahNumber == ayah }
    if (index < 0) return null
    return index + SURAH_HEADER_ITEMS
}

/**
 * The ayah at the top of the page when the list is scrolled to [listIndex].
 *
 * Null for the rows above the first verse — the basmala is not an ayah of this
 * surah, so having scrolled it off is not having read verse 1 — and for an index
 * past the end, which a list that has just been emptied reports.
 */
internal fun readingPositionAyah(verses: List<QuranVerse>, listIndex: Int): Int? =
    verses.getOrNull(listIndex - SURAH_HEADER_ITEMS)?.ayahNumber

/**
 * The surah reader (§1–§10): one continuous scroll of scripture.
 *
 * It used to be a column of rounded CARDS, one per verse, each with its own
 * reference line and copy/bookmark buttons — which is a list of verses, not a
 * surah. Reading Quran is continuous: the eye moves down the page through the
 * text, and four borders per screen, a repeated reference row and two buttons
 * per verse interrupt exactly that. So there are no cards here at all: the text
 * sits directly on the page, separated by space and typography, and the per-verse
 * actions appear under the verse they belong to when it is tapped (§8).
 *
 * There is no Previous/Next either (§6). Those exist in the one-verse dashboard
 * screen, where a single ayah is the unit; here the whole surah is loaded at once
 * and the reader moves through it by scrolling, which is what the app's own
 * dashboard already does.
 *
 * What is NOT changed: the data source, the parse, the search, the bookmark store
 * and the copy action are all the ones that were already here (§11).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurahReader(
    state: QuranHubState,
    surahNumber: Int,
    query: String,
    /** §2: whether the banner above the text is showing. Owned by the top bar. */
    headerOpen: Boolean,
    /**
     * How fast the text should scroll itself, owned by the top bar's speed menu
     * above this reader rather than here — the control and the state belong to
     * the same row of chrome, and splitting them across two composables is how a
     * check mark ends up disagreeing with what the page is doing.
     */
    speed: QuranScrollSpeed,
    /** Called when the reader's own hand, or the end of the surah, stops it. */
    onSpeedChange: (QuranScrollSpeed) -> Unit,
    /** Bumped by the top bar's jump-to-top icon; 0 means no request yet. */
    scrollToTop: Int
) {
    var verses by remember(surahNumber) { mutableStateOf<List<QuranVerse>?>(null) }
    // Arabic first: this is the Quran, and the translation is what a reader turns
    // to when they want it. The mode is not persisted between surahs on purpose —
    // opening a surah fresh is opening the Quran.
    var mode by remember(surahNumber) { mutableStateOf(QuranReadMode.ARABIC) }
    // Which verse's actions are showing, by key. One at a time: a second tap
    // moves them rather than opening a second set.
    var actionsFor by remember(surahNumber) { mutableStateOf<String?>(null) }
    var matchCursor by remember(surahNumber) { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current

    // ── Listen: one verse at a time, streamed ─────────────────────-
    //
    // ONE player for the whole surah, not one per row: a surah can hold 286
    // verses, and a decoder per row would be 286 decoders nobody asked for. The
    // verse being played is tracked by key, so the row that is playing can show
    // Pause while every other row still offers Listen. The player is released
    // when the surah (or the screen) goes away, so a recitation is never left
    // running behind the page. Nothing is downloaded — see [VerseAudioPlayer].
    val verseAudio = remember { VerseAudioPlayer() }
    var audioVerseKey by remember(surahNumber) { mutableStateOf<String?>(null) }
    DisposableEffect(surahNumber) {
        onDispose { verseAudio.release() }
    }
    LaunchedEffect(verseAudio.status) {
        if (verseAudio.status == VerseAudioStatus.FAILED) {
            Toast.makeText(context, R.string.quran_verse_listen_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // §1: how far through the surah the reader has scrolled. Derived, and read in
    // the DRAW pass below, so that a value which changes on every frame of a
    // scroll invalidates three pixels of bar rather than the screenful of text it
    // is measuring.
    val progress = remember(listState) { derivedStateOf { listState.readFraction() } }
    val railAccent = MaterialTheme.colorScheme.primary
    val railTrack = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)

    // ── Where the reader left off (§2) ──
    //
    // Three steps rather than one, because two things have to be true before the
    // page can be moved: the position must be off disk, and the surah must be
    // parsed. [positionLoaded] is the first, [all] the second. [restored] is the
    // third and the reason the save below waits for it — writing the position back
    // before the restore has moved the list would save the top of the surah over
    // the very position being resumed at.
    var savedAyah by remember(surahNumber) { mutableStateOf<Int?>(null) }
    var positionLoaded by remember(surahNumber) { mutableStateOf(false) }
    var restored by remember(surahNumber) { mutableStateOf(false) }

    // Parsed once per surah per process (the state caches it), off the main
    // thread — a 286-verse surah is a real parse.
    LaunchedEffect(surahNumber) {
        verses = withContext(Dispatchers.IO) { state.surahVerses(surahNumber) }
    }

    // The opening line, taken from the edition's own 1:1 rather than typed out
    // here. Typing it would mean re-deciding its harakat and its waqf mark by
    // hand on the one line in the surah that is not a numbered ayah, and the
    // edition already states it — for Uthmani Hafs that is
    // "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", wasla and all. Blank when the Arabic
    // is not cached, in which case the reader opens without it: a missing line
    // is better than an invented one. Al-Faatiha itself is cached after the
    // first reader opens.
    var basmala by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        basmala = withContext(Dispatchers.IO) {
            state.surahVerses(1).firstOrNull { it.ayahNumber == 1 }?.arabicText.orEmpty()
        }
    }

    val all = verses
    val q = query.trim()
    // The VERSES that match, not a filtered list: the surah stays whole and the
    // matches are marked in it, because a reader searching inside a surah is
    // looking for a place in the text, not a separate list of extracts (§10).
    val matches = remember(all, q) {
        val list = all ?: return@remember emptyList()
        matchingVerseIndices(list, q)
    }

    // A new term starts the count over, so "next" means the first match of what is
    // in the box rather than a position left over from the previous word.
    LaunchedEffect(q, all) { matchCursor = 0 }

    // A prefs read, so off the composition thread: a synchronous read here would
    // make opening a surah wait on the disk for one integer.
    LaunchedEffect(surahNumber) {
        savedAyah = withContext(Dispatchers.IO) { state.readingPosition(surahNumber) }
        positionLoaded = true
    }

    // ── Hands-free reading (§1) ──
    //
    // Per FRAME rather than per tick of a timer, and from the time each frame
    // actually took: a fixed `delay` drifts against the display, and a scroll
    // driven by "how many milliseconds I meant to wait" runs at a different speed
    // on a 120Hz phone than on a 60Hz one. The remainder is carried rather than
    // dropped, because a slow pace is well under one pixel per frame — truncating
    // it every time would leave the page perfectly still at exactly the pace a
    // reader is most likely to want.
    //
    // It stops itself at the end of the surah: a menu that still says the text is
    // moving, over text that has nowhere left to go, is a control that has stopped
    // telling the truth.
    var scrollPaused by remember(surahNumber) { mutableStateOf(false) }

    LaunchedEffect(speed, listState, all) {
        if (speed == QuranScrollSpeed.OFF || all == null) {
            scrollPaused = false
            return@LaunchedEffect
        }
        val pxPerSecond = with(density) { speed.dpPerSecond.dp.toPx() }

        var previous = withFrameNanos { it }
        var carry = 0f
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - previous) / 1_000_000_000f
            previous = now

            if (!listState.canScrollForward) {
                onSpeedChange(QuranScrollSpeed.OFF)
                break
            }

            // A finger on the page pauses the drift without cancelling it: the
            // reader is holding a place, not stopping for good. Frame time is
            // still consumed above so releasing does not bank the whole hold
            // into one jump.
            if (scrollPaused) {
                carry = 0f
                continue
            }

            carry += pxPerSecond * seconds
            val whole = carry.toInt()
            if (whole > 0) {
                carry -= whole
                listState.scrollBy(whole.toFloat())
            }
        }
    }

    // Jump back to the opening of the surah when the top bar asks.
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) listState.animateScrollToItem(0)
    }

    // ── Resume where it was left (§2) ──
    //
    // Before anything is written back: the jump is to the saved ayah's ROW, and
    // the row index is the verse's place in the surah plus the basmala above it.
    LaunchedEffect(all, positionLoaded) {
        val list = all ?: return@LaunchedEffect
        if (!positionLoaded || restored) return@LaunchedEffect
        readingPositionIndex(list, savedAyah)?.let { listState.scrollToItem(it) }
        restored = true
    }

    // ── Remember where it was left (§2) ──
    //
    // Keyed to the top VISIBLE row rather than to the bottom, because the top of
    // the page is what a reader recognises as "where I was". Written only after
    // the list sits still, so a fling through forty verses is one position and one
    // prefs commit rather than forty — and written one last time on the way out,
    // because closing the reader mid-fling is exactly when the position matters.
    LaunchedEffect(all, restored) {
        val list = all ?: return@LaunchedEffect
        if (!restored) return@LaunchedEffect
        try {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collectLatest { index ->
                    delay(POSITION_SETTLE_MS)
                    readingPositionAyah(list, index)
                        ?.let { state.saveReadingPosition(surahNumber, it) }
                }
        } finally {
            // Deliberately not a suspend write, and the store's is not one: a
            // coroutine that has been cancelled cannot be suspended again, so a
            // suspend save here would be the one write that never happens.
            readingPositionAyah(list, listState.firstVisibleItemIndex)
                ?.let { state.saveReadingPosition(surahNumber, it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── What is open (§2) ──
        //
        // Pinned above the text rather than part of it, which is exactly why it
        // costs its height on every screenful of a read that may be a hundred of
        // them — and why the fold icon in the top bar exists.
        if (headerOpen) {
            SurahBanner(
                surahNumber = surahNumber,
                verseCount = all?.size ?: 0,
                mode = mode,
                onModeChange = { mode = it }
            )
        }

        // ── What was found in the surah, and the way to it (§10) ──
        //
        // A count and a Next rather than a scroll that fires while the reader is
        // still typing: the text jumps under the thumb once per keystroke
        // otherwise, and the reader cannot tell which match they were taken to.
        if (q.isNotEmpty() && all != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.quran_surah_matches_count,
                        matches.size,
                        matches.size,
                        all.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (matches.isNotEmpty()) {
                    TextButton(onClick = {
                        val index = matches[matchCursor.coerceIn(0, matches.size - 1)]
                        matchCursor = (matchCursor + 1) % matches.size
                        scope.launch { listState.animateScrollToItem(index) }
                    }) {
                        Text(stringResource(R.string.quran_reader_matches_next))
                    }
                }
            }
        }

        when {
            all == null -> Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.quran_bookmarks_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            all.isEmpty() -> Text(
                text = stringResource(R.string.quran_verse_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    // Holding the page pauses auto-scroll; releasing it resumes.
                    // A tap no longer stops the pace for good, which matters when
                    // the tap was only opening a verse's actions.
                    //
                    // Observed on the INITIAL pass, and never consumed: this is a
                    // witness, not a gesture. Consuming here would swallow the tap
                    // on every verse in the surah.
                    .pointerInput(speed) {
                        if (speed == QuranScrollSpeed.OFF) return@pointerInput
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.any { it.pressed }
                                if (scrollPaused != pressed) scrollPaused = pressed
                            }
                        }
                    }
                    // §1: the read position, as a hairline down the right edge.
                    //
                    // Drawn rather than composed: this is the one thing on screen
                    // that changes every frame while the text moves, and a composed
                    // bar would recompose a screenful of scripture sixty times a
                    // second to move three pixels.
                    //
                    // Draws after the text, because it is on the list's own node —
                    // and sits inside the list's 22dp content padding, so no word
                    // ever passes under it.
                    .drawWithContent {
                        drawContent()
                        // Hidden when there is nowhere to scroll: a track with no
                        // fill beside a surah that already fits reads as something
                        // broken, not as progress.
                        if (listState.canScrollForward || listState.canScrollBackward) {
                            drawScrollRail(
                                fraction = progress.value,
                                accent = railAccent,
                                track = railTrack
                            )
                        }
                    },
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                // Wider than the gaps between the cards it replaced: with no
                // borders, SPACE is what separates one ayah from the next (§7).
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                item(key = "surah-head") {
                    SurahHead(surahNumber = surahNumber, mode = mode, basmala = basmala)
                }

                itemsIndexed(
                    all,
                    key = { _, verse -> "${verse.surahNumber}:${verse.ayahNumber}" }
                ) { index, verse ->
                    val key = "${verse.surahNumber}:${verse.ayahNumber}"
                    val bookmarked = key in state.bookmarkKeys
                    val matching = index in matches

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Tap or hold reveals this verse's actions (§8). Not a
                            // tap target that CHANGES the reading position: a tap
                            // in a list of 286 rows is a way to lose your place.
                            .combinedClickable(
                                onClick = { actionsFor = if (actionsFor == key) null else key },
                                onLongClick = { actionsFor = key }
                            )
                    ) {
                        if (mode == QuranReadMode.ARABIC) {
                            ArabicVerse(
                                verse = verse,
                                query = q,
                                bookmarked = bookmarked,
                                matching = matching
                            )
                        } else {
                            EnglishVerse(
                                verse = verse,
                                query = q,
                                matching = matching
                            )
                        }

                        if (actionsFor == key) {
                            val listening = audioVerseKey == key
                            Spacer(Modifier.height(10.dp))
                            // One action per line: on a narrow screen, sharing a
                            // row pushed Bookmark off the edge and made the reader
                            // think it did not exist. Stacked, all three are
                            // always visible and easy to hit.
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuranSegment(
                                    selected = false,
                                    label = stringResource(R.string.quran_verse_copy),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        copyVerseToClipboard(context, verse)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.quran_verse_copied),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        actionsFor = null
                                    }
                                )
                                QuranSegment(
                                    selected = bookmarked,
                                    label = stringResource(
                                        if (bookmarked) R.string.quran_bookmark_remove
                                        else R.string.quran_bookmark
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = if (bookmarked) Icons.Filled.Bookmark
                                    else Icons.Filled.BookmarkBorder,
                                    onClick = {
                                        state.toggleBookmarkAt(
                                            verse.surahNumber,
                                            verse.ayahNumber
                                        )
                                        // Stay open: the reader must SEE the icon
                                        // fill (and the label become Remove)
                                        // before the row disappears.
                                    }
                                )
                            }

                            // Listen sits on its OWN line under the copy /
                            // bookmark pair: the three labels together were
                            // wider than the row, and a third segment squeezed
                            // into it would have shrunk all three to ellipses.
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuranSegment(
                                    modifier = Modifier.weight(1f),
                                    selected = listening &&
                                        verseAudio.status == VerseAudioStatus.PLAYING,
                                    label = stringResource(
                                        when {
                                            listening && verseAudio.status == VerseAudioStatus.PLAYING ->
                                                R.string.quran_verse_listen_pause
                                            listening && verseAudio.status == VerseAudioStatus.PAUSED ->
                                                R.string.quran_verse_listen_resume
                                            listening && verseAudio.status == VerseAudioStatus.ENDED ->
                                                R.string.quran_verse_listen_replay
                                            else -> R.string.quran_verse_listen
                                        }
                                    ),
                                    onClick = {
                                        when {
                                            listening && verseAudio.status == VerseAudioStatus.PLAYING ->
                                                verseAudio.pause()
                                            listening && verseAudio.status == VerseAudioStatus.PAUSED ->
                                                verseAudio.resume()
                                            listening && verseAudio.status == VerseAudioStatus.ENDED ->
                                                verseAudio.restart()
                                            // In flight: the tap is inert rather than
                                            // restarting a request already running.
                                            listening && verseAudio.status == VerseAudioStatus.LOADING -> Unit
                                            else -> {
                                                // A different verse (or the same one
                                                // after a failure): become the
                                                // active verse and start it.
                                                audioVerseKey = key
                                                verseAudio.start(
                                                    verseAudioUrl(
                                                        verse.surahNumber,
                                                        verse.ayahNumber
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                // Restart appears only while there is something
                                // loaded to restart.
                                if (listening && (
                                        verseAudio.status == VerseAudioStatus.PLAYING ||
                                            verseAudio.status == VerseAudioStatus.PAUSED ||
                                            verseAudio.status == VerseAudioStatus.ENDED
                                        )
                                ) {
                                    IconButton(onClick = { verseAudio.restart() }) {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = stringResource(
                                                R.string.quran_verse_listen_restart
                                            ),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            // Seek bar under the Listen controls: the reader can
                            // see how far into the ayah they are and drag to the
                            // part they want. Draws itself only once the file is
                            // prepared (see VerseAudioProgressBar).
                            if (listening) {
                                Spacer(Modifier.height(6.dp))
                                VerseAudioProgressBar(
                                    player = verseAudio,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The read position (§1): a hairline down the right edge, filled in proportion.
 *
 * [fraction] is measured in pixels against the list's viewport rather than as a
 * count of verses, because a surah mixes three-word ayahs with thirty-word ones
 * — a fraction of the ITEM COUNT stalls the bar across a long passage and jumps
 * it across a short one. A lazy list has never measured a row it has not come
 * near, so the visible rows' average height stands in for the ones it has not;
 * beside a mushaf page of near-even rows that is very close, and the only way it
 * can be wrong is by how much text is still to come, which is not something a
 * bar can know either.
 *
 * A [DrawScope] extension rather than a composable because it is called from the
 * list's own draw pass — see the caller for why.
 */
private fun DrawScope.drawScrollRail(fraction: Float, accent: Color, track: Color) {
    val width = 3.dp.toPx()
    val top = 16.dp.toPx()
    val bottom = size.height - 16.dp.toPx()
    if (bottom <= top) return

    val left = size.width - width - 6.dp.toPx()
    val radius = CornerRadius(width / 2f)
    val span = bottom - top

    drawRoundRect(
        color = track,
        topLeft = Offset(left, top),
        size = Size(width, span),
        cornerRadius = radius
    )

    val filled = span * fraction
    if (filled > 0f) {
        drawRoundRect(
            color = accent,
            topLeft = Offset(left, top),
            size = Size(width, filled.coerceIn(0f, span)),
            cornerRadius = radius
        )
    }
}

/**
 * How far the reader has scrolled through the open surah, 0..1.
 *
 * The state read here is [LazyListState.layoutInfo], which is why the caller
 * wraps this in `derivedStateOf` and reads it from a draw pass: unfiltered, a
 * snapshot read of the layout is a subscription to every scroll frame there is.
 */
private fun LazyListState.readFraction(): Float {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (info.totalItemsCount <= 0 || visible.isEmpty()) return 0f

    val average = visible.sumOf { it.size }.toFloat() / visible.size
    if (average <= 0f) return 0f

    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val content = average * info.totalItemsCount
    // Everything already fits: there is no position to be part-way through.
    if (content <= viewport) return 0f

    val scrolled = firstVisibleItemIndex * average + firstVisibleItemScrollOffset
    return (scrolled / (content - viewport)).coerceIn(0f, 1f)
}

/**
 * The surah's identity and the way it is being read (§2, §3): the name, its
 * translation, the surah's own numbers, and the Arabic|English switch.
 *
 * Centred and plain, in the same visual language as the Quran dashboard (§9),
 * and not in a card — a card here would be the very thing this reader is
 * removing. It is a composable of its own because it is the one thing on the
 * screen that can be folded away while the text cannot, and a condition in the
 * middle of the reader would have made that a diff to read rather than a branch.
 */
@Composable
private fun SurahBanner(
    surahNumber: Int,
    verseCount: Int,
    mode: QuranReadMode,
    onModeChange: (QuranReadMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = QuranJsonParser.surahName(surahNumber),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        val meaning = QuranJsonParser.surahTranslation(surahNumber)
        if (meaning.isNotBlank()) {
            Text(
                text = meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.quran_reader_meta, surahNumber, verseCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    // ── Arabic | English, one at a time (§3) ──
    //
    // Never side by side: two columns of two scripts on a phone is a comparison
    // table, and what a reader wants is one text they can read.
    //
    // Inside the fold, which makes the switch something you reach for and then
    // put away. That is the right cost: a reader switches mode a handful of times
    // in a surah and reads for the other hundred screenfuls.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuranSegment(
            selected = mode == QuranReadMode.ARABIC,
            label = stringResource(R.string.quran_reader_arabic),
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(QuranReadMode.ARABIC) }
        )
        QuranSegment(
            selected = mode == QuranReadMode.ENGLISH,
            label = stringResource(R.string.quran_reader_english),
            modifier = Modifier.weight(1f),
            onClick = { onModeChange(QuranReadMode.ENGLISH) }
        )
    }
}

/**
 * The surah's own opening (§2): the basmala, in Arabic mode only.
 *
 * A liturgical line that the corpus carries as ayah 1 of Al-Faatiha and nowhere
 * else, passed in from there rather than written here (see the reader). It opens
 * every surah except At-Tawba, which is the one surah that does not carry it. It
 * is NOT shown in English mode: it is the Arabic that is being opened, and a
 * translated basmala above a translation of the surah is a line of the reader's
 * own commentary.
 */
@Composable
private fun SurahHead(surahNumber: Int, mode: QuranReadMode, basmala: String) {
    if (mode != QuranReadMode.ARABIC) return
    if (surahNumber == 9) return
    if (basmala.isBlank()) return

    Text(
        text = basmala,
        fontSize = quranFontSize(24f),
        lineHeight = quranLineHeight(24f),
        // Arabic scripture (opens every surah but At-Tawba): the Quran face, so
        // the basmala's harakat render exactly as the verses' do. No synthetic
        // bold — it merges dense marks into the letters.
        fontFamily = QuranFontFamily,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * One ayah, as scripture (§4).
 *
 * Large Arabic type, the app's own serif face, centred, with a line height that
 * leaves the harakat room to breathe — the same treatment the Quran dashboard
 * gives a verse, minus the card. The ayah number rides INSIDE the running text as
 * the end-of-ayah ornament `۝` followed by Arabic-Indic digits, which is what a
 * printed mushaf does and what "verse numbers integrated naturally" means: no
 * badge, no separate counter, no second row.
 */
@Composable
private fun ArabicVerse(
    verse: QuranVerse,
    query: String,
    bookmarked: Boolean,
    matching: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    val body = MaterialTheme.colorScheme.onSurface
    val ornament = if (bookmarked) accent else body.copy(alpha = 0.55f)

    val text = remember(verse, query, bookmarked, matching, ornament, accent) {
        buildAnnotatedString {
            if (verse.arabicText.isNotBlank()) {
                append(
                    highlightMatches(
                        verse.arabicText,
                        query.ifBlank { null },
                        accent,
                        bold = false
                    )
                )
                append("  ")
            }
            // Colour only, NO synthetic bold. The Quran face ships Regular
            // only, so SemiBold here is a FAKE weight: it thickens the ornament
            // and its Arabic-Indic digits past the verse's own strokes and smears
            // their curves — the "text looks too bold" report. Colour plus alpha
            // already set the number apart from the verse, so nothing is lost.
            withStyle(SpanStyle(color = ornament)) {
                append("۝" + arabicDigits(verse.ayahNumber))
            }
        }
    }

    Text(
        text = text,
        fontSize = quranFontSize(30f),
        lineHeight = quranLineHeight(30f),
        // Quran face so every harakat in the authoritative text renders (§1).
        // No synthetic bold: it merges dense marks into the letters.
        fontFamily = QuranFontFamily,
        textAlign = TextAlign.Center,
        // RTL is the script's own direction — the paragraph direction is taken
        // from the first strong character, so the Arabic lays itself out from the
        // right without being told.
        modifier = Modifier
            .fillMaxWidth()
            .then(if (matching) Modifier.background(accent.copy(alpha = 0.06f)) else Modifier)
    )
}

/**
 * One ayah, as the translation (§5).
 *
 * A paragraph, not a card and not a blockquote: the reference rides at the end of
 * the sentence it belongs to — `[N]`, the way the app's other translation surfaces
 * write it — and the type is the app's serif, set at reading size with room
 * between the lines. Left-aligned: a page of centred prose is harder to read, and
 * centring is what marks the Arabic as a different thing from the words around it.
 */
@Composable
private fun EnglishVerse(verse: QuranVerse, query: String, matching: Boolean) {
    val accent = MaterialTheme.colorScheme.primary

    val text = remember(verse, query, matching, accent) {
        buildAnnotatedString {
            append(highlightMatches(verse.text, query.ifBlank { null }, accent))
            append(" ")
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                append("[" + verse.ayahNumber + "]")
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Serif,
        lineHeight = 32.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (matching) Modifier.background(accent.copy(alpha = 0.06f)) else Modifier)
    )
}

/**
 * The indices of the verses that match [query], in reading order (§10).
 *
 * The same three-way match the tabbed search uses, and literally the same
 * reference regex — the translation, a `2:255`-style reference, or a bare ayah
 * number — because "why does it not find 2:255" is a question nobody should have
 * to ask twice. Matched against the TRANSLATION rather than the Arabic: a reader
 * on a phone keyboard is typing Latin letters or a reference, and
 * substring-matching Arabic from a Latin keyboard is not a feature.
 *
 * Scoped to the surah it is given, which changes two of the three branches:
 * a plain number means an AYAH here (the surah is already chosen, so "16" inside
 * An-Nahl is its sixteenth ayah, not every verse of it), and a reference to
 * another surah matches nothing rather than reaching outside the text on screen.
 * Without that, typing `1` in An-Nahl matched all 128 verses, because every
 * reference in the surah starts with the digits "16".
 */
internal fun matchingVerseIndices(verses: List<QuranVerse>, query: String): List<Int> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()

    val lower = q.lowercase()
    val ref = verseReference(q)
    val refSurah = ref?.first
    val refAyah = ref?.second
    val plainNumber = q.toIntOrNull()

    return verses.indices.filter { index ->
        val verse = verses[index]
        when {
            refSurah != null && refAyah != null ->
                verse.surahNumber == refSurah && verse.ayahNumber == refAyah
            plainNumber != null -> verse.ayahNumber == plainNumber
            else ->
                verse.text.lowercase().contains(lower) ||
                    verse.surahName.lowercase().contains(lower) ||
                    "${verse.surahNumber}:${verse.ayahNumber}".contains(lower)
        }
    }
}

/**
 * [value] in Arabic-Indic digits, which is what an ayah number wears in the text.
 *
 * Unicode puts the digits INSIDE the `۝` ornament by convention, and the ornament
 * is a mark, not a font — so the digits have to be the ones a mushaf uses rather
 * than Latin ones sitting next to an Arabic symbol.
 */
internal fun arabicDigits(value: Int): String =
    value.toString().map { character ->
        if (character in '0'..'9') ('\u0660' + (character - '0')) else character
    }.joinToString("")

/** The two kinds of thing a reader can save: a passage, or a whole surah. */
private enum class QuranBookmarkTab { SURAH, VERSE }

/**
 * The Bookmarks tab (§3, §10): everything saved, in the two kinds it can be.
 *
 * ## Why the two are split rather than one list with two row shapes
 *
 * A saved verse and a starred surah are not two flavours of the same row: one is
 * a line of text to read again, the other is a place to go back to. Interleaved,
 * the shorter list steals the top of the screen from the longer one and the count
 * stops meaning anything — "6 bookmarks" that are five verses and a surah answers
 * neither question. Split, each count is a count of the thing on the tab.
 *
 * Both tabs filter on the screen's one field ([query]) — a reader who types "cow"
 * and taps Bookmarks is looking for what they saved about it, whichever kind that
 * is — and both remove through the screen's own confirmation: this is the one list
 * the reader built by hand, so nothing should leave it on a single tap.
 *
 * The verses arrive already loaded (the screen owns that, so switching tabs does
 * not re-read the store). The surahs need no loading at all: they are numbers in
 * prefs, and their names come from the bundled parser.
 */
@Composable
private fun BookmarksTab(
    state: QuranHubState,
    query: String,
    bookmarks: List<QuranVerse>?,
    onOpenVerse: (QuranVerse) -> Unit,
    onRemove: (QuranVerse) -> Unit,
    onRemoveSurah: (Int) -> Unit
) {
    // Surahs first, and that is the order the reader reads in: a saved surah is a
    // place to return to and belongs at the top of the reader's own shelf, with
    // the saved verses under it.
    var tab by remember { mutableStateOf(QuranBookmarkTab.SURAH) }
    val starred = remember(state.surahBookmarkKeys) { state.starredSurahs }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuranSegment(
                selected = tab == QuranBookmarkTab.SURAH,
                // The count belongs ON the tab: it is the answer to "did I save
                // anything", and under the segments it took a line of height to
                // say what the label could say for free.
                label = pluralStringResource(
                    R.plurals.quran_surah_bookmarks_count,
                    starred.size,
                    starred.size
                ),
                modifier = Modifier.weight(1f),
                onClick = { tab = QuranBookmarkTab.SURAH }
            )
            QuranSegment(
                selected = tab == QuranBookmarkTab.VERSE,
                label = pluralStringResource(
                    R.plurals.quran_bookmarks_count,
                    bookmarks?.size ?: 0,
                    bookmarks?.size ?: 0
                ),
                modifier = Modifier.weight(1f),
                onClick = { tab = QuranBookmarkTab.VERSE }
            )
        }

        when (tab) {
            QuranBookmarkTab.VERSE -> BookmarkedVerses(
                query = query,
                bookmarks = bookmarks,
                onOpenVerse = onOpenVerse,
                onRemove = onRemove
            )

            QuranBookmarkTab.SURAH -> BookmarkedSurahs(
                query = query,
                starred = starred,
                onOpen = { state.openSurah(it) },
                onRemove = onRemoveSurah
            )
        }
    }
}

/** The saved verses: loading, nothing saved, nothing matching the filter, or the list. */
@Composable
private fun BookmarkedVerses(
    query: String,
    bookmarks: List<QuranVerse>?,
    onOpenVerse: (QuranVerse) -> Unit,
    onRemove: (QuranVerse) -> Unit
) {
    val q = query.trim()
    val filtered = remember(bookmarks, q) {
        val list = bookmarks ?: return@remember emptyList()
        if (q.isEmpty()) list
        else {
            val lower = q.lowercase()
            list.filter {
                it.text.lowercase().contains(lower) ||
                    it.surahName.lowercase().contains(lower) ||
                    "${it.surahNumber}:${it.ayahNumber}".contains(lower)
            }
        }
    }

    when {
        bookmarks == null -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.quran_bookmarks_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        bookmarks.isEmpty() -> Text(
            text = stringResource(R.string.quran_bookmarks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
        )
        filtered.isEmpty() -> Text(
            text = stringResource(R.string.quran_bookmarks_no_match),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { "${it.surahNumber}:${it.ayahNumber}" }) { verse ->
                VerseSearchRow(
                    verse = verse,
                    highlight = query,
                    onClick = { onOpenVerse(verse) },
                    onRemove = { onRemove(verse) }
                )
            }
        }
    }
}

/**
 * The starred surahs, drawn with the same card as the browse list so a surah looks
 * like itself wherever it appears, and filtered by the same rule.
 */
@Composable
private fun BookmarkedSurahs(
    query: String,
    starred: List<Int>,
    onOpen: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    val filtered = remember(starred, query) { filterSurahs(starred, query) }

    when {
        starred.isEmpty() -> Text(
            text = stringResource(R.string.quran_surah_bookmarks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
        )
        filtered.isEmpty() -> Text(
            text = stringResource(R.string.quran_bookmarks_no_match),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(filtered, key = { it }) { number ->
                SurahCard(
                    number = number,
                    starred = true,
                    onOpen = { onOpen(number) },
                    onStar = { onRemove(number) }
                )
            }
        }
    }
}

/**
 * One surah, as a row of a list: number badge, name, translation, and the star.
 *
 * The star is the card's only control, because it is the only thing the card
 * cannot already say by being tapped: the tap opens the surah, so the arrow that
 * used to sit there pointed at what the reader was already doing. It fills in
 * place the moment it is pressed — [starred] comes from the hub's one set of
 * starred surahs, so the card, the bookmarks tab and the next launch agree.
 */
@Composable
private fun SurahCard(
    number: Int,
    starred: Boolean,
    onOpen: () -> Unit,
    onStar: () -> Unit
) {
    val name = QuranJsonParser.surahName(number)
    val translation = QuranJsonParser.surahTranslation(number)

    Card(
        // Opens the whole surah, not its first verse (§2): the reader scrolls
        // from here, and the surah's own text is what it needs — nothing to look
        // up first, so the tap is instant and cannot fail.
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Two-digit number badge.
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = number.toString().padStart(2, '0'),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            // titleMedium / bodyMedium rather than the smaller pair this list
            // used: the surah names are the thing the reader came for, not a
            // dense index they are skimming past.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (translation.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onStar) {
                Icon(
                    imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (starred) R.string.quran_surah_unstar else R.string.quran_surah_star
                    ),
                    tint = if (starred) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * All 114 surahs with number + transliterated name + translation, filtered
 * live by the shared search field ([query]); tapping one opens the continuous
 * reader for it (§2). The names come from [QuranJsonParser] — pure, no network —
 * so the list renders and filters immediately.
 */
@Composable
private fun SurahBrowseList(
    state: QuranHubState,
    query: String
) {
    // Filter the 114 surahs by the shared search field ([filterSurahs]): a blank
    // query shows all of them, a pure number is the surah number exactly, and
    // anything else is a substring of the name or the translation. All from
    // [QuranJsonParser] — bundled data, so filtering is instant with no I/O.
    val surahs = remember(query) { filterSurahs((1..114).toList(), query) }

    if (surahs.isEmpty()) {
        Text(
            text = stringResource(R.string.quran_surah_no_matches),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        items(surahs, key = { it }) { number ->
            SurahCard(
                number = number,
                starred = state.isSurahStarred(number),
                onOpen = { state.openSurah(number) },
                onStar = { state.toggleSurahBookmarkAt(number) }
            )
        }
    }
}

/**
 * [available] surahs narrowed by the sheet's shared field (§2): a blank term is
 * all of them, a pure number is the surah number exactly ("2" → Surah 2, not
 * every surah containing a 2), and anything else is a case-insensitive substring
 * of the transliterated name or its English translation ("cow", "baqara", "ya").
 *
 * All of it from [QuranJsonParser] — bundled data, no I/O — so the browse list
 * and the bookmarks screen's Surah tab filter the same way and neither waits for
 * anything. Shared rather than written twice because the two lists must agree:
 * a term that finds a surah in one and not the other reads as a lost bookmark.
 */
internal fun filterSurahs(available: List<Int>, query: String): List<Int> {
    val q = query.trim()
    if (q.isEmpty()) return available

    val lower = q.lowercase()
    val numeric = q.all { it.isDigit() }
    return available.filter { number ->
        (numeric && number.toString() == q) ||
            QuranJsonParser.surahName(number).lowercase().contains(lower) ||
            QuranJsonParser.surahTranslation(number).lowercase().contains(lower)
    }
}

/**
 * A single verse card (search result or bookmark): surah and reference on one
 * quiet line, the Arabic, then the English. The whole card opens the verse
 * detail, and Back from there returns to this list. Pass [onRemove] for the
 * bookmarks manager; nested clicks are consumed so remove never opens the verse.
 */
@Composable
private fun VerseSearchRow(
    verse: QuranVerse,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    highlight: String? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 13.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = verse.surahName,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${verse.surahNumber}:${verse.ayahNumber}",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.quran_bookmarks_remove),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp).padding(start = 2.dp)
                    )
                }
            }
            if (verse.arabicText.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(
                    text = verse.arabicText,
                    fontSize = quranFontSize(19f),
                    lineHeight = quranLineHeight(19f),
                    fontFamily = QuranFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = highlightMatches(
                    verse.text,
                    highlight,
                    MaterialTheme.colorScheme.primary
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Serif,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The verse a result or bookmark card opened: the whole scripture, not a peek.
 * It is an overlay on the search screen so closing it returns to the list the
 * reader was on, with their query and place still there.
 */
@Composable
private fun QuranVerseDetailOverlay(
    verse: QuranVerse,
    state: QuranHubState,
    onBack: () -> Unit,
    onSetCurrent: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val bookmarked = state.isVerseBookmarked(verse.surahNumber, verse.ayahNumber)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.quran_search_back)
                    )
                }
                Text(
                    text = stringResource(R.string.quran_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    state.toggleBookmarkAt(verse.surahNumber, verse.ayahNumber)
                }) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Bookmark
                        else Icons.Filled.BookmarkBorder,
                        contentDescription = stringResource(R.string.quran_bookmark),
                        tint = if (bookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = verse.surahName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
                if (verse.surahTranslation.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = verse.surahTranslation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (verse.totalAyahs > 0) {
                        stringResource(
                            R.string.quran_ayah_reference_of,
                            verse.surahNumber,
                            verse.ayahNumber,
                            verse.totalAyahs
                        )
                    } else {
                        stringResource(
                            R.string.quran_ayah_reference,
                            verse.surahNumber,
                            verse.ayahNumber
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(26.dp))
                Text(
                    text = verse.arabicText,
                    fontSize = quranFontSize(30f),
                    lineHeight = quranLineHeight(30f),
                    fontFamily = QuranFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(26.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "“${verse.text}”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 27.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PillButton(
                        label = stringResource(R.string.quran_verse_copy),
                        icon = Icons.Filled.ContentCopy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            copyVerseToClipboard(context, verse)
                            Toast.makeText(
                                context,
                                context.getString(R.string.quran_verse_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    PillButton(
                        label = stringResource(
                            if (bookmarked) R.string.quran_detail_saved
                            else R.string.quran_bookmark
                        ),
                        icon = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            state.toggleBookmarkAt(verse.surahNumber, verse.ayahNumber)
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                FilledPill(
                    label = stringResource(R.string.quran_detail_set_current),
                    icon = Icons.Filled.Bookmark,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSetCurrent
                )
            }
        }
    }
}

/**
 * Returns [text] with every case-insensitive occurrence of [query] styled with
 * [color] + bold (for search/bookmark result highlighting). Plain when [query]
 * is blank or purely numeric/reference-shaped ("2:255" / "255" lookups match
 * verse numbers, so highlighting text would be noise).
 */
private fun highlightMatches(
    text: String,
    query: String?,
    color: Color,
    /**
     * Whether matched spans are drawn bold. FALSE for Arabic (§1): the Quran
     * face ships Regular only, so a Bold span is a SYNTHETIC weight that
     * re-shapes the run and can sit the harakat differently from the surrounding
     * text — which is exactly the "highlighted words look different" complaint.
     * Arabic marks the match with colour alone.
     */
    bold: Boolean = true
): AnnotatedString {
    val q = query?.trim().orEmpty()
    if (q.isEmpty() || q.all { it.isDigit() || it == ':' || it == '.' || it.isWhitespace() }) {
        return AnnotatedString(text)
    }
    val lowerText = text.lowercase()
    val lowerQuery = q.lowercase()
    // A case fold that changes LENGTH would misalign every index (the Turkish
    // dotted I is the usual culprit). Rather than highlight the wrong span, pass
    // the text through untouched.
    if (lowerText.length != text.length || lowerQuery.length != q.length) {
        return AnnotatedString(text)
    }
    val boundaries = graphemeBoundaries(text)
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val match = lowerText.indexOf(lowerQuery, index)
            if (match < 0) {
                append(text, index, text.length)
                break
            }
            // Never split a grapheme cluster: an Arabic base letter and its
            // harakat are ONE unit, and cutting between them either drops the
            // mark or draws it on the wrong letter. Snapping both edges to
            // cluster boundaries keeps the highlighted slice — and the plain
            // slice before it — shaped exactly as the undivided text would be.
            val start = snapToCluster(boundaries, match, forward = false)
            val end = snapToCluster(boundaries, match + q.length, forward = true)
            if (start > index) append(text, index, start)
            if (end > start) {
                val style = if (bold) {
                    SpanStyle(color = color, fontWeight = FontWeight.Bold)
                } else {
                    SpanStyle(color = color)
                }
                withStyle(style) {
                    append(text, start, end)
                }
            }
            index = maxOf(end, start + 1)
        }
    }
}

/**
 * The set of grapheme-cluster boundaries in [text] (character instance), used
 * to keep highlighting from cutting through a combining sequence.
 */
private fun graphemeBoundaries(text: String): List<Int> {
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val boundaries = ArrayList<Int>()
    var at = iterator.first()
    while (at != java.text.BreakIterator.DONE) {
        boundaries.add(at)
        at = iterator.next()
    }
    return boundaries
}

/** The nearest cluster boundary to [offset] in the given [direction]. */
private fun snapToCluster(boundaries: List<Int>, offset: Int, forward: Boolean): Int {
    if (boundaries.isEmpty()) return offset
    var low = 0
    var high = boundaries.size - 1
    // The last boundary <= offset (the cluster start at or before it).
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (boundaries[mid] <= offset) low = mid else high = mid - 1
    }
    val atOrBefore = boundaries[low]
    if (atOrBefore == offset) return offset
    return if (forward) {
        boundaries.getOrElse(low + 1) { atOrBefore }
    } else {
        atOrBefore
    }
}

/**
 * Quran settings (the gear in the expanded card): how often the verse refreshes,
 * the Arabic text size, and whether a new verse is announced.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuranSettingsSheet(state: QuranHubState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val presets = VERSE_INTERVAL_OPTIONS
    val interval = state.refreshIntervalHours
    val deniedMessage = stringResource(R.string.media_notification_permission_denied)

    var pendingPermissionApply by remember { mutableStateOf<(Boolean) -> Unit>({}) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingPermissionApply(granted)
        if (!granted) {
            Toast.makeText(context, deniedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.quran_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.quran_verse_refresh_frequency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { hours ->
                    FilterChip(
                        selected = hours == interval,
                        onClick = {
                            if (hours != interval) state.changeInterval(context, hours)
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        label = {
                            Text(
                                text = stringResource(R.string.quran_verse_hour_short, hours),
                                fontWeight = if (hours == interval) FontWeight.Bold
                                else FontWeight.Medium
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.quran_verse_refresh_note_hours,
                    interval,
                    interval
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            ArabicTextControls(state = state)

            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.quran_settings_warmth),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(
                                R.string.quran_text_size_percent,
                                (state.themeWarmth * 100).roundToInt()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    SleekSlider(
                        value = state.themeWarmth,
                        valueRange = 0f..1f,
                        step = 0.05f,
                        onValueChange = { state.changeThemeWarmth(it) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Permission first: the setting defaults to on, so on a
                        // fresh Android 13+ install the first tap must ask for
                        // notifications rather than silently disable them.
                        if (needsNotificationPermission(context)) {
                            pendingPermissionApply = { granted ->
                                state.setQuranNotifications(granted)
                            }
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            state.setQuranNotifications(!state.quranNotificationsEnabled)
                        }
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.quran_notifications),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.quran_notifications_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.quranNotificationsEnabled,
                    onCheckedChange = null
                )
            }
        }
    }
}

/** Whether the app still needs the runtime notification permission (Android 13+). */
private fun needsNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return false
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
}

/** The ±1 day Umm al-Qura adjustment, opened by the pencil beside the date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicDateAdjustmentSheet(
    adjustment: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.islamic_date_adjustment_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-1, 0, 1).forEach { days ->
                    FilterChip(
                        selected = adjustment == days,
                        onClick = { onSelect(days) },
                        modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        label = {
                            Text(
                                stringResource(
                                    when (days) {
                                        -1 -> R.string.islamic_date_adjustment_minus
                                        1 -> R.string.islamic_date_adjustment_plus
                                        else -> R.string.islamic_date_adjustment_default
                                    }
                                )
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.islamic_date_current,
                    stringResource(
                        when (adjustment) {
                            -1 -> R.string.islamic_date_adjustment_minus
                            1 -> R.string.islamic_date_adjustment_plus
                            else -> R.string.islamic_date_zero_days
                        }
                    )
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.islamic_date_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
