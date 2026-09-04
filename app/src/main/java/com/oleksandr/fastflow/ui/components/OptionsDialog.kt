package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/** One labelled choice in an [OptionsDialog]. */
data class DialogOption<T>(val label: String, val value: T)

/**
 * A plain single-choice dialog.
 *
 * Settings rows used to cycle their value on every tap, which hid the
 * available choices and made a row with one reachable value look broken.
 */
@Composable
fun <T> OptionsDialog(
    title: String,
    options: List<DialogOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(text = title, style = AppTypography.titleMedium, color = palette.textPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    GroupedRow(
                        onClick = {
                            onSelect(option.value)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = option.label,
                            style = AppTypography.bodyLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (option.value == selected) {
                            Text(text = "✓", style = AppTypography.bodyLarge, color = palette.fasting)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = palette.textSecondary)
            }
        },
    )
}
