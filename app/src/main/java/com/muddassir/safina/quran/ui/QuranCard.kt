package com.muddassir.safina.quran.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.muddassir.safina.LocalSafinaPalette
import com.muddassir.safina.R
import com.muddassir.safina.SafinaAccent
import com.muddassir.safina.SafinaField
import com.muddassir.safina.SafinaInk
import com.muddassir.safina.SafinaMuted
import com.muddassir.safina.SafinaOnInk
import com.muddassir.safina.SafinaSurface
import com.muddassir.safina.quran.data.IslamicDateFormatter
import com.muddassir.safina.quran.model.QuranVerse
import com.muddassir.safina.quran.util.copyVerseToClipboard
import java.time.LocalDate
import kotlinx.coroutines.delay

// The Quran surfaces borrow Safina's own ink/paper palette; only the hairline
// rules and selected fills are derived, so both themes stay quiet and consistent
// with the rest of the app instead of introducing a second mint/white scheme.
private val QuranAccent: Color @Composable get() = SafinaAccent
private val QuranInk: Color @Composable get() = SafinaInk
private val QuranMuted: Color @Composable get() = SafinaMuted
private val QuranTrack: Color @Composable get() = QuranInk.copy(alpha = 0.10f)
private val QuranSelected: Color @Composable get() = QuranAccent.copy(alpha = 0.14f)

/**
 * The Quran's paper, warmed toward Safina's sand tone by the reader's comfort
 * setting. Kept to a small blend so the result is still paper, not a tint.
 */
@Composable
private fun quranPaper(warmth: Float): Color {
    val palette = LocalSafinaPalette.current
    return lerp(palette.surface, palette.warm, warmth.coerceIn(0f, 1f) * 0.12f)
}

/**
 * The Quran card shown at the top of Safina's dhikr list.
 *
 * Collapsed it is the URL Blocker 2 widget: the English verse, and nothing else,
 * in the same footprint as a dhikr card. Tapping it hands control to the expanded
 * card through [onExpand].
 */
@Composable
fun QuranCollapsedCard(
    state: QuranHubState,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verse = state.verse
    Box(
        modifier
            .fillMaxWidth()
            // Exactly the dhikr card's footprint: the Quran sits in the same
            // stack, so it should read as one more card in that stack.
            .height(172.dp)
            .shadow(
                6.dp, RoundedCornerShape(24.dp),
                ambientColor = QuranInk.copy(alpha = 0.18f),
                spotColor = QuranInk.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(quranPaper(state.themeWarmth))
            .border(1.dp, QuranInk.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
            .clickable(onClick = onExpand)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (verse == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = QuranAccent
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (state.verseLoading) "Loading verse…"
                    else stringResource(R.string.quran_verse_unavailable),
                    fontSize = 12.sp,
                    color = QuranMuted
                )
            }
        } else {
            Text(
                text = verse.text,
                fontSize = 16.sp,
                lineHeight = 25.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                color = QuranInk,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The surah name, meaning, reference and thin position bar. */
@Composable
private fun VerseHeading(verse: QuranVerse) {
    Text(
        text = verse.surahName,
        fontSize = 22.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        color = QuranInk,
        textAlign = TextAlign.Center
    )
    if (verse.surahTranslation.isNotBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(
            text = verse.surahTranslation,
            fontSize = 12.sp,
            color = QuranMuted,
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
            stringResource(R.string.quran_ayah_reference, verse.surahNumber, verse.ayahNumber)
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = QuranAccent,
        textAlign = TextAlign.Center
    )
    if (verse.totalAyahs > 0) {
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .width(150.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(QuranTrack)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(
                        (verse.ayahNumber.toFloat() / verse.totalAyahs.toFloat())
                            .coerceIn(0f, 1f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(QuranAccent)
            )
        }
    }
}

/**
 * The expanded Quran card: a pinned header over one smooth scrolling body.
 *
 * The screen behind it is blurred by the dashboard; a soft scrim keeps the paper
 * card distinct on devices that cannot blur.
 */
@Composable
fun QuranExpandedOverlay(
    state: QuranHubState,
    onClose: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showDateSheet by rememberSaveable { mutableStateOf(false) }
    var listenVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val verse = state.verse

    BackHandler(enabled = true, onBack = onClose)

    // A new verse starts at the top, as the original reader did.
    LaunchedEffect(verse?.surahNumber, verse?.ayahNumber) {
        scrollState.scrollTo(0)
    }

    QuranTheme(warmth = state.themeWarmth) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)
                    .statusBarsPadding()
                    .clip(RoundedCornerShape(28.dp))
                    .background(quranPaper(state.themeWarmth))
                    .border(1.dp, QuranInk.copy(alpha = 0.09f), RoundedCornerShape(28.dp))
            ) {
                // ── Pinned header ───────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quran",
                        fontSize = 25.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        color = QuranInk
                    )
                    Spacer(Modifier.weight(1f))
                    HeaderIcon(
                        description = stringResource(R.string.quran_card_listen),
                        active = listenVisible,
                        onClick = { listenVisible = !listenVisible }
                    ) {
                        Icon(
                            Icons.Filled.Headphones,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    HeaderIcon(
                        description = stringResource(R.string.quran_search),
                        onClick = { showSearch = true }
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    HeaderIcon(
                        description = stringResource(R.string.quran_bookmark),
                        onClick = { state.toggleBookmark() }
                    ) {
                        Icon(
                            imageVector = if (state.isBookmarked) Icons.Filled.Bookmark
                            else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    HeaderIcon(
                        description = stringResource(R.string.quran_settings_title),
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SafinaField)
                            .border(1.dp, QuranInk.copy(alpha = 0.16f), CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✕",
                            fontSize = 20.sp,
                            color = QuranInk
                        )
                    }
                }

                // ── Scrolling body ─────────────────────────────────
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 22.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    if (verse == null) {
                        Spacer(Modifier.height(40.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = QuranAccent
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (state.verseLoading) "Loading verse…" else stringResource(R.string.quran_verse_unavailable),
                                fontSize = 13.sp,
                                color = QuranMuted
                            )
                        }
                    } else {
                        VerseBody(
                            verse = verse,
                            state = state,
                            listenVisible = listenVisible,
                            onAdjustDate = { showDateSheet = true }
                        )
                    }
                    Spacer(Modifier.height(36.dp))
                }
            }
        }

        if (showSearch) {
            CompositionLocalProvider(LocalQuranTextScale provides state.arabicTextScale) {
                QuranSearchScreen(state = state, onDismiss = { showSearch = false })
            }
        }
        if (showSettings) {
            QuranSettingsSheet(state = state, onDismiss = { showSettings = false })
        }
        if (showDateSheet) {
            IslamicDateAdjustmentSheet(
                adjustment = state.islamicDateAdjustment,
                onSelect = { state.changeIslamicDateAdjustment(it) },
                onDismiss = { showDateSheet = false }
            )
        }
    }

    // Keep a stale verse from lingering after a long session.
    LaunchedEffect(Unit) { state.reloadCurrentVerse() }
}

@Composable
private fun VerseBody(
    verse: QuranVerse,
    state: QuranHubState,
    listenVisible: Boolean,
    onAdjustDate: () -> Unit
) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        VerseHeading(verse = verse)
        Spacer(Modifier.height(20.dp))

        Text(
            text = verse.arabicText,
            fontSize = quranFontSize(32f),
            lineHeight = quranLineHeight(32f),
            fontFamily = QuranFontFamily,
            color = QuranInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                label = stringResource(R.string.quran_verse_previous),
                enabled = state.canGoPrevious,
                leading = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = { state.goToAdjacentVerse(-1) }
            )
            PillButton(
                label = stringResource(R.string.quran_verse_next),
                enabled = state.canGoNext,
                trailing = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { state.goToAdjacentVerse(+1) }
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "“${verse.text}”",
            fontSize = 15.sp,
            lineHeight = 24.sp,
            fontFamily = FontFamily.Serif,
            color = QuranInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (listenVisible) {
            Spacer(Modifier.height(20.dp))
            ListenControls(verse)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledPill(
                label = stringResource(R.string.quran_verse_new),
                icon = Icons.Filled.Refresh,
                modifier = Modifier.weight(1f),
                onClick = { state.pickNewVerse() }
            )
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
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = QuranTrack)
        Spacer(Modifier.height(14.dp))
        HijriDateRow(adjustment = state.islamicDateAdjustment, onAdjust = onAdjustDate)
    }
}

@Composable
private fun ListenControls(verse: QuranVerse) {
    val audio = rememberVerseAudioPlayer(verse.surahNumber, verse.ayahNumber)
    val context = LocalContext.current
    LaunchedEffect(audio.status) {
        if (audio.status == VerseAudioStatus.FAILED) {
            Toast.makeText(
                context,
                R.string.quran_verse_listen_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val label = when (audio.status) {
        VerseAudioStatus.PLAYING -> R.string.quran_verse_listen_pause
        VerseAudioStatus.PAUSED -> R.string.quran_verse_listen_resume
        VerseAudioStatus.ENDED -> R.string.quran_verse_listen_replay
        else -> R.string.quran_verse_listen
    }
    val icon = when (audio.status) {
        VerseAudioStatus.PLAYING -> Icons.Filled.Pause
        VerseAudioStatus.ENDED -> Icons.Filled.Replay
        else -> Icons.Filled.PlayArrow
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SafinaField)
            .border(1.dp, QuranInk.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                label = stringResource(label),
                icon = icon,
                enabled = audio.status != VerseAudioStatus.LOADING,
                onClick = {
                    when (audio.status) {
                        VerseAudioStatus.PLAYING -> audio.pause()
                        VerseAudioStatus.PAUSED -> audio.resume()
                        VerseAudioStatus.ENDED -> audio.restart()
                        VerseAudioStatus.LOADING -> Unit
                        else -> audio.start(verseAudioUrl(verse.surahNumber, verse.ayahNumber))
                    }
                }
            )
            if (audio.status == VerseAudioStatus.PLAYING ||
                audio.status == VerseAudioStatus.PAUSED ||
                audio.status == VerseAudioStatus.ENDED
            ) {
                IconButton(onClick = { audio.restart() }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.quran_verse_listen_restart),
                        modifier = Modifier.size(20.dp),
                        tint = QuranAccent
                    )
                }
            }
        }
        VerseAudioProgressBar(player = audio, modifier = Modifier.padding(top = 8.dp))
        if (audio.status == VerseAudioStatus.LOADING) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.quran_verse_listen_loading),
                fontSize = 11.sp,
                color = QuranMuted
            )
        }
    }
}

@Composable
private fun HijriDateRow(adjustment: Int, onAdjust: () -> Unit) {
    var dayTick by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            dayTick = LocalDate.now()
        }
    }
    val date = remember(dayTick, adjustment) {
        IslamicDateFormatter.format(dayTick, adjustment)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = date,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = QuranInk
        )
        IconButton(onClick = onAdjust, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.quran_islamic_date_adjust),
                modifier = Modifier.size(15.dp),
                tint = QuranMuted
            )
        }
    }
}

/** A circular icon button in the pinned header, tinted while active. */
@Composable
private fun HeaderIcon(
    description: String,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description }
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (active) QuranSelected else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (active) QuranAccent else QuranInk
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val tint = if (enabled) QuranInk else QuranMuted.copy(alpha = 0.45f)
    Row(
        modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(SafinaField)
            .border(1.dp, QuranInk.copy(alpha = if (enabled) 0.12f else 0.05f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        (leading ?: icon)?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tint)
        trailing?.let {
            Spacer(Modifier.width(8.dp))
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
        }
    }
}

@Composable
internal fun FilledPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(QuranAccent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = SafinaOnInk)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SafinaOnInk)
    }
}

/**
 * Safina's own paper-and-ink palette, exposed to the ported Quran screens as a
 * Material theme. No second accent family: the Quran is part of the app, not a
 * differently-coloured sub-app.
 */
@Composable
private fun QuranTheme(warmth: Float, content: @Composable () -> Unit) {
    val palette = LocalSafinaPalette.current
    val dark = palette.backdropTop.luminance() < 0.35f
    val paper = lerp(palette.surface, palette.warm, warmth.coerceIn(0f, 1f) * 0.12f)
    val surfaceVariant = lerp(paper, palette.ink, if (dark) 0.07f else 0.05f)
    val outline = lerp(paper, palette.ink, if (dark) 0.18f else 0.14f)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onInk,
            primaryContainer = lerp(paper, palette.accent, 0.22f),
            onPrimaryContainer = palette.ink,
            background = paper,
            surface = paper,
            surfaceVariant = surfaceVariant,
            onSurface = palette.ink,
            onSurfaceVariant = palette.muted,
            outline = outline,
            error = Color(0xFFE6A399)
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onInk,
            primaryContainer = lerp(paper, palette.accent, 0.16f),
            onPrimaryContainer = palette.ink,
            background = paper,
            surface = paper,
            surfaceVariant = surfaceVariant,
            onSurface = palette.ink,
            onSurfaceVariant = palette.muted,
            outline = outline,
            error = Color(0xFF9A4A3E)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
