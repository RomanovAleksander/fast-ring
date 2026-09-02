package com.oleksandr.fastflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.CapsuleShape
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion

/** Filled or tinted, per SPEC 5.1. */
enum class CapsuleStyle { FILLED, TINTED }

/**
 * The full-width 50dp capsule button.
 *
 * No ripple and no elevation: presses read as a 0.97 scale, the way iOS
 * controls behave (SPEC 5.4).
 */
@Composable
fun CapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CapsuleStyle = CapsuleStyle.FILLED,
    accent: Color? = null,
    enabled: Boolean = true,
) {
    val palette = LocalAppPalette.current
    val tint = accent ?: palette.fasting

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "buttonScale",
    )

    val background by animateColorAsState(
        targetValue = when (style) {
            CapsuleStyle.FILLED -> tint
            CapsuleStyle.TINTED -> tint.copy(alpha = 0.15f)
        },
        animationSpec = tween(Motion.BUTTON_COLOR_MILLIS),
        label = "buttonBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = when (style) {
            CapsuleStyle.FILLED -> palette.background
            CapsuleStyle.TINTED -> tint
        },
        animationSpec = tween(Motion.BUTTON_COLOR_MILLIS),
        label = "buttonContent",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .scale(scale)
            .background(color = background, shape = CapsuleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppTypography.titleMedium, color = contentColor)
    }
}
