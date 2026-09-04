package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Twelve weeks at a glance, GitHub-style (SPEC 3.4).
 *
 * Columns are weeks running left to right, rows are Monday to Sunday. Squares
 * rather than rings: at this size a ring is unreadable, and the point here is
 * the pattern of kept and missed days, not how close each one came.
 */
@Composable
fun Heatmap(
    days: Map<LocalDate, DayInfo>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val palette = LocalAppPalette.current
    val firstMonday = remember(today, weeks) {
        today.minusWeeks(weeks.toLong()).with(DayOfWeek.MONDAY)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP.dp),
    ) {
        DayOfWeek.entries.forEach { dayOfWeek ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP.dp)) {
                repeat(weeks + 1) { week ->
                    val date = firstMonday
                        .plusWeeks(week.toLong())
                        .with(dayOfWeek)
                    val info = days[date]

                    val color = when {
                        date.isAfter(today) -> palette.surfaceVariant.copy(alpha = 0.4f)
                        info == null -> palette.surfaceVariant
                        else -> when (info.status) {
                            DayStatus.SUCCESS -> palette.success
                            DayStatus.PARTIAL -> palette.partial
                            DayStatus.ACTIVE -> palette.fasting
                            DayStatus.MISSED -> palette.missed.copy(alpha = 0.5f)
                            DayStatus.NONE, DayStatus.REST -> palette.surfaceVariant
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(CELL_SIZE.dp)
                            .background(color, RoundedCornerShape(3.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !date.isAfter(today),
                            ) { onDayClick(date) },
                    )
                }
            }
        }
    }
}

private const val CELL_SIZE = 14
private const val CELL_GAP = 3
