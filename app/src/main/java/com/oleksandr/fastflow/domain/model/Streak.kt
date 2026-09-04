package com.oleksandr.fastflow.domain.model

/**
 * Consecutive successful days.
 *
 * Any PARTIAL or MISSED day resets the current streak — deliberately strict,
 * with no weekend or rest-day exceptions (SPEC 3.4).
 */
data class Streak(
    val current: Int = 0,
    val longest: Int = 0,
)
