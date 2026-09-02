package com.oleksandr.fastflow.widget

import com.oleksandr.fastflow.domain.model.ThemePalette

/** Everything the widget needs, resolved before Glance composes. */
data class WidgetState(
    val label: String,
    val timer: String,
    val progress: Float,
    val running: Boolean,
    val goalReached: Boolean,
    val palette: ThemePalette,
)
