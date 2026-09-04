package com.oleksandr.fastflow.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.WheelPicker
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/**
 * Editor for a custom protocol (SPEC 3.1).
 *
 * Hours are chosen on wheels rather than sliders, as the spec asks: a slider
 * cannot hit "16:30" reliably, and half-hour steps are the point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanSheet(
    initialFastingMinutes: Int,
    initialEatingMinutes: Int?,
    onConfirm: (fastingMinutes: Int, eatingMinutes: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalAppPalette.current

    val fastingSteps = remember {
        (FastingPlan.MIN_FASTING_MINUTES..FastingPlan.MAX_FASTING_MINUTES step
            FastingPlan.CUSTOM_STEP_MINUTES).toList()
    }
    val eatingSteps = remember {
        listOf(0) + (FastingPlan.CUSTOM_STEP_MINUTES..FastingPlan.MAX_EATING_MINUTES step
            FastingPlan.CUSTOM_STEP_MINUTES).toList()
    }

    var fastingIndex by remember {
        mutableIntStateOf(
            fastingSteps.indexOfFirst { it >= initialFastingMinutes }.coerceAtLeast(0),
        )
    }
    var eatingIndex by remember {
        mutableIntStateOf(
            eatingSteps.indexOfFirst { it >= (initialEatingMinutes ?: 0) }.coerceAtLeast(0),
        )
    }

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
                text = stringResource(R.string.plan_custom_editor_title),
                style = AppTypography.headlineSmall,
                color = palette.textPrimary,
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.plan_fasting_hours),
                    style = AppTypography.bodySmall,
                    color = palette.textSecondary,
                )
                WheelPicker(
                    items = fastingSteps.map { DurationFormat.hoursLabel(it) },
                    selectedIndex = fastingIndex,
                    onSelect = { fastingIndex = it },
                    modifier = Modifier.height(140.dp),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.plan_eating_hours),
                    style = AppTypography.bodySmall,
                    color = palette.textSecondary,
                )
                WheelPicker(
                    items = eatingSteps.map { minutes ->
                        if (minutes == 0) {
                            stringResource(R.string.plan_no_eating_window)
                        } else {
                            DurationFormat.hoursLabel(minutes)
                        }
                    },
                    selectedIndex = eatingIndex,
                    onSelect = { eatingIndex = it },
                    modifier = Modifier.height(140.dp),
                )
            }

            CapsuleButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    val eating = eatingSteps[eatingIndex].takeIf { it > 0 }
                    onConfirm(fastingSteps[fastingIndex], eating)
                },
                style = CapsuleStyle.FILLED,
            )
            CapsuleButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = CapsuleStyle.TINTED,
            )
        }
    }
}
