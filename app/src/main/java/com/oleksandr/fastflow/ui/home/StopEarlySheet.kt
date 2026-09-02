package com.oleksandr.fastflow.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.util.ClockFormat
import java.time.ZoneId

/**
 * Confirmation for ending a fast short of its goal (SPEC 3.2).
 *
 * Shows what the user still has to gain: either they are already past the 90 %
 * threshold, or there is a time by which starting the next fast would earn the
 * day back through compensation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopEarlySheet(
    percent: Int,
    compensationDeadlineMillis: Long?,
    zone: ZoneId,
    use24Hour: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
        scrimColor = palette.background.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.stop_early_title),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )
            Text(
                text = stringResource(R.string.stop_early_progress, percent),
                style = AppTypography.bodyLarge,
                color = palette.textSecondary,
            )
            Text(
                text = if (compensationDeadlineMillis != null) {
                    stringResource(
                        R.string.stop_early_compensate,
                        ClockFormat.time(compensationDeadlineMillis, zone, use24Hour),
                    )
                } else {
                    stringResource(R.string.stop_early_threshold)
                },
                style = AppTypography.bodyMedium,
                color = palette.partial,
                textAlign = TextAlign.Center,
            )

            CapsuleButton(
                text = stringResource(R.string.stop_early_confirm),
                onClick = onConfirm,
                style = CapsuleStyle.FILLED,
                accent = palette.partial,
                modifier = Modifier.padding(top = 8.dp),
            )
            CapsuleButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = CapsuleStyle.TINTED,
            )
        }
    }
}
