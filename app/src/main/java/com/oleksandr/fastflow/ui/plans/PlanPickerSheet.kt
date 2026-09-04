package com.oleksandr.fastflow.ui.plans

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
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.ui.components.GroupedDivider
import com.oleksandr.fastflow.ui.components.GroupedRow
import com.oleksandr.fastflow.ui.components.GroupedSection
import com.oleksandr.fastflow.ui.components.MiniRing
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/**
 * Plan chooser opened from the chip on Home (SPEC 5.3).
 *
 * Each row carries a small ring showing the fasting-to-eating split, so the
 * protocols are comparable at a glance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPickerSheet(
    plans: List<FastingPlan>,
    activePlanId: String,
    onSelect: (String) -> Unit,
    onCreateCustom: (fastingMinutes: Int, eatingMinutes: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current
    var editingCustom by remember { mutableStateOf(false) }
    val active = plans.firstOrNull { it.id == activePlanId }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.screen_plans_title),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )

            GroupedSection {
                plans.forEachIndexed { index, plan ->
                    if (index > 0) GroupedDivider()
                    PlanPickerRow(
                        plan = plan,
                        selected = plan.id == activePlanId,
                        onClick = { onSelect(plan.id) },
                    )
                }
                GroupedDivider()
                GroupedRow(onClick = { editingCustom = true }) {
                    Text(
                        text = stringResource(R.string.plan_custom),
                        style = AppTypography.bodyLarge,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "›", style = AppTypography.bodyLarge, color = palette.textTertiary)
                }
            }
        }
    }

    if (editingCustom) {
        CustomPlanSheet(
            initialFastingMinutes = active?.fastingMinutes ?: DEFAULT_FASTING_MINUTES,
            initialEatingMinutes = active?.eatingMinutes ?: DEFAULT_EATING_MINUTES,
            onConfirm = { fasting, eating ->
                editingCustom = false
                onCreateCustom(fasting, eating)
            },
            onDismiss = { editingCustom = false },
        )
    }
}

@Composable
private fun PlanPickerRow(plan: FastingPlan, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    GroupedRow(onClick = onClick) {
        MiniRing(
            progress = plan.eatingMinutes?.let {
                plan.fastingMinutes.toFloat() / (plan.fastingMinutes + it)
            } ?: 1f,
            color = palette.fasting,
            size = 24.dp,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = if (plan.isExtended) {
                stringResource(
                    R.string.plan_extended_hours,
                    DurationFormat.hoursLabel(plan.fastingMinutes),
                )
            } else {
                plan.name
            },
            style = AppTypography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(text = "✓", style = AppTypography.bodyLarge, color = palette.fasting)
        }
    }
}

private const val DEFAULT_FASTING_MINUTES = 16 * 60
private const val DEFAULT_EATING_MINUTES = 8 * 60
