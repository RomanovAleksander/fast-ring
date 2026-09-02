package com.oleksandr.fastflow.domain.model

/** A recorded fast together with how it scored (SPEC 5.3, history list). */
data class HistoryEntry(
    val fast: Fast,
    val outcome: FastOutcome,
)
