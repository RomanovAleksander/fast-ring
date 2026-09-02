package com.oleksandr.fastflow.domain.model

/**
 * A fasting protocol.
 *
 * `domain` never touches the Android SDK (CLAUDE.md), so this stays a plain
 * Kotlin type usable from unit tests.
 */
data class FastingPlan(
    val id: String,
    val name: String,
    val fastingMinutes: Int,
    /** `null` marks an extended plan: a one-off fast with no eating window. */
    val eatingMinutes: Int?,
    val isPreset: Boolean,
    val sortOrder: Int,
) {
    val isExtended: Boolean get() = eatingMinutes == null

    val isDaily: Boolean get() = eatingMinutes != null

    companion object {
        const val CUSTOM_ID = "custom"

        const val MIN_FASTING_MINUTES = 60
        const val MAX_FASTING_MINUTES = 168 * 60
        const val MAX_EATING_MINUTES = 24 * 60

        /** Custom plans are edited in 30-minute steps (SPEC 3.1). */
        const val CUSTOM_STEP_MINUTES = 30

        const val DEFAULT_ID = "p16_8"
    }
}

/** The built-in protocols from SPEC 3.1, in display order. */
object FastingPlans {
    val presets: List<FastingPlan> = listOf(
        FastingPlan("p12_12", "12:12", 12 * 60, 12 * 60, isPreset = true, sortOrder = 0),
        FastingPlan("p14_10", "14:10", 14 * 60, 10 * 60, isPreset = true, sortOrder = 1),
        FastingPlan("p16_8", "16:8", 16 * 60, 8 * 60, isPreset = true, sortOrder = 2),
        FastingPlan("p18_6", "18:6", 18 * 60, 6 * 60, isPreset = true, sortOrder = 3),
        FastingPlan("p20_4", "20:4", 20 * 60, 4 * 60, isPreset = true, sortOrder = 4),
        FastingPlan("omad", "OMAD 23:1", 23 * 60, 1 * 60, isPreset = true, sortOrder = 5),
        FastingPlan("h36", "36 год", 36 * 60, null, isPreset = true, sortOrder = 6),
        FastingPlan("h48", "48 год", 48 * 60, null, isPreset = true, sortOrder = 7),
    )

    val default: FastingPlan = presets.first { it.id == FastingPlan.DEFAULT_ID }

    fun byId(id: String): FastingPlan? = presets.firstOrNull { it.id == id }
}
