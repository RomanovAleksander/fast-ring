package com.oleksandr.fastflow.domain

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's source of "now".
 *
 * An interface rather than direct `System.currentTimeMillis()` calls so day
 * boundaries, streaks and alarms can be tested at fixed instants.
 */
interface AppClock {
    fun nowMillis(): Long

    fun zone(): ZoneId

    fun today(): LocalDate = java.time.Instant.ofEpochMilli(nowMillis()).atZone(zone()).toLocalDate()
}

/** Production clock: the device's wall clock and current zone. */
@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
