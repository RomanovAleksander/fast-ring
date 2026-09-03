package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Picks a full instant — date and time together.
 *
 * History records can be any age, so unlike the "today or yesterday" shortcut
 * on Home this needs a real calendar (SPEC 3.2, editing history).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeDialog(
    title: String,
    initialMillis: Long,
    zone: ZoneId,
    use24Hour: Boolean,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val initial = remember(initialMillis) { Instant.ofEpochMilli(initialMillis).atZone(zone) }

    // DatePicker works in UTC, so the local date is shifted in and back out.
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = use24Hour,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(text = title, style = AppTypography.titleMedium, color = palette.textPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DatePicker(
                    state = dateState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = palette.surface,
                        selectedDayContainerColor = palette.fasting,
                        selectedDayContentColor = palette.background,
                        todayContentColor = palette.fasting,
                        todayDateBorderColor = palette.fasting,
                        dayContentColor = palette.textPrimary,
                        weekdayContentColor = palette.textSecondary,
                        subheadContentColor = palette.textSecondary,
                    ),
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dateMillis = dateState.selectedDateMillis ?: return@TextButton onDismiss()
                    val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(
                        date.atTime(LocalTime.of(timeState.hour, timeState.minute))
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli(),
                    )
                },
            ) {
                Text(stringResource(R.string.action_save), color = palette.fasting)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
            }
        },
    )
}
