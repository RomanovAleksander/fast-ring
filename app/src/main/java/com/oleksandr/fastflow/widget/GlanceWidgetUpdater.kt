package com.oleksandr.fastflow.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.oleksandr.fastflow.domain.repository.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Redraws every placed widget after a state change (SPEC 3.6). */
@Singleton
class GlanceWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetUpdater {

    override suspend fun refresh() {
        // A widget may not be placed at all, which Glance reports as an error.
        runCatching { FastFlowWidget().updateAll(context) }
    }
}
