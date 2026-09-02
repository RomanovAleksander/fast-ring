package com.oleksandr.fastflow.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.model.AppSettings
import com.oleksandr.fastflow.domain.model.FastingPlan
import com.oleksandr.fastflow.domain.model.ThemePalette
import com.oleksandr.fastflow.ui.components.GroupedDivider
import com.oleksandr.fastflow.ui.components.GroupedRow
import com.oleksandr.fastflow.ui.components.GroupedSection
import com.oleksandr.fastflow.ui.components.MiniRing
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.paletteFor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = viewModel.exportCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = viewModel.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (text != null) viewModel.importJson(text)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.screen_settings_title),
                style = AppTypography.displaySmall,
                color = palette.textPrimary,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        }

        item {
            GroupedSection(header = stringResource(R.string.settings_section_plan)) {
                state.plans.forEachIndexed { index, plan ->
                    if (index > 0) GroupedDivider()
                    PlanRow(
                        plan = plan,
                        selected = plan.id == state.settings.activePlanId,
                        onClick = { viewModel.setActivePlan(plan.id) },
                    )
                }
            }
        }

        item {
            GroupedSection(
                header = stringResource(R.string.settings_section_reminders),
                footer = stringResource(R.string.settings_milestones_footer),
            ) {
                ToggleRow(
                    label = stringResource(R.string.settings_auto_start),
                    checked = state.settings.autoStartNextFast,
                    onChange = viewModel::setAutoStart,
                )
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_eating_end_reminder),
                    value = state.settings.eatingEndReminderMinutes
                        ?.let { stringResource(R.string.settings_minutes_before, it) }
                        ?: stringResource(R.string.settings_off),
                    onClick = { viewModel.setEatingEndReminder(nextReminderChoice(state.settings)) },
                )
                GroupedDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_milestones),
                    checked = state.settings.milestonesEnabled,
                    onChange = viewModel::setMilestones,
                )
            }
        }

        item {
            GroupedSection(header = stringResource(R.string.settings_section_appearance)) {
                PaletteRow(
                    palette = ThemePalette.MINT,
                    label = stringResource(R.string.settings_palette_mint),
                    selected = state.settings.palette == ThemePalette.MINT,
                    onClick = { viewModel.setPalette(ThemePalette.MINT) },
                )
                GroupedDivider()
                PaletteRow(
                    palette = ThemePalette.SYSTEM,
                    label = stringResource(R.string.settings_palette_system),
                    selected = state.settings.palette == ThemePalette.SYSTEM,
                    onClick = { viewModel.setPalette(ThemePalette.SYSTEM) },
                )
            }
        }

        item {
            GroupedSection(header = stringResource(R.string.settings_section_data)) {
                ChoiceRow(
                    label = stringResource(R.string.settings_export_csv),
                    value = "",
                    onClick = { exportCsvLauncher.launch("fastflow-history.csv") },
                )
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_export_json),
                    value = "",
                    onClick = { exportJsonLauncher.launch("fastflow-history.json") },
                )
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_import_json),
                    value = "",
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
            }
        }

        item {
            GroupedSection(header = stringResource(R.string.settings_section_permissions)) {
                ChoiceRow(
                    label = stringResource(R.string.settings_perm_notifications),
                    value = stringResource(R.string.action_open_settings),
                    onClick = { openAppSettings(context) },
                )
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_perm_battery),
                    value = stringResource(R.string.action_open_settings),
                    onClick = { openBatterySettings(context) },
                )
            }
        }
    }
}

@Composable
private fun PlanRow(plan: FastingPlan, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    GroupedRow(onClick = onClick) {
        MiniRing(
            // The ring shows the fasting-to-eating split at a glance.
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

@Composable
private fun PaletteRow(
    palette: ThemePalette,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val current = LocalAppPalette.current
    val preview = paletteFor(palette)
    GroupedRow(onClick = onClick) {
        MiniRing(progress = 0.7f, color = preview.fasting, size = 24.dp, strokeWidth = 2.5.dp)
        MiniRing(progress = 0.4f, color = preview.eating, size = 24.dp, strokeWidth = 2.5.dp)
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(text = "✓", style = AppTypography.bodyLarge, color = current.fasting)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalAppPalette.current
    GroupedRow {
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.fasting,
                checkedThumbColor = palette.background,
                uncheckedTrackColor = palette.surfaceVariant,
                uncheckedThumbColor = palette.textSecondary,
            ),
        )
    }
}

@Composable
private fun ChoiceRow(label: String, value: String, onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    GroupedRow(onClick = onClick) {
        Text(
            text = label,
            style = AppTypography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = AppTypography.bodyMedium, color = palette.textSecondary)
        Text(text = "›", style = AppTypography.bodyLarge, color = palette.textTertiary)
    }
}

/** Cycles 15 → 30 → 60 → off, so the row needs no extra picker. */
private fun nextReminderChoice(settings: AppSettings): Int? {
    val choices = AppSettings.EATING_REMINDER_CHOICES
    val current = settings.eatingEndReminderMinutes ?: return choices.first()
    val index = choices.indexOf(current)
    return if (index == -1 || index == choices.lastIndex) null else choices[index + 1]
}

private fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(
                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }
}

private fun openBatterySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure { openAppSettings(context) }
}
