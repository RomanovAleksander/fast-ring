package com.oleksandr.fastflow.domain.model

/** The app's state machine (SPEC 3.2). */
sealed interface FastState {

    /** Nothing running; the button offers to start. */
    data object Idle : FastState

    /** A fast is running and the goal is still ahead. */
    data class Fasting(val fast: Fast, val plan: FastingPlan) : FastState

    /** Goal reached, user has not pressed stop yet. */
    data class Overtime(val fast: Fast, val plan: FastingPlan) : FastState

    /**
     * The eating window that follows a finished daily fast.
     *
     * @param windowEndsAtMillis when the window closes.
     * @param creditDeadlineMillis if the fast ended short, starting the next
     *   fast before this instant still earns the day (SPEC 3.4b); `null` when
     *   the day is already credited.
     */
    data class Eating(
        val previousFast: Fast,
        val plan: FastingPlan,
        val windowEndsAtMillis: Long,
        val creditDeadlineMillis: Long? = null,
    ) : FastState
}

/** Overtime milestones, in minutes (SPEC 3.3 and 5.2). */
object Milestones {
    val thresholdsMinutes: List<Int> = listOf(16 * 60, 24 * 60, 36 * 60, 48 * 60, 72 * 60)

    /** The next milestone strictly above [minutes], or `null` past the last one. */
    fun next(minutes: Int): Int? = thresholdsMinutes.firstOrNull { it > minutes }

    /** Milestones worth announcing for a goal of [targetMinutes]. */
    fun above(targetMinutes: Int): List<Int> = thresholdsMinutes.filter { it > targetMinutes }
}
