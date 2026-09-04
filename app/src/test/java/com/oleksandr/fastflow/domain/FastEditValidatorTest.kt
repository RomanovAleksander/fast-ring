package com.oleksandr.fastflow.domain

import com.oleksandr.fastflow.domain.logic.FastEditError
import com.oleksandr.fastflow.domain.logic.FastEditResult
import com.oleksandr.fastflow.domain.logic.FastEditValidator
import com.oleksandr.fastflow.domain.model.Fast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC 7, case 11. */
class FastEditValidatorTest {

    private val now = 1_760_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun fast(startMillis: Long, endMillis: Long? = null) = Fast(
        planId = "p16_8",
        targetMinutes = 16 * 60,
        eatingWindowMinutes = 8 * 60,
        startMillis = startMillis,
        endMillis = endMillis,
        timeZoneId = "Europe/Kyiv",
    )

    @Test
    fun `a start moved back more than seven days is rejected`() {
        val result = FastEditValidator.validate(fast(now - 8 * day), now)
        assertEquals(FastEditResult.Invalid(FastEditError.START_TOO_FAR_BACK), result)
    }

    @Test
    fun `a start six days back is accepted`() {
        assertTrue(FastEditValidator.validate(fast(now - 6 * day), now) is FastEditResult.Valid)
    }

    @Test
    fun `a start in the future is rejected`() {
        val result = FastEditValidator.validate(fast(now + 60_000L), now)
        assertEquals(FastEditResult.Invalid(FastEditError.START_IN_FUTURE), result)
    }

    @Test
    fun `an end before the start is rejected`() {
        val result = FastEditValidator.validate(fast(now - day, now - 2 * day), now)
        assertEquals(FastEditResult.Invalid(FastEditError.END_BEFORE_START), result)
    }

    @Test
    fun `an end in the future is rejected`() {
        val result = FastEditValidator.validate(fast(now - day, now + day), now)
        assertEquals(FastEditResult.Invalid(FastEditError.END_IN_FUTURE), result)
    }

    @Test
    fun `a plain finished fast passes`() {
        assertTrue(
            FastEditValidator.validate(fast(now - day, now - day / 2), now) is FastEditResult.Valid,
        )
    }
}
