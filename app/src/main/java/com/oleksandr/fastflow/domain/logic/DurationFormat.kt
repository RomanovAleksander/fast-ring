package com.oleksandr.fastflow.domain.logic

import java.util.Locale

/**
 * Duration strings shared by the ring, the notification and the widget.
 *
 * Hours are never wrapped into days: a 36-hour fast reads "36:00", which is
 * what the user set out to do.
 */
object DurationFormat {

    /** `HH:MM`, zero-padded, hours unbounded. */
    fun hhmm(millis: Long): String {
        val total = totalSeconds(millis)
        return String.format(Locale.ROOT, "%02d:%02d", total / 3600, total % 3600 / 60)
    }

    /** `HH:MM:SS`, for the timer while seconds are visible. */
    fun hhmmss(millis: Long): String {
        val total = totalSeconds(millis)
        return String.format(
            Locale.ROOT,
            "%02d:%02d:%02d",
            total / 3600,
            total % 3600 / 60,
            total % 60,
        )
    }

    /** `H:MM` without a leading zero — used for the "+1:05" overtime caption. */
    fun compact(millis: Long): String {
        val total = totalSeconds(millis)
        return String.format(Locale.ROOT, "%d:%02d", total / 3600, total % 3600 / 60)
    }

    /** Just the seconds part, for the small digits beside the big timer. */
    fun seconds(millis: Long): String =
        String.format(Locale.ROOT, "%02d", totalSeconds(millis) % 60)

    /** Whole hours as the plan labels write them: "16", "13.5". */
    fun hoursLabel(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) hours.toString() else String.format(Locale.ROOT, "%d.%d", hours, rest * 10 / 60)
    }

    private fun totalSeconds(millis: Long): Long = (millis.coerceAtLeast(0L)) / 1000
}
