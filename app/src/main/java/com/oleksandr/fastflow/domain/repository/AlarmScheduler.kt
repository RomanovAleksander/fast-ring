package com.oleksandr.fastflow.domain.repository

/**
 * Schedules the app's exact alarms.
 *
 * The interface lives in `domain` (free of `android.*`) so use cases can call
 * it; SPEC 4 puts the AlarmManager-backed implementation in `alarm/`.
 */
interface AlarmScheduler {
    /**
     * Cancels every known request code and re-plans from current state.
     *
     * Must be idempotent: called on app start, after boot, and after every
     * write to the fast repository (SPEC 3.3).
     */
    suspend fun rescheduleAll()

    /** Drops every alarm the app owns, without re-planning. */
    fun cancelAll()
}
