package com.oleksandr.fastflow.domain.model

import java.time.Duration

/** Aggregates shown on the statistics screen (SPEC 3.4). */
data class FastStats(
    val totalFasts: Int = 0,
    val successfulFasts: Int = 0,
    val endedEarlyFasts: Int = 0,
    val averageDuration: Duration = Duration.ZERO,
    val longestDuration: Duration = Duration.ZERO,
    val totalThisWeek: Duration = Duration.ZERO,
    val totalThisMonth: Duration = Duration.ZERO,
    val totalAllTime: Duration = Duration.ZERO,
)
