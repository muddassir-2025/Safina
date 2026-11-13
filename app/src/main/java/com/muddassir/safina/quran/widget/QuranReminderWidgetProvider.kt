package com.muddassir.safina.quran.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.muddassir.safina.MainActivity
import com.muddassir.safina.R
import com.muddassir.safina.ZikrStore
import com.muddassir.safina.quran.data.QuranRepository
import com.muddassir.safina.quran.worker.QuranWorkScheduler

/**
 * Home-screen widget showing the current English Quran verse.
 *
 * **The verse is the whole card.** The TextView shrinks to fit and ellipsizes if
 * even the smallest size has no room, so the card is safe to resize. Tapping it
 * opens Safina, where the full verse, Copy and Share live.
 *
 * Rendering is instant and offline: it reads the persisted current verse, with no
 * parsing, no network and no coroutine. The verse itself is refreshed on the
 * user's schedule (default 6 hours) by the worker; this provider only reflects
 * what is stored.
 */
class QuranReminderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        QuranWorkScheduler.ensureScheduled(context)
        appWidgetIds.forEach { widgetId ->
            renderWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        QuranWorkScheduler.ensureScheduled(context)
    }

    companion object {

        /** Re-renders every active widget instance after a verse refresh. */
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, QuranReminderWidgetProvider::class.java)
            )
            ids.forEach { widgetId ->
                renderWidget(context, manager, widgetId)
            }
        }

        private fun renderWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quran_reminder)
            val verse = QuranRepository(context).getCurrentVerse()

            // The app's explicit Dark mode toggle wins; otherwise the widget
            // follows the system (values-night) configuration.
            val systemNight = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val dark = ZikrStore(context).darkModeOverride ?: systemNight
            if (dark) {
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    R.drawable.widget_quran_reminder_bg_dark
                )
                views.setTextColor(
                    R.id.widget_title,
                    ContextCompat.getColor(context, R.color.widget_dim)
                )
                views.setTextColor(
                    R.id.widget_verse_text,
                    ContextCompat.getColor(context, R.color.widget_verse_text)
                )
            } else {
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    R.drawable.widget_quran_reminder_bg
                )
                views.setTextColor(
                    R.id.widget_title,
                    ContextCompat.getColor(context, R.color.widget_dim)
                )
                views.setTextColor(
                    R.id.widget_verse_text,
                    ContextCompat.getColor(context, R.color.widget_verse_text)
                )
            }

            views.setTextViewText(
                R.id.widget_verse_text,
                verse?.text ?: context.getString(R.string.widget_quran_loading)
            )

            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            manager.updateAppWidget(widgetId, views)
        }
    }
}
