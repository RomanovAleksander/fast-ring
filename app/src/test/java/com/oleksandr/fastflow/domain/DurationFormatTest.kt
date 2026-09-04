package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.DurationFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    private fun minutes(m: Long) = m * 60_000L

    @Test
    fun `hours are never folded into days`() {
        assertEquals("36:00", DurationFormat.hhmm(minutes(36 * 60)))
        assertEquals("48:30", DurationFormat.hhmm(minutes(48 * 60 + 30)))
    }

    @Test
    fun `short durations stay zero padded`() {
        assertEquals("00:00", DurationFormat.hhmm(0))
        assertEquals("00:09", DurationFormat.hhmm(minutes(9)))
        assertEquals("01:05", DurationFormat.hhmm(minutes(65)))
    }

    @Test
    fun `the overtime caption drops the leading zero`() {
        assertEquals("1:05", DurationFormat.compact(minutes(65)))
        assertEquals("0:07", DurationFormat.compact(minutes(7)))
    }

    @Test
    fun `seconds are taken modulo a minute`() {
        assertEquals("00", DurationFormat.seconds(0))
        assertEquals("07", DurationFormat.seconds(7_000))
        assertEquals("59", DurationFormat.seconds(119_000))
    }

    @Test
    fun `negative durations read as zero rather than going backwards`() {
        assertEquals("00:00", DurationFormat.hhmm(-5_000))
        assertEquals("00:00:00", DurationFormat.hhmmss(-5_000))
    }

    @Test
    fun `plan labels keep half hours readable`() {
        assertEquals("16", DurationFormat.hoursLabel(16 * 60))
        assertEquals("13.5", DurationFormat.hoursLabel(13 * 60 + 30))
    }
}
