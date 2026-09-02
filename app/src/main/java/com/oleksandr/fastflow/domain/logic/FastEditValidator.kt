package com.oleksandr.fastflow.domain.logic

import com.oleksandr.fastflow.domain.model.Fast

/** Why an edit to a fast's times was refused (SPEC 3.2). */
enum class FastEditError {
    START_IN_FUTURE,
    START_TOO_FAR_BACK,
    END_BEFORE_START,
    END_IN_FUTURE,
}

sealed interface FastEditResult {
    data class Valid(val fast: Fast) : FastEditResult
    data class Invalid(val error: FastEditError) : FastEditResult
}

/**
 * Guards manual start/end edits.
 *
 * A start may not be in the future and may not be dragged back more than
 * [Fast.MAX_BACKDATE_DAYS] days (SPEC 7, case 11).
 */
object FastEditValidator {

    fun validate(fast: Fast, nowMillis: Long): FastEditResult {
        val earliestStart = nowMillis - Fast.MAX_BACKDATE_DAYS * 24 * 60 * 60 * 1000L

        if (fast.startMillis > nowMillis) {
            return FastEditResult.Invalid(FastEditError.START_IN_FUTURE)
        }
        if (fast.startMillis < earliestStart) {
            return FastEditResult.Invalid(FastEditError.START_TOO_FAR_BACK)
        }

        val end = fast.endMillis
        if (end != null) {
            if (end < fast.startMillis) {
                return FastEditResult.Invalid(FastEditError.END_BEFORE_START)
            }
            if (end > nowMillis) {
                return FastEditResult.Invalid(FastEditError.END_IN_FUTURE)
            }
        }
        return FastEditResult.Valid(fast)
    }
}
