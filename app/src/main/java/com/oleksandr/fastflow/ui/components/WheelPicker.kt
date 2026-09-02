package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * iOS-style wheel: a snapping list that fades at both ends (SPEC 5.3).
 *
 * Built on LazyColumn with snap fling rather than a slider, so half-hour steps
 * can actually be hit.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // The item resting in the middle is the selection.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> if (index in items.indices) onSelect(index) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val verticalPadding = (maxHeight - ITEM_HEIGHT.dp) / 2

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = if (selected) AppTypography.titleMedium else AppTypography.bodyLarge,
                        color = if (selected) palette.textPrimary else palette.textTertiary,
                    )
                }
            }
        }

        // Fade the ends so the wheel reads as a cylinder. The scrim has no
        // pointer modifier, so touches still reach the list underneath.
        val scrim = remember(palette) {
            Brush.verticalGradient(
                0f to palette.surface,
                0.25f to Color.Transparent,
                0.75f to Color.Transparent,
                1f to palette.surface,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim),
        )
    }
}

private const val ITEM_HEIGHT = 36
