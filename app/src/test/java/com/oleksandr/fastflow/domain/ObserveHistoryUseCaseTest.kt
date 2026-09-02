package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome
import com.oleksandr.fastflow.domain.usecase.ObserveHistoryUseCase
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveHistoryUseCaseTest {

    private val kyiv: ZoneId = ZoneId.of("Europe/Kyiv")

    private fun millis(text: String) =
        LocalDateTime.parse(text).atZone(kyiv).toInstant().toEpochMilli()

    private fun fast(start: String, end: String?) = Fast(
        planId = "p16_8",
        targetMinutes = 16 * 60,
        eatingWindowMinutes = 8 * 60,
        startMillis = millis(start),
        endMillis = end?.let { millis(it) },
        timeZoneId = kyiv.id,
    )

    @Test
    fun `history is newest first`() = runBlocking {
        val repo = InMemoryFastRepository(
            listOf(
                fast("2025-03-10T06:00", "2025-03-10T22:00").copy(id = 1),
                fast("2025-03-12T06:00", "2025-03-12T22:00").copy(id = 2),
            ),
        )
        val entries = ObserveHistoryUseCase(repo)().first()
        assertEquals(listOf(2L, 1L), entries.map { it.fast.id })
    }

    @Test
    fun `each fast is scored against the one that follows it`() = runBlocking {
        // 13h30m of a 16h goal, then a next fast 5h30m later: compensated.
        val repo = InMemoryFastRepository(
            listOf(
                fast("2025-03-10T00:00", "2025-03-10T13:30").copy(id = 1),
                fast("2025-03-10T19:00", "2025-03-11T11:00").copy(id = 2),
            ),
        )
        val entries = ObserveHistoryUseCase(repo)().first().associateBy { it.fast.id }
        assertEquals(FastOutcome.COMPENSATED, entries.getValue(1L).outcome)
        assertEquals(FastOutcome.SUCCESS, entries.getValue(2L).outcome)
    }

    @Test
    fun `a running fast is reported as unfinished`() = runBlocking {
        val repo = InMemoryFastRepository(listOf(fast("2025-03-10T06:00", null).copy(id = 1)))
        val entries = ObserveHistoryUseCase(repo)().first()
        assertEquals(FastOutcome.UNFINISHED, entries.single().outcome)
    }
}
