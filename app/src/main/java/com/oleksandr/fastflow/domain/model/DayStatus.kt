package com.oleksandr.fastflow.domain.model

import java.time.LocalDate

/** Calendar status of a single day (SPEC 3.4). */
enum class DayStatus {
    /** Goal met, either on the 90 % threshold or through compensation. */
    SUCCESS,

    /** A fast covered the day but fell short. */
    PARTIAL,

    /** Covered by a fast that is still running. */
    ACTIVE,

    /** A past day inside the history range with no fast at all. */
    MISSED,

    /** Before the first fast, or in the future. */
    NONE,

    /** Reserved for manual rest days (v1.1); never produced in v1. */
    REST,
}

/**
 * A day as the calendar draws it.
 *
 * @param compensated day was credited only by the compensation rule, so the UI
 *   shows ↺ instead of ✓ (SPEC 3.4).
 * @param completionRatio best goal completion among the day's fasts, used to
 *   fill the day's mini ring.
 */
data class DayInfo(
    val date: LocalDate,
    val status: DayStatus,
    val compensated: Boolean = false,
    val completionRatio: Float = 0f,
)

/** How a finished fast scored (SPEC 3.4). */
enum class FastOutcome {
    /** Reached at least 90 % of the goal. */
    SUCCESS,

    /** Fell short, but the next fast started early enough to pay it back. */
    COMPENSATED,

    /** Fell short with no compensation. */
    PARTIAL,

    /** Still running, so it cannot be scored yet. */
    UNFINISHED;

    val isSuccess: Boolean get() = this == SUCCESS || this == COMPENSATED
}
