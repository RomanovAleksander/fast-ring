package com.oleksandr.fastflow.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.DualProgressRing
import com.oleksandr.fastflow.ui.components.WeekStrip
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.OverlineStyle
import com.oleksandr.fastflow.ui.theme.TimerRingStyle
import com.oleksandr.fastflow.ui.theme.TimerSecondsStyle
import com.oleksandr.fastflow.ui.theme.rememberAnimationsEnabled
import com.oleksandr.fastflow.ui.util.ClockFormat
import com.oleksandr.fastflow.ui.util.rememberUse24Hour
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDay: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onStart = { viewModel.startFast() },
        onStop = { viewModel.endFast() },
        onOpenDay = onOpenDay,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val palette = LocalAppPalette.current
    val animationsEnabled = rememberAnimationsEnabled()
    val use24Hour = rememberUse24Hour(state.use24HourClock)
    val zone = rememberZoneId()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Phase caption and plan chip, top-left (SPEC 5.3).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(state.phase.captionRes()),
                style = OverlineStyle,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = planLabel(state),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )
        }

        Spacer(Modifier.height(24.dp))

        DualProgressRing(
            outerProgress = state.outerProgress,
            outerColor = if (state.phase == HomePhase.FASTING) palette.fasting else palette.success,
            innerProgress = state.innerProgress,
            innerColor = when (state.phase) {
                HomePhase.OVERTIME -> palette.success
                else -> palette.eating
            },
            showInnerRing = state.showInnerRing,
            animationsEnabled = animationsEnabled,
        ) {
            RingCenter(state = state)
        }

        Spacer(Modifier.height(16.dp))

        // "20:00 → 12:00"
        val start = state.startMillis
        val end = state.plannedEndMillis
        if (start != null && end != null) {
            Text(
                text = stringResource(
                    R.string.home_window_range,
                    ClockFormat.time(start, zone, use24Hour),
                    ClockFormat.time(end, zone, use24Hour),
                ),
                style = AppTypography.bodyMedium,
                color = palette.textSecondary,
            )
        }

        Spacer(Modifier.height(24.dp))

        WeekStrip(
            days = state.week,
            today = LocalDate.now(zone),
            onDayClick = onOpenDay,
        )

        Spacer(Modifier.weight(1f))

        when (state.phase) {
            HomePhase.IDLE -> CapsuleButton(
                text = stringResource(R.string.action_start),
                onClick = onStart,
                style = CapsuleStyle.FILLED,
            )

            HomePhase.FASTING, HomePhase.OVERTIME -> CapsuleButton(
                text = stringResource(R.string.action_stop),
                onClick = onStop,
                style = CapsuleStyle.TINTED,
                accent = if (state.phase == HomePhase.OVERTIME) palette.success else palette.fasting,
            )

            HomePhase.EATING -> CapsuleButton(
                text = stringResource(R.string.action_start_fasting),
                onClick = onStart,
                style = CapsuleStyle.TINTED,
                accent = palette.eating,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Big timer plus the caption underneath it (SPEC 5.2). */
@Composable
private fun RingCenter(state: HomeUiState) {
    val palette = LocalAppPalette.current
    val use24Hour = rememberUse24Hour(state.use24HourClock)
    val zone = rememberZoneId()

    val displayMillis = when (state.phase) {
        HomePhase.EATING -> state.eatingRemainingMillis
        HomePhase.IDLE -> 0L
        else -> state.elapsedMillis
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = DurationFormat.hhmm(displayMillis),
                style = TimerRingStyle,
                color = palette.textPrimary,
            )
            Text(
                text = DurationFormat.seconds(displayMillis),
                style = TimerSecondsStyle,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }

        val caption: String? = when (state.phase) {
            HomePhase.FASTING ->
                stringResource(R.string.home_remaining, DurationFormat.compact(state.remainingMillis))

            HomePhase.OVERTIME ->
                stringResource(R.string.home_overtime, DurationFormat.compact(state.overtimeMillis))

            HomePhase.EATING ->
                stringResource(R.string.home_eating_window, DurationFormat.compact(state.eatingRemainingMillis))

            HomePhase.IDLE -> null
        }

        if (caption != null) {
            Text(
                text = caption,
                style = AppTypography.bodyMedium,
                color = if (state.phase == HomePhase.OVERTIME) palette.success else palette.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        // After finishing early, show how long is left to still earn the day.
        val deadline = state.creditDeadlineMillis
        if (state.phase == HomePhase.EATING && deadline != null) {
            Text(
                text = stringResource(
                    R.string.home_credit_hint,
                    ClockFormat.time(deadline, zone, use24Hour),
                ),
                style = AppTypography.bodySmall,
                color = palette.partial,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
            )
        }
    }
}

private fun HomePhase.captionRes(): Int = when (this) {
    HomePhase.FASTING, HomePhase.OVERTIME -> R.string.state_fasting
    HomePhase.EATING -> R.string.state_eating
    HomePhase.IDLE -> R.string.state_idle
}

/** Extended plans read "36 год"; daily ones already carry a "16:8" label. */
@Composable
private fun planLabel(state: HomeUiState): String =
    if (state.isExtended) {
        stringResource(
            R.string.plan_extended_hours,
            DurationFormat.hoursLabel(state.planFastingMinutes),
        )
    } else {
        state.planName.ifEmpty {
            "${state.planFastingMinutes / 60}:${(state.planEatingMinutes ?: 0) / 60}"
        }
    }

/** The device zone, resolved once per composition. */
@Composable
private fun rememberZoneId(): ZoneId = remember { ZoneId.systemDefault() }
