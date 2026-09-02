package com.oleksandr.fastflow.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One fasting session.
 *
 * [timeZoneId] is the zone the fast was started in and is what the calendar
 * uses, so moving the device to another zone does not repaint history
 * (SPEC 7, case 10).
 */
data class Fast(
    val id: Long = 0L,
    val planId: String,
    /** Goal frozen at start, so later plan edits never rewrite history. */
    val targetMinutes: Int,
    /** Planned eating window; `null` for extended plans. */
    val eatingWindowMinutes: Int?,
    val startMillis: Long,
    /** `null` while the fast is running. */
    val endMillis: Long? = null,
    val timeZoneId: String,
    val note: String? = null,
    val createdAt: Long = startMillis,
    val updatedAt: Long = startMillis,
) {
    val isActive: Boolean get() = endMillis == null

    val zone: ZoneId
        get() = runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.systemDefault() }

    val target: Duration get() = Duration.ofMinutes(targetMinutes.toLong())

    val isExtended: Boolean get() = eatingWindowMinutes == null

    /** Real elapsed time — measured in millis, never in wall-clock hours, so a
     *  DST change does not distort it (SPEC 7, case 9). */
    fun elapsed(nowMillis: Long): Duration =
        Duration.ofMillis(((endMillis ?: nowMillis) - startMillis).coerceAtLeast(0L))

    /** Duration of a finished fast; `null` while it is still running. */
    val actualDuration: Duration?
        get() = endMillis?.let { Duration.ofMillis((it - startMillis).coerceAtLeast(0L)) }

    /** Fraction of the goal reached, where 1f means the target was met. */
    fun completionRatio(nowMillis: Long): Float {
        if (targetMinutes <= 0) return 1f
        val elapsedMinutes = elapsed(nowMillis).toMillis() / 60_000.0
        return (elapsedMinutes / targetMinutes).toFloat().coerceAtLeast(0f)
    }

    val startDate: LocalDate get() = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()

    /**
     * Last calendar date the fast touches. The interval is half-open, so a fast
     * ending exactly at midnight does not claim the following day.
     */
    fun lastDate(nowMillis: Long): LocalDate {
        val endExclusive = endMillis ?: nowMillis
        val lastInstantMillis = (endExclusive - 1).coerceAtLeast(startMillis)
        return Instant.ofEpochMilli(lastInstantMillis).atZone(zone).toLocalDate()
    }

    /** Every calendar date this fast overlaps, in the zone it was started in. */
    fun coveredDates(nowMillis: Long): List<LocalDate> {
        val first = startDate
        val last = lastDate(nowMillis)
        val dates = mutableListOf<LocalDate>()
        var cursor = first
        while (!cursor.isAfter(last)) {
            dates += cursor
            cursor = cursor.plusDays(1)
        }
        return dates
    }

    companion object {
        /** A start time may not be moved further back than this (SPEC 3.2). */
        const val MAX_BACKDATE_DAYS = 7L
    }
}
