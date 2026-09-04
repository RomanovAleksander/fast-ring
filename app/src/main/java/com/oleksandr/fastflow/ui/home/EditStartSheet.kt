package com.oleksandr.fastflow.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.SegmentedControl
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Picks when a fast actually began — "I started at 20:00" (SPEC 3.2).
 *
 * Offers today or yesterday rather than a full date picker: the start may not
 * be in the future, and in practice it is one of those two days.
 *
 * @param title what the sheet is for: correcting a running fast's start, or
 *   naming the moment a new one really began.
 * @param earliestMillis when the previous fast ended, if there is one. A start
 *   before it would overlap two fasts, which the totals and the day statuses
 *   have no way to read sensibly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStartSheet(
    currentStartMillis: Long,
    nowMillis: Long,
    zone: ZoneId,
    use24Hour: Boolean,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.home_edit_start),
    earliestMillis: Long? = null,
) {
    val palette = LocalAppPalette.current
    val startDateTime = remember(currentStartMillis) {
        Instant.ofEpochMilli(currentStartMillis).atZone(zone)
    }
    val today = remember(nowMillis) { Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate() }

    var dayIndex by remember {
        mutableIntStateOf(if (startDateTime.toLocalDate() == today) 0 else 1)
    }
    val timeState = rememberTimePickerState(
        initialHour = startDateTime.hour,
        initialMinute = startDateTime.minute,
        is24Hour = use24Hour,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
        scrimColor = palette.background.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A clock dial plus two buttons is taller than a short screen
                // has room for, and the buttons are what ends up cut off.
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )

            SegmentedControl(
                options = listOf(
                    stringResource(R.string.edit_start_today),
                    stringResource(R.string.edit_start_yesterday),
                ),
                selectedIndex = dayIndex,
                onSelect = { dayIndex = it },
            )

            TimePicker(
                state = timeState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = palette.surfaceVariant,
                    selectorColor = palette.fasting,
                    clockDialSelectedContentColor = palette.background,
                    clockDialUnselectedContentColor = palette.textPrimary,
                    timeSelectorSelectedContainerColor = palette.fasting.copy(alpha = 0.18f),
                    timeSelectorSelectedContentColor = palette.fasting,
                    timeSelectorUnselectedContainerColor = palette.surfaceVariant,
                    timeSelectorUnselectedContentColor = palette.textPrimary,
                ),
            )

            // Recomputed as the dial turns, so an impossible time is refused
            // before it is tapped rather than swallowed by the validator after.
            val chosen = run {
                val date = if (dayIndex == 0) today else today.minusDays(1)
                date.atTime(LocalTime.of(timeState.hour, timeState.minute))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }
            val refusal: Int? = when {
                chosen > nowMillis -> R.string.edit_start_future
                earliestMillis != null && chosen < earliestMillis -> R.string.edit_start_before_previous
                else -> null
            }
            if (refusal != null) {
                Text(
                    text = stringResource(refusal),
                    style = AppTypography.bodyMedium,
                    color = palette.partial,
                )
            }

            CapsuleButton(
                text = stringResource(R.string.action_save),
                onClick = { onConfirm(chosen) },
                style = CapsuleStyle.FILLED,
                enabled = refusal == null,
            )
            CapsuleButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = CapsuleStyle.TINTED,
            )
        }
    }
}
