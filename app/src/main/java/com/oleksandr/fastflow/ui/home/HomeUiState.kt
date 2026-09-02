package com.oleksandr.fastflow.ui.home

import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.FastingPlan

/** Which of the four Home states is on screen (SPEC 5.3). */
enum class HomePhase { IDLE, FASTING, OVERTIME, EATING }

/**
 * Everything Home draws, as data.
 *
 * Formatting and string lookup happen in the composable, so the ViewModel
 * stays free of resources and the UI free of business logic (CLAUDE.md).
 */
data class HomeUiState(
    val phase: HomePhase = HomePhase.IDLE,
    val planId: String = FastingPlan.DEFAULT_ID,
    val planName: String = "",
    val planFastingMinutes: Int = 16 * 60,
    val planEatingMinutes: Int? = 8 * 60,

    val elapsedMillis: Long = 0L,
    val targetMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val overtimeMillis: Long = 0L,

    val outerProgress: Float = 0f,
    val innerProgress: Float = 0f,
    val showInnerRing: Boolean = true,

    val startMillis: Long? = null,
    val plannedEndMillis: Long? = null,
    val eatingEndsAtMillis: Long? = null,
    val eatingRemainingMillis: Long = 0L,
    /** Start the next fast before this to still earn the day (SPEC 3.4b). */
    val creditDeadlineMillis: Long? = null,
    /** Overtime target the inner ring is filling towards. */
    val nextMilestoneMinutes: Int? = null,

    val week: List<DayInfo> = emptyList(),
    val use24HourClock: Boolean? = null,
) {
    val isExtended: Boolean get() = planEatingMinutes == null

    /** Fraction of the goal reached, for the "finish early?" sheet. */
    val completionPercent: Int
        get() = if (targetMillis <= 0L) 100 else ((elapsedMillis * 100) / targetMillis).toInt()

    val canFinishEarly: Boolean get() = phase == HomePhase.FASTING
}
