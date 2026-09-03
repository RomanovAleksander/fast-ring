package com.oleksandr.fastflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion
import com.oleksandr.fastflow.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month grid where each day is a small ring rather than a dot, so the fill
 * also shows how close the day came to its goal (SPEC 5.3).
 */
@Composable
fun CalendarGrid(
    month: YearMonth,
    days: Map<LocalDate, DayInfo>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val palette = LocalAppPalette.current
    val firstDay = month.atDay(1)
    // Weeks start on Monday, as they do in Ukraine.
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = leadingBlanks + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { labelRes ->
                Text(
                    text = stringResource(labelRes),
                    style = AppTypography.labelSmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        repeat(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    val dayOfMonth = index - leadingBlanks + 1
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayOfMonth in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayOfMonth)
                            DayCell(
                                date = date,
                                info = days[date],
                                isToday = date == today,
                                // Rings fill in sequence as the month appears.
                                appearOrder = dayOfMonth - 1,
                                monthKey = month,
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    info: DayInfo?,
    isToday: Boolean,
    appearOrder: Int,
    monthKey: YearMonth,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val resolved = info ?: DayInfo(date, DayStatus.NONE)
    val animationsEnabled = rememberAnimationsEnabled()

    // Staggered reveal: each ring starts 15 ms after the previous one.
    var revealed by remember(monthKey) { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(monthKey, animationsEnabled) {
        if (animationsEnabled) {
            delay(appearOrder.toLong() * Motion.CALENDAR_STAGGER_MILLIS)
            revealed = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (revealed) resolved.completionRatio else 0f,
        animationSpec = tween(Motion.BUTTON_COLOR_MILLIS),
        label = "dayRing",
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MiniRing(
            progress = progress,
            color = colorForDay(resolved),
            size = 30.dp,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = AppTypography.labelSmall,
            color = if (isToday) palette.fasting else palette.textSecondary,
        )
    }
}

private val WEEKDAY_LABELS = listOf(
    R.string.weekday_mon,
    R.string.weekday_tue,
    R.string.weekday_wed,
    R.string.weekday_thu,
    R.string.weekday_fri,
    R.string.weekday_sat,
    R.string.weekday_sun,
)
