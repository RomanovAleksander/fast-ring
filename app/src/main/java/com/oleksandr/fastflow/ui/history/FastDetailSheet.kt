package com.oleksandr.fastflow.ui.history

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.Fast
import com.oleksandr.fastflow.domain.model.FastOutcome
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.DateTimeDialog
import com.oleksandr.fastflow.ui.components.GroupedDivider
import com.oleksandr.fastflow.ui.components.GroupedRow
import com.oleksandr.fastflow.ui.components.GroupedSection
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.NumericStyle
import com.oleksandr.fastflow.ui.util.ClockFormat
import java.time.ZoneId

/** Which end of the fast the date picker is editing. */
private enum class EditTarget { START, END }

/**
 * Detail and editor for one recorded fast (SPEC 5.3, 6 phase 4).
 *
 * Changing either time re-scores the day and re-plans alarms, which the edit
 * use case handles, so the list and streaks update on their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastDetailSheet(
    fast: Fast,
    outcome: FastOutcome,
    zone: ZoneId,
    use24Hour: Boolean,
    onSave: (Fast) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current
    var draft by remember(fast.id) { mutableStateOf(fast) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.history_detail_title),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )

            GroupedSection {
                DetailRow(
                    label = stringResource(R.string.history_start),
                    value = datedTime(draft.startMillis, zone, use24Hour),
                    onClick = { editing = EditTarget.START },
                )
                GroupedDivider()
                DetailRow(
                    label = stringResource(R.string.history_end),
                    value = draft.endMillis?.let { datedTime(it, zone, use24Hour) }.orEmpty(),
                    onClick = { if (draft.endMillis != null) editing = EditTarget.END },
                )
                GroupedDivider()
                GroupedRow {
                    Text(
                        text = stringResource(R.string.history_duration),
                        style = AppTypography.bodyLarge,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = draft.actualDuration
                            ?.let { DurationFormat.hhmm(it.toMillis()) }
                            .orEmpty(),
                        style = NumericStyle,
                        color = palette.textSecondary,
                    )
                }
                GroupedDivider()
                GroupedRow {
                    Text(
                        text = stringResource(R.string.history_goal),
                        style = AppTypography.bodyLarge,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = DurationFormat.hhmm(draft.targetMinutes * 60_000L),
                        style = NumericStyle,
                        color = palette.textSecondary,
                    )
                }
            }

            if (outcome == FastOutcome.COMPENSATED) {
                Text(
                    text = "↺ " + stringResource(R.string.history_compensated),
                    style = AppTypography.bodyMedium,
                    color = palette.success,
                )
            }

            CapsuleButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(draft) },
                style = CapsuleStyle.FILLED,
            )
            CapsuleButton(
                text = stringResource(R.string.action_delete),
                onClick = onDelete,
                style = CapsuleStyle.TINTED,
                accent = palette.missed,
            )
        }
    }

    when (editing) {
        EditTarget.START -> DateTimeDialog(
            title = stringResource(R.string.history_start),
            initialMillis = draft.startMillis,
            zone = zone,
            use24Hour = use24Hour,
            onConfirm = { millis ->
                draft = draft.copy(startMillis = millis)
                editing = null
            },
            onDismiss = { editing = null },
        )

        EditTarget.END -> DateTimeDialog(
            title = stringResource(R.string.history_end),
            initialMillis = draft.endMillis ?: draft.startMillis,
            zone = zone,
            use24Hour = use24Hour,
            onConfirm = { millis ->
                draft = draft.copy(endMillis = millis)
                editing = null
            },
            onDismiss = { editing = null },
        )

        null -> Unit
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    GroupedRow(onClick = onClick) {
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = NumericStyle, color = palette.textSecondary)
        Text(text = "›", style = AppTypography.bodyLarge, color = palette.textTertiary)
    }
}

/** "12 березня, 20:00" in whichever clock format the user chose. */
private fun datedTime(millis: Long, zone: ZoneId, use24Hour: Boolean): String =
    ClockFormat.date(millis, zone, "d MMMM") + ", " +
        ClockFormat.time(millis, zone, use24Hour)
