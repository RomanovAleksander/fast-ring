package com.oleksandr.fastflow.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/**
 * Picks a time of day, with a way to switch the reminder off entirely.
 *
 * SPEC 3.5 asks for "time / off" on the daily reminder, which a cycling row
 * could not express.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    title: String,
    initialMinuteOfDay: Int?,
    use24Hour: Boolean,
    onConfirm: (Int) -> Unit,
    onTurnOff: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val start = initialMinuteOfDay ?: DEFAULT_MINUTE_OF_DAY
    val timeState = rememberTimePickerState(
        initialHour = start / 60,
        initialMinute = start % 60,
        is24Hour = use24Hour,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(text = title, style = AppTypography.titleMedium, color = palette.textPrimary) },
        text = {
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timeState.hour * 60 + timeState.minute) }) {
                Text(stringResource(R.string.action_save), color = palette.fasting)
            }
        },
        dismissButton = {
            TextButton(onClick = onTurnOff) {
                Text(stringResource(R.string.settings_off), color = palette.textSecondary)
            }
        },
    )
}

/** 20:00 — the evening slot most people set a fasting reminder for. */
private const val DEFAULT_MINUTE_OF_DAY = 20 * 60
