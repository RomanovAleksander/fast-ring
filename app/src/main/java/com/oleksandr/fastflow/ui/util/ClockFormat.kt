package com.oleksandr.fastflow.ui.util

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Wall-clock times, honouring the 12/24-hour setting (SPEC 3.5). */
object ClockFormat {

    fun time(millis: Long, zone: ZoneId, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .format(Instant.ofEpochMilli(millis).atZone(zone))
    }

    fun date(millis: Long, zone: ZoneId, pattern: String = "d MMMM"): String =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .format(Instant.ofEpochMilli(millis).atZone(zone))
}

/**
 * Resolves the clock format: the user's explicit choice, or the system's when
 * they have not made one.
 */
@Composable
fun rememberUse24Hour(preference: Boolean?): Boolean {
    val context: Context = LocalContext.current
    return remember(preference, context) {
        preference ?: DateFormat.is24HourFormat(context)
    }
}
