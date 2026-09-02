package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CSV export of the fast history (SPEC 3.5).
 *
 * Timestamps are written both as ISO-8601 in the fast's own zone and as raw
 * epoch millis, so the file is readable by a person and lossless for a machine.
 */
object HistoryCsv {

    private val HEADER = listOf(
        "id",
        "planId",
        "targetMinutes",
        "eatingWindowMinutes",
        "start",
        "end",
        "startMillis",
        "endMillis",
        "timeZoneId",
        "durationMinutes",
        "note",
    )

    fun export(fasts: List<Fast>): String = buildString {
        appendLine(HEADER.joinToString(","))
        fasts.sortedBy { it.startMillis }.forEach { fast ->
            appendLine(row(fast).joinToString(",") { escape(it) })
        }
    }

    private fun row(fast: Fast): List<String> {
        val zone = fast.zone
        return listOf(
            fast.id.toString(),
            fast.planId,
            fast.targetMinutes.toString(),
            fast.eatingWindowMinutes?.toString().orEmpty(),
            iso(fast.startMillis, zone),
            fast.endMillis?.let { iso(it, zone) }.orEmpty(),
            fast.startMillis.toString(),
            fast.endMillis?.toString().orEmpty(),
            fast.timeZoneId,
            fast.actualDuration?.let { (it.toMillis() / 60_000L).toString() }.orEmpty(),
            fast.note.orEmpty(),
        )
    }

    private fun iso(millis: Long, zone: ZoneId): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(millis).atZone(zone))

    /** Quotes a field only when it would otherwise break the row. */
    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
