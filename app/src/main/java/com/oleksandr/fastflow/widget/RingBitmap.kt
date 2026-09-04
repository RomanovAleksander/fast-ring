package com.oleksandr.fastflow.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.ColorInt

/**
 * Draws the progress ring into a bitmap.
 *
 * Glance has no Canvas composable (SPEC 8), so the widget's ring is rendered
 * here and handed over as an image.
 */
object RingBitmap {

    /**
     * @param sizePx bitmap edge in pixels.
     * @param progress 0f..1f; values above 1 are clamped, since overtime shows
     *   a full ring rather than a second lap.
     */
    fun draw(
        sizePx: Int,
        progress: Float,
        @ColorInt ringColor: Int,
        @ColorInt trackColor: Int,
        strokeWidthPx: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val inset = strokeWidthPx / 2f
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
        }

        paint.color = trackColor
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - inset, paint)

        val sweep = progress.coerceIn(0f, 1f) * 360f
        if (sweep > 0f) {
            paint.color = ringColor
            // Starts at twelve o'clock, like the ring on the main screen.
            canvas.drawArc(bounds, -90f, sweep, false, paint)
        }
        return bitmap
    }
}
