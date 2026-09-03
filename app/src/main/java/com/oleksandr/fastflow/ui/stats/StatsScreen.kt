package com.oleksandr.fastflow.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.FastOutcome
import com.oleksandr.fastflow.ui.components.CalendarGrid
import com.oleksandr.fastflow.ui.components.DayFastsSheet
import com.oleksandr.fastflow.ui.components.DurationBar
import com.oleksandr.fastflow.ui.components.DurationBarChart
import com.oleksandr.fastflow.ui.components.GroupedDivider
import com.oleksandr.fastflow.ui.components.Heatmap
import com.oleksandr.fastflow.ui.components.GroupedRow
import com.oleksandr.fastflow.ui.components.GroupedSection
import com.oleksandr.fastflow.ui.components.SegmentedControl
import com.oleksandr.fastflow.ui.theme.AppShapes
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion
import com.oleksandr.fastflow.ui.theme.NumericStyle
import com.oleksandr.fastflow.ui.theme.StatNumberStyle
import com.oleksandr.fastflow.ui.util.rememberUse24Hour
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    focusDate: LocalDate? = null,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current
    var tab by remember { mutableIntStateOf(if (focusDate != null) CALENDAR_TAB else 0) }
    var selectedDay by remember { mutableStateOf(focusDate) }

    // Arriving from the week strip lands straight on that day in the calendar.
    LaunchedEffect(focusDate) {
        if (focusDate != null) {
            viewModel.showMonth(YearMonth.from(focusDate))
            tab = CALENDAR_TAB
            selectedDay = focusDate
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.screen_stats_title),
                style = AppTypography.displaySmall,
                color = palette.textPrimary,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        }

        item {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.stats_tab_overview),
                    stringResource(R.string.stats_tab_calendar),
                ),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        item {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(Motion.TAB_CROSSFADE_MILLIS)) togetherWith
                        fadeOut(tween(Motion.TAB_CROSSFADE_MILLIS))
                },
                label = "statsTab",
            ) { current ->
                if (current == 0) {
                    Overview(state, onDayClick = { selectedDay = it })
                } else {
                    Calendar(state, viewModel, onDayClick = { selectedDay = it })
                }
            }
        }
    }
}

    val day = selectedDay
    if (day != null) {
        val zone = remember { ZoneId.systemDefault() }
        DayFastsSheet(
            date = day,
            // A fast counts for the day it overlaps, not only the one it started on.
            entries = state.recent.filter { entry ->
                entry.fast.coveredDates(state.nowMillis).contains(day)
            },
            zone = zone,
            use24Hour = rememberUse24Hour(null),
            onDismiss = { selectedDay = null },
        )
    }
}

private const val CALENDAR_TAB = 1

@Composable
private fun Overview(state: StatsUiState, onDayClick: (LocalDate) -> Unit) {
    val palette = LocalAppPalette.current

    if (state.isEmpty) {
        Text(
            text = stringResource(R.string.stats_empty),
            style = AppTypography.bodyLarge,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp, horizontal = 24.dp),
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.stats_current_streak),
                value = state.streak.current,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.stats_longest_streak),
                value = state.streak.longest,
                modifier = Modifier.weight(1f),
            )
        }

        GroupedSection {
            StatRow(stringResource(R.string.stats_total_fasts), state.stats.totalFasts.toString())
            GroupedDivider()
            StatRow(stringResource(R.string.stats_successful), state.stats.successfulFasts.toString())
            GroupedDivider()
            StatRow(stringResource(R.string.stats_ended_early), state.stats.endedEarlyFasts.toString())
            GroupedDivider()
            StatRow(
                stringResource(R.string.stats_average),
                DurationFormat.hhmm(state.stats.averageDuration.toMillis()),
            )
            GroupedDivider()
            StatRow(
                stringResource(R.string.stats_longest),
                DurationFormat.hhmm(state.stats.longestDuration.toMillis()),
            )
            GroupedDivider()
            StatRow(
                stringResource(R.string.stats_week),
                DurationFormat.hhmm(state.stats.totalThisWeek.toMillis()),
            )
            GroupedDivider()
            StatRow(
                stringResource(R.string.stats_month),
                DurationFormat.hhmm(state.stats.totalThisMonth.toMillis()),
            )
            GroupedDivider()
            StatRow(
                stringResource(R.string.stats_all_time),
                DurationFormat.hhmm(state.stats.totalAllTime.toMillis()),
            )
        }

        Column {
            Text(
                text = stringResource(R.string.stats_heatmap_title),
                style = AppTypography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Heatmap(days = state.heatmap, today = state.today, onDayClick = onDayClick)
        }

        val finished = state.recent.filter { it.outcome != FastOutcome.UNFINISHED }.take(30)
        if (finished.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.stats_chart_title),
                    style = AppTypography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                DurationBarChart(
                    // Oldest on the left, so the chart reads left to right.
                    bars = finished.asReversed().map { entry ->
                        DurationBar(
                            millis = entry.fast.actualDuration?.toMillis() ?: 0L,
                            success = entry.outcome.isSuccess,
                        )
                    },
                    goalMillis = finished.first().fast.targetMinutes * 60_000L,
                )
            }
        }
    }
}

@Composable
private fun Calendar(
    state: StatsUiState,
    viewModel: StatsViewModel,
    onDayClick: (LocalDate) -> Unit,
) {
    val palette = LocalAppPalette.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MonthArrow("‹") { viewModel.showPreviousMonth() }
            Text(
                text = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("uk")).format(state.month),
                style = AppTypography.titleMedium,
                color = palette.textPrimary,
            )
            MonthArrow("›") { viewModel.showNextMonth() }
        }

        Spacer(Modifier.height(16.dp))

        CalendarGrid(
            month = state.month,
            days = state.days,
            today = state.today,
            onDayClick = onDayClick,
        )
    }
}

@Composable
private fun MonthArrow(glyph: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Text(
        text = glyph,
        style = AppTypography.displaySmall,
        color = palette.fasting,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    // Counts up from zero rather than snapping into place (SPEC 5.4).
    val shown by animateIntAsState(targetValue = value, animationSpec = Motion.statCount, label = "stat")
    Column(
        modifier = modifier
            .clip(AppShapes.medium)
            .background(palette.surface)
            .padding(16.dp),
    ) {
        Text(text = shown.toString(), style = StatNumberStyle, color = palette.textPrimary)
        Text(text = label, style = AppTypography.bodySmall, color = palette.textSecondary)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val palette = LocalAppPalette.current
    GroupedRow {
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = NumericStyle, color = palette.textSecondary)
    }
}
