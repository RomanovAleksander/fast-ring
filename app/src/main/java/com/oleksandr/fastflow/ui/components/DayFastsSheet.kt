package com.oleksandr.fastflow.ui.components

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
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.FastOutcome
import com.oleksandr.fastflow.domain.model.HistoryEntry
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.NumericStyle
import com.oleksandr.fastflow.ui.util.ClockFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The fasts covering one calendar day (SPEC 3.4, tap a day in the calendar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayFastsSheet(
    date: LocalDate,
    entries: List<HistoryEntry>,
    zone: ZoneId,
    use24Hour: Boolean,
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
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk")).format(date),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )

            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.day_no_fasts),
                    style = AppTypography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            GroupedSection {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) GroupedDivider()
                    GroupedRow {
                        val start = ClockFormat.time(entry.fast.startMillis, zone, use24Hour)
                        val end = entry.fast.endMillis
                            ?.let { ClockFormat.time(it, zone, use24Hour) }
                        Text(
                            text = if (end != null) {
                                stringResource(R.string.home_window_range, start, end)
                            } else {
                                start
                            },
                            style = AppTypography.bodyLarge,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = entry.fast.actualDuration
                                ?.let { DurationFormat.hhmm(it.toMillis()) }
                                .orEmpty(),
                            style = NumericStyle,
                            color = palette.textSecondary,
                        )
                        MiniRing(
                            progress = entry.fast.endMillis
                                ?.let { entry.fast.completionRatio(it) } ?: 0f,
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
        }
    }
}
