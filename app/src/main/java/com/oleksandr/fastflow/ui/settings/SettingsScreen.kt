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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
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
import com.oleksandr.fastflow.ui.plans.CustomPlanSheet
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
    var showCustomPlan by remember { mutableStateOf(false) }

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
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.plan_custom),
                    value = "",
                    onClick = { showCustomPlan = true },
                )
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
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_daily_reminder),
                    value = state.settings.dailyReminderMinuteOfDay
                        ?.let { formatMinuteOfDay(it) }
                        ?: stringResource(R.string.settings_off),
                    onClick = {
                        viewModel.setDailyReminder(nextDailyReminderChoice(state.settings))
                    },
                )
                GroupedDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_milestones),
                    checked = state.settings.milestonesEnabled,
                    onChange = viewModel::setMilestones,
                )
                GroupedDivider()
                ChoiceRow(
                    label = stringResource(R.string.settings_time_format),
                    value = when (state.settings.use24HourClock) {
                        null -> stringResource(R.string.settings_time_format_system)
                        true -> stringResource(R.string.settings_time_format_24)
                        false -> stringResource(R.string.settings_time_format_12)
                    },
                    onClick = {
                        viewModel.setUse24HourClock(nextClockChoice(state.settings.use24HourClock))
                    },
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

    if (showCustomPlan) {
        val active = state.activePlan
        CustomPlanSheet(
            initialFastingMinutes = active?.fastingMinutes ?: 16 * 60,
            initialEatingMinutes = active?.eatingMinutes ?: 8 * 60,
            onConfirm = { fasting, eating ->
                showCustomPlan = false
                scope.launch { viewModel.createCustomPlan(fasting, eating) }
            },
            onDismiss = { showCustomPlan = false },
        )
    }
}

/** Cycles 20:00 → 21:00 → 22:00 → off, keeping the row a single tap. */
private fun nextDailyReminderChoice(settings: AppSettings): Int? {
    val choices = listOf(20 * 60, 21 * 60, 22 * 60)
    val current = settings.dailyReminderMinuteOfDay ?: return choices.first()
    val index = choices.indexOf(current)
    return if (index == -1 || index == choices.lastIndex) null else choices[index + 1]
}

/** System → 24h → 12h → system. */
private fun nextClockChoice(current: Boolean?): Boolean? = when (current) {
    null -> true
    true -> false
    false -> null
}

private fun formatMinuteOfDay(minuteOfDay: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

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
