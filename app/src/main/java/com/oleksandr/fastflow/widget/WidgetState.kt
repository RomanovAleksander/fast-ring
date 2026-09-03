package com.oleksandr.fastflow.widget

import com.oleksandr.fastflow.domain.model.ThemePalette

/** Which ring the widget is showing, mirroring the app's Home states. */
enum class WidgetPhase { IDLE, FASTING, OVERTIME, EATING }

/** Everything the widgets need, resolved before Glance composes. */
data class WidgetState(
    val phase: WidgetPhase,
    val label: String,
    /** `HH:MM`; minutes only, since a widget refreshes at most every 15 min. */
    val timer: String,
    /** Whole hours, for the 1x1 tile where `HH:MM` will not fit. */
    val hours: String,
    val progress: Float,
    val palette: ThemePalette,
) {
    val running: Boolean get() = phase == WidgetPhase.FASTING || phase == WidgetPhase.OVERTIME
}
