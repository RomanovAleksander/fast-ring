package com.oleksandr.fastflow.domain.model

/** Everything stored in DataStore (SPEC 4.1). */
data class AppSettings(
    val activePlanId: String = FastingPlan.DEFAULT_ID,
    /** Begin the next fast by itself when the eating window closes. */
    val autoStartNextFast: Boolean = false,
    /** Tracking is on hold: no countdown, no alarms, no auto-start. */
    val trackingPaused: Boolean = false,
    /** Lead time for the "eating window closes soon" alarm; `null` = off. */
    val eatingEndReminderMinutes: Int? = 60,
    /** Minutes past midnight for the daily nudge; `null` = off. */
    val dailyReminderMinuteOfDay: Int? = null,
    val milestonesEnabled: Boolean = true,
    val palette: ThemePalette = ThemePalette.DEFAULT,
    /** `null` follows the system's 12/24-hour setting. */
    val use24HourClock: Boolean? = null,
    val onboardingDone: Boolean = false,
    /** One-off Samsung battery-optimisation hint has been shown (SPEC 8). */
    val batteryHintShown: Boolean = false,
) {
    companion object {
        val EATING_REMINDER_CHOICES = listOf(15, 30, 60)
    }
}
