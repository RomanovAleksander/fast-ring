package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import java.time.DayOfWeek
import java.time.LocalDate

/** Monday-to-Sunday row of day rings under the main ring (SPEC 5.3). */
@Composable
fun WeekStrip(
    days: List<DayInfo>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val palette = LocalAppPalette.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { info ->
            val isToday = info.date == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDayClick(info.date) }
                    .padding(horizontal = 2.dp),
            ) {
                Text(
                    text = stringResource(info.date.dayOfWeek.shortLabelRes()),
                    style = AppTypography.labelSmall,
                    color = if (isToday) palette.textPrimary else palette.textSecondary,
                )
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    MiniRing(
                        progress = info.completionRatio,
                        color = colorForDay(info),
                        size = 28.dp,
                    )
                }
            }
        }
    }
}

private fun DayOfWeek.shortLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.weekday_mon
    DayOfWeek.TUESDAY -> R.string.weekday_tue
    DayOfWeek.WEDNESDAY -> R.string.weekday_wed
    DayOfWeek.THURSDAY -> R.string.weekday_thu
    DayOfWeek.FRIDAY -> R.string.weekday_fri
    DayOfWeek.SATURDAY -> R.string.weekday_sat
    DayOfWeek.SUNDAY -> R.string.weekday_sun
}
