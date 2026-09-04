package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.ui.theme.AppShapes
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.OverlineStyle

/**
 * An iOS-style inset grouped section: an uppercase header, a rounded block of
 * rows, and an optional footnote (SPEC 5.1).
 */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalAppPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                style = OverlineStyle,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(palette.surface),
            content = content,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = AppTypography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
    }
}

/** One row inside a [GroupedSection]. */
@Composable
fun GroupedRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val clickable = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Hairline between rows, inset to match iOS. */
@Composable
fun GroupedDivider(startInset: Boolean = true) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (startInset) 16.dp else 0.dp)
            .height(0.5.dp)
            .background(palette.divider),
    )
}
