package com.oleksandr.fastflow.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome
import com.oleksandr.fastflow.domain.model.HistoryEntry
import com.oleksandr.fastflow.ui.components.GroupedDivider
import com.oleksandr.fastflow.ui.components.GroupedRow
import com.oleksandr.fastflow.ui.components.GroupedSection
import com.oleksandr.fastflow.ui.components.MiniRing
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.NumericStyle
import com.oleksandr.fastflow.ui.util.ClockFormat
import com.oleksandr.fastflow.ui.util.rememberUse24Hour
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Fast?>(null) }

    val deletedLabel = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    // Deleting shows an undo snackbar; restoring re-inserts the same record.
    LaunchedEffect(pendingDelete) {
        val fast = pendingDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedLabel,
            actionLabel = undoLabel,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.restore(fast)
        pendingDelete = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isEmpty) {
            Text(
                text = stringResource(R.string.history_empty),
                style = AppTypography.bodyLarge,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.screen_history_title),
                        style = AppTypography.displaySmall,
                        color = palette.textPrimary,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 12.dp),
                    )
                }

                state.months.forEach { month ->
                    item(key = month.month.toString()) {
                        GroupedSection(header = month.month.displayName()) {
                            month.entries.forEachIndexed { index, entry ->
                                if (index > 0) GroupedDivider()
                                HistoryRow(
                                    entry = entry,
                                    use24Hour = state.use24HourClock,
                                    onDelete = {
                                        viewModel.delete(entry.fast)
                                        pendingDelete = entry.fast
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    use24Hour: Boolean?,
    onDelete: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val zone = remember { ZoneId.systemDefault() }
    val resolved24 = rememberUse24Hour(use24Hour)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Only a full swipe from the end deletes, as in iOS lists.
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.missed)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    style = AppTypography.bodyLarge,
                    color = palette.background,
                )
            }
        },
    ) {
        GroupedRow(modifier = Modifier.background(palette.surface)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ClockFormat.date(entry.fast.startMillis, zone),
                    style = AppTypography.bodyLarge,
                    color = palette.textPrimary,
                )
                val start = ClockFormat.time(entry.fast.startMillis, zone, resolved24)
                val end = entry.fast.endMillis?.let { ClockFormat.time(it, zone, resolved24) }
                if (end != null) {
                    Text(
                        text = stringResource(R.string.home_window_range, start, end),
                        style = AppTypography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }

            Text(
                text = entry.fast.actualDuration
                    ?.let { DurationFormat.hhmm(it.toMillis()) }
                    .orEmpty(),
                style = NumericStyle,
                color = palette.textSecondary,
            )

            MiniRing(
                progress = entry.fast.endMillis?.let {
                    entry.fast.completionRatio(it)
                } ?: 0f,
                color = when (entry.outcome) {
                    FastOutcome.SUCCESS, FastOutcome.COMPENSATED -> palette.success
                    FastOutcome.PARTIAL -> palette.partial
                    FastOutcome.UNFINISHED -> palette.fasting
                },
                size = 20.dp,
                strokeWidth = 2.5.dp,
            )
        }
    }
}

/** "БЕРЕЗЕНЬ 2025" — month names come from the platform's Ukrainian locale. */
private fun YearMonth.displayName(): String =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale("uk")).format(this)
