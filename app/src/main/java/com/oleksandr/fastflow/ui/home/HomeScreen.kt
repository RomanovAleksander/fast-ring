package com.oleksandr.fastflow.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.DualProgressRing
import com.oleksandr.fastflow.ui.components.MiniRing
import com.oleksandr.fastflow.ui.components.WeekStrip
import com.oleksandr.fastflow.ui.plans.PlanPickerSheet
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion
import com.oleksandr.fastflow.ui.theme.OverlineStyle
import com.oleksandr.fastflow.ui.theme.TimerRingStyle
import com.oleksandr.fastflow.ui.theme.TimerSecondsStyle
import com.oleksandr.fastflow.ui.theme.rememberAnimationsEnabled
import com.oleksandr.fastflow.ui.util.ClockFormat
import com.oleksandr.fastflow.ui.util.rememberUse24Hour
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenDay: (LocalDate) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showStopConfirmation by remember { mutableStateOf(false) }
    var showStartEditor by remember { mutableStateOf(false) }
    // Holds the moment the sheet was opened, so the one-second tick underneath
    // cannot move the dial while it is being read.
    var startPickerSeed by remember { mutableStateOf<Long?>(null) }
    var showPlanPicker by remember { mutableStateOf(false) }

    val zone = rememberZoneId()
    val use24Hour = rememberUse24Hour(state.use24HourClock)

    HomeContent(
        state = state,
        // Stopping short of the goal asks first; past 90 % it just stops.
        onStop = { if (state.needsStopConfirmation) showStopConfirmation = true else viewModel.endFast() },
        onStart = { viewModel.startFast() },
        onEditStart = { showStartEditor = true },
        onOpenPlans = { showPlanPicker = true },
        onOpenDay = onOpenDay,
        onSetPaused = viewModel::setPaused,
        onStartEarlier = { startPickerSeed = state.nowMillis },
    )

    val pickerSeed = startPickerSeed
    if (pickerSeed != null) {
        EditStartSheet(
            currentStartMillis = pickerSeed,
            nowMillis = state.nowMillis,
            zone = zone,
            use24Hour = use24Hour,
            title = stringResource(R.string.home_start_at),
            earliestMillis = state.earliestStartMillis,
            onConfirm = { millis ->
                startPickerSeed = null
                viewModel.startFast(millis)
            },
            onDismiss = { startPickerSeed = null },
        )
    }

    if (showPlanPicker) {
        PlanPickerSheet(
            plans = state.plans,
            activePlanId = state.planId,
            onSelect = { id ->
                showPlanPicker = false
                viewModel.selectPlan(id)
            },
            onCreateCustom = { fasting, eating ->
                showPlanPicker = false
                viewModel.createCustomPlan(fasting, eating)
            },
            onDismiss = { showPlanPicker = false },
        )
    }

    if (showStopConfirmation) {
        StopEarlySheet(
            percent = state.completionPercent,
            compensationDeadlineMillis = state.stopEarlyDeadlineMillis,
            zone = zone,
            use24Hour = use24Hour,
            onConfirm = {
                showStopConfirmation = false
                viewModel.endFast()
            },
            onDismiss = { showStopConfirmation = false },
        )
    }

    val editableStart = state.startMillis
    if (showStartEditor && editableStart != null) {
        EditStartSheet(
            currentStartMillis = editableStart,
            nowMillis = state.nowMillis,
            zone = zone,
            use24Hour = use24Hour,
            earliestMillis = state.earliestStartMillis,
            onConfirm = { millis ->
                showStartEditor = false
                viewModel.editStart(millis)
            },
            onDismiss = { showStartEditor = false },
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEditStart: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onStartEarlier: () -> Unit,
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
        // Phase caption and plan chip on the left, streak on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(state.phase.captionRes()),
                    style = OverlineStyle,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = planLabel(state) + " ⌄",
                    style = AppTypography.headlineSmall,
                    color = palette.textPrimary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        // Always tappable: a change during a fast applies to the
                        // next one, since the running goal is frozen on the record.
                        onClick = onOpenPlans,
                    ),
                )
            }
            StreakBadge(days = state.currentStreak)
        }

        Spacer(Modifier.height(24.dp))

        // The ring absorbs whatever height is left over and shrinks when there
        // is not enough: at a fixed 300dp the column overflowed on a 731dp
        // screen and pushed the last action underneath the tab bar, where it
        // could still be seen but no longer tapped.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            DualProgressRing(
                // Bounded by both axes: a short screen and a narrow one
                // each have to be able to shrink it.
                diameter = minOf(RING_MAX_DIAMETER, maxHeight, maxWidth),
                outerProgress = state.outerProgress,
                outerColor = when (state.phase) {
                    HomePhase.FASTING, HomePhase.IDLE -> palette.fasting
                    // Paused reads as switched off, so even the empty track dims.
                    HomePhase.PAUSED -> palette.textTertiary
                    HomePhase.OVERTIME -> palette.success
                    // Ended short of the goal: the ring stays partial-coloured.
                    HomePhase.EATING ->
                        if (state.previousFastEarnedDay) palette.success else palette.partial
                },
                innerProgress = state.innerProgress,
                innerColor = when (state.phase) {
                    HomePhase.OVERTIME -> palette.success
                    else -> palette.eating
                },
                showInnerRing = state.showInnerRing,
                goalReached = state.phase == HomePhase.OVERTIME ||
                    (state.phase == HomePhase.EATING && state.previousFastEarnedDay),
                animationsEnabled = animationsEnabled,
            ) {
                RingCenter(state = state)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Start and goal on one line; each half opens what it describes.
        val start = state.startMillis
        val end = state.plannedEndMillis
        if (start != null && end != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = relativeDateTime(start, zone, use24Hour),
                    style = AppTypography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = state.phase == HomePhase.FASTING ||
                                state.phase == HomePhase.OVERTIME,
                            onClick = onEditStart,
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Text(text = "→", style = AppTypography.bodyMedium, color = palette.textTertiary)
                Text(
                    text = relativeDateTime(end, zone, use24Hour),
                    style = AppTypography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            // The goal follows from the plan, so this opens the picker.
                            onClick = onOpenPlans,
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        WeekStrip(
            days = state.week,
            today = LocalDate.now(zone),
            onDayClick = onOpenDay,
        )

        Spacer(Modifier.height(24.dp))

        when (state.phase) {
            HomePhase.IDLE, HomePhase.PAUSED -> CapsuleButton(
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

        // A fast usually begins before anyone thinks to press a button — the
        // last meal, not the moment of remembering. Offered wherever the next
        // fast has yet to start; during one, the start row above edits it.
        val canBackdateStart = when (state.phase) {
            HomePhase.IDLE, HomePhase.PAUSED, HomePhase.EATING -> true
            HomePhase.FASTING, HomePhase.OVERTIME -> false
        }
        if (canBackdateStart) {
            TextAction(
                text = stringResource(R.string.action_start_earlier),
                onClick = onStartEarlier,
            )
        }

        // The way out of the loop: pausing stops the eating-window countdown,
        // the reminders and the auto-start until the user comes back. Offered
        // only where there is a loop to leave — never mid-fast, where the
        // honest action is to stop the fast.
        val pauseAction: Pair<Int, Boolean>? = when (state.phase) {
            HomePhase.IDLE, HomePhase.EATING -> R.string.action_pause to true
            HomePhase.PAUSED -> R.string.action_resume_tracking to false
            HomePhase.FASTING, HomePhase.OVERTIME -> null
        }
        if (pauseAction != null) {
            TextAction(
                text = stringResource(pauseAction.first),
                onClick = { onSetPaused(pauseAction.second) },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** As large as SPEC 5.2 asks for, where the screen has room for it. */
private val RING_MAX_DIAMETER = 300.dp

/** The quiet secondary choice under the capsule: text only, no chrome. */
@Composable
private fun TextAction(text: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Text(
        text = text,
        style = AppTypography.bodyMedium,
        color = palette.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
    )
}

/** Big timer plus the caption underneath it (SPEC 5.2). */
@Composable
private fun RingCenter(state: HomeUiState) {
    val palette = LocalAppPalette.current
    val use24Hour = rememberUse24Hour(state.use24HourClock)
    val zone = rememberZoneId()

    val displayMillis = when (state.phase) {
        HomePhase.EATING -> state.eatingRemainingMillis
        HomePhase.IDLE, HomePhase.PAUSED -> 0L
        else -> state.elapsedMillis
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = DurationFormat.hhmm(displayMillis),
                style = TimerRingStyle,
                color = palette.textPrimary,
            )
            AnimatedContent(
                targetState = DurationFormat.seconds(displayMillis),
                transitionSpec = {
                    val duration = Motion.DIGIT_FLIP_MILLIS
                    (slideInVertically { height -> height } + fadeIn(tween(duration)))
                        .togetherWith(
                            slideOutVertically { height -> -height } + fadeOut(tween(duration)),
                        )
                },
                label = "seconds",
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            ) { seconds ->
                Text(text = seconds, style = TimerSecondsStyle, color = palette.textSecondary)
            }
        }

        val caption: String? = when (state.phase) {
            HomePhase.FASTING ->
                stringResource(R.string.home_remaining, DurationFormat.compact(state.remainingMillis))

            HomePhase.OVERTIME ->
                stringResource(R.string.home_overtime, DurationFormat.compact(state.overtimeMillis))

            HomePhase.EATING ->
                stringResource(R.string.home_eating_window, DurationFormat.compact(state.eatingRemainingMillis))

            HomePhase.PAUSED -> stringResource(R.string.home_paused_hint)

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

/**
 * Successful days in a row, top-right (the one number worth glancing at).
 *
 * A ring rather than a plain number, so it echoes the main element instead of
 * introducing a new shape.
 */
@Composable
private fun StreakBadge(days: Int) {
    val palette = LocalAppPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            MiniRing(
                // Full at a week, so early streaks still show visible movement.
                progress = (days.coerceAtLeast(0) % STREAK_RING_DAYS) / STREAK_RING_DAYS.toFloat(),
                color = if (days > 0) palette.success else palette.textTertiary,
                size = 44.dp,
                strokeWidth = 3.dp,
            )
            Text(
                text = days.toString(),
                style = AppTypography.titleMedium,
                color = if (days > 0) palette.textPrimary else palette.textTertiary,
            )
        }
        Text(
            text = stringResource(R.string.home_streak_caption),
            style = AppTypography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** One ring turn per week. */
private const val STREAK_RING_DAYS = 7

/** "Сьогодні 14:58", "Учора 22:58", otherwise "Ср 22:58". */
@Composable
private fun relativeDateTime(millis: Long, zone: ZoneId, use24Hour: Boolean): String {
    val date = remember(millis, zone) {
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }
    val today = remember(zone) { LocalDate.now(zone) }
    val time = ClockFormat.time(millis, zone, use24Hour)

    val dayLabel = when (date) {
        today -> stringResource(R.string.edit_start_today)
        today.minusDays(1) -> stringResource(R.string.edit_start_yesterday)
        today.plusDays(1) -> stringResource(R.string.home_tomorrow)
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("uk")).replaceFirstChar { it.uppercase() }
    }
    return "$dayLabel $time"
}

private fun HomePhase.captionRes(): Int = when (this) {
    HomePhase.FASTING, HomePhase.OVERTIME -> R.string.state_fasting
    HomePhase.EATING -> R.string.state_eating
    HomePhase.PAUSED -> R.string.state_paused
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
