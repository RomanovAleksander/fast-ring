package com.oleksandr.fastflow.domain.usecase

import com.oleksandr.fastflow.domain.logic.FastScoring
import com.oleksandr.fastflow.domain.model.HistoryEntry
import com.oleksandr.fastflow.domain.repository.FastRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * History, newest first, with each fast already scored.
 *
 * Scoring happens here rather than in the list, because a fast's outcome
 * depends on the one that follows it (SPEC 3.4b).
 */
class ObserveHistoryUseCase @Inject constructor(
    private val fastRepository: FastRepository,
) {
    operator fun invoke(): Flow<List<HistoryEntry>> = fastRepository.observeAll().map { fasts ->
        val chronological = fasts.sortedBy { it.startMillis }
        chronological
            .mapIndexed { index, fast ->
                HistoryEntry(fast, FastScoring.outcome(fast, chronological.getOrNull(index + 1)))
            }
            .asReversed()
    }
}
