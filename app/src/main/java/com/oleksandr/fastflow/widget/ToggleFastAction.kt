package com.oleksandr.fastflow.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Start or stop a fast straight from the home screen, without opening the app
 * (SPEC 3.6).
 */
class ToggleFastAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val entryPoint = WidgetEntryPoint.from(context)
        if (entryPoint.fastRepository().getActive() != null) {
            entryPoint.endFastUseCase()()
        } else {
            entryPoint.startFastUseCase()()
        }
        // The use cases already refresh the widget, but updating here as well
        // keeps the tapped instance in step even if that path changes.
        FastFlowWidget().update(context, glanceId)
    }
}
