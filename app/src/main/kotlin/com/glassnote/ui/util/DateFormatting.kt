package com.glassnote.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.glassnote.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val TIME_PATTERN = "HH:mm"
private const val NOTE_LIST_DATE_PATTERN = "yyyy/MM/dd HH:mm EEEE"
private const val NOTE_DETAIL_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss EEEE"

/**
 * Formats the timestamp shown on a note card.
 *
 * Today and yesterday keep their temporal context. Notes between two and three
 * days old use a relative "N days ago". Anything older switches to a complete
 * date with the localised weekday.
 */
fun formatNoteListTime(
    context: Context,
    timestamp: Long,
    nowMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault(),
): String {
    val zone = ZoneId.systemDefault()
    val effectiveLocale = resolveDateLocale(locale)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()
    val noteTime = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime()
    val daysBetween = ChronoUnit.DAYS.between(noteTime.toLocalDate(), now.toLocalDate())
    val timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN, effectiveLocale)

    return when {
        daysBetween < 0L -> {
            DateTimeFormatter.ofPattern(NOTE_LIST_DATE_PATTERN, effectiveLocale).format(noteTime)
        }

        daysBetween == 0L -> {
            val minutes = ChronoUnit.MINUTES.between(noteTime, now).coerceAtLeast(0L)
            when {
                minutes < 1L -> context.getString(R.string.time_just_now)
                minutes < 60L -> context.resources.getQuantityString(
                    R.plurals.time_minutes_ago,
                    minutes.toInt(),
                    minutes.toInt(),
                )
                else -> {
                    val hours = minutes / 60L
                    context.resources.getQuantityString(
                        R.plurals.time_hours_ago,
                        hours.toInt(),
                        hours.toInt(),
                    )
                }
            }
        }

        daysBetween == 1L -> context.getString(
            R.string.time_yesterday_at,
            noteTime.format(timeFormatter),
        )

        daysBetween <= 3L -> context.resources.getQuantityString(
            R.plurals.time_days_ago,
            daysBetween.toInt(),
            daysBetween.toInt(),
        )

        else -> DateTimeFormatter.ofPattern(NOTE_LIST_DATE_PATTERN, effectiveLocale).format(noteTime)
    }
}

/** Complete timestamp used in the editor details. */
fun formatFullDateTime(
    timestamp: Long,
    locale: Locale = Locale.getDefault(),
): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return DateTimeFormatter.ofPattern(
        NOTE_DETAIL_DATE_PATTERN,
        resolveDateLocale(locale),
    ).format(dateTime)
}

/**
 * The app falls back to the Simplified Chinese resource set for languages other
 * than English, Simplified Chinese and Traditional Chinese. Keep java.time in
 * sync with that fallback so unsupported system locales do not mix languages.
 */
private fun resolveDateLocale(locale: Locale): Locale = when {
    locale.language.equals("zh", ignoreCase = true) &&
        locale.country.equals("TW", ignoreCase = true) -> Locale.TRADITIONAL_CHINESE
    locale.language.equals("zh", ignoreCase = true) -> Locale.SIMPLIFIED_CHINESE
    locale.language.equals("en", ignoreCase = true) -> Locale.ENGLISH
    else -> Locale.SIMPLIFIED_CHINESE
}

@Composable
fun rememberNoteListTime(
    timestamp: Long,
    nowMillis: Long,
): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(timestamp, nowMillis, locale) {
        formatNoteListTime(
            context = context,
            timestamp = timestamp,
            nowMillis = nowMillis,
            locale = locale,
        )
    }
}

@Composable
fun rememberFullDateTime(timestamp: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(timestamp, locale) {
        formatFullDateTime(timestamp = timestamp, locale = locale)
    }
}
