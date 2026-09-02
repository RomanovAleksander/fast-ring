package com.oleksandr.fastflow.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.domain.logic.DurationFormat
import com.oleksandr.fastflow.domain.logic.FastStateResolver
import com.oleksandr.fastflow.domain.model.FastState
import com.oleksandr.fastflow.ui.theme.paletteFor

/**
 * The 2x2 home-screen widget (SPEC 3.6).
 *
 * Shows minutes only — a widget refreshes at most every fifteen minutes, so
 * seconds would be wrong more often than right.
 */
class FastFlowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetStateLoader.load(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(state: WidgetState) {
        // The widget reads the same palette as the app (SPEC 3.6).
        val palette = paletteFor(state.palette)
        val ringColor: Color = if (state.goalReached) palette.success else palette.fasting

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .cornerRadius(20.dp)
                .padding(8.dp)
                .clickable(actionRunCallback<ToggleFastAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(
                    RingBitmap.draw(
                        sizePx = RING_PX,
                        progress = state.progress,
                        ringColor = ringColor.toArgb(),
                        trackColor = palette.trackOf(ringColor).toArgb(),
                        strokeWidthPx = RING_STROKE_PX,
                    ),
                ),
                contentDescription = null,
                modifier = GlanceModifier.size(RING_DP.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.timer,
                    style = TextStyle(
                        color = ColorProvider(palette.textPrimary),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = state.label,
                    style = TextStyle(
                        color = ColorProvider(palette.textSecondary),
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }

    private companion object {
        const val RING_DP = 96
        const val RING_PX = 288
        const val RING_STROKE_PX = 20f
    }
}

/** Reads the current state for the widget, outside of composition. */
internal object WidgetStateLoader {

    suspend fun load(context: Context): WidgetState {
        val entryPoint = WidgetEntryPoint.from(context)
        val clock = entryPoint.clock()
        val fasts = entryPoint.fastRepository()
        val plans = entryPoint.planRepository()
        val settings = entryPoint.settingsRepository().get()

        val active = fasts.getActive()
        val lastFinished = fasts.getLastFinished()
        val now = clock.nowMillis()

        val state = FastStateResolver.resolve(
            activeFast = active,
            activePlan = active?.let { plans.getById(it.planId) },
            lastFinished = lastFinished,
            lastPlan = lastFinished?.let { plans.getById(it.planId) },
            nowMillis = now,
        )

        return when (state) {
            is FastState.Fasting -> WidgetState(
                label = context.getString(R.string.state_fasting),
                timer = DurationFormat.hhmm(state.fast.elapsed(now).toMillis()),
                progress = state.fast.completionRatio(now),
                running = true,
                goalReached = false,
                palette = settings.palette,
            )

            is FastState.Overtime -> WidgetState(
                label = context.getString(R.string.state_fasting),
                timer = DurationFormat.hhmm(state.fast.elapsed(now).toMillis()),
                progress = 1f,
                running = true,
                goalReached = true,
                palette = settings.palette,
            )

            is FastState.Eating -> WidgetState(
                label = context.getString(R.string.state_eating),
                timer = DurationFormat.hhmm(state.windowEndsAtMillis - now),
                progress = 1f,
                running = false,
                goalReached = true,
                palette = settings.palette,
            )

            FastState.Idle -> WidgetState(
                label = context.getString(R.string.widget_idle),
                timer = "--:--",
                progress = 0f,
                running = false,
                goalReached = false,
                palette = settings.palette,
            )
        }
    }
}
