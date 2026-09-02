package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.HistoryCsv
import com.oleksandr.fastflow.domain.model.Fast
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCsvTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun millis(text: String) =
        LocalDateTime.parse(text).atZone(kyiv).toInstant().toEpochMilli()

    private fun fast(note: String? = null, end: String? = "2025-03-10T22:00") = Fast(
        id = 7,
        planId = "p16_8",
        targetMinutes = 16 * 60,
        eatingWindowMinutes = 8 * 60,
        startMillis = millis("2025-03-10T06:00"),
        endMillis = end?.let { millis(it) },
        timeZoneId = kyiv.id,
        note = note,
    )

    @Test
    fun `the header comes first and every fast gets a row`() {
        val csv = HistoryCsv.export(listOf(fast()))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("id,planId,targetMinutes"))
        assertTrue(lines[1].startsWith("7,p16_8,960,480,"))
    }

    @Test
    fun `duration is written in whole minutes`() {
        val csv = HistoryCsv.export(listOf(fast()))
        assertTrue("16h is 960 minutes", csv.contains(",960,"))
    }

    @Test
    fun `a running fast leaves the end columns empty`() {
        val csv = HistoryCsv.export(listOf(fast(end = null)))
        val fields = csv.trim().lines()[1].split(",")
        assertEquals("", fields[5])
        assertEquals("", fields[7])
    }

    @Test
    fun `notes with commas and quotes cannot break a row`() {
        val csv = HistoryCsv.export(listOf(fast(note = "боксував, потім \"їв\"")))
        val line = csv.trim().lines()[1]
        assertTrue(line.endsWith("\"боксував, потім \"\"їв\"\"\""))
        // Escaped correctly, the row still has the expected column count.
        assertEquals(11, splitCsv(line).size)
    }

    @Test
    fun `rows are ordered oldest first`() {
        val older = fast().copy(id = 1, startMillis = millis("2025-03-09T06:00"))
        val newer = fast().copy(id = 2)
        val ids = HistoryCsv.export(listOf(newer, older))
            .trim().lines().drop(1).map { it.substringBefore(",") }
        assertEquals(listOf("1", "2"), ids)
    }

    /** Minimal RFC-4180 split, enough to count columns in the test. */
    private fun splitCsv(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"'); index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> { fields += current.toString(); current.clear() }
                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }
}
