package com.oleksandr.fastflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** iOS-flavoured radii from SPEC 5.1. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    // Grouped inset list blocks.
    medium = RoundedCornerShape(12.dp),
    // Bottom sheets.
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Full-width capsule button, 50dp tall. */
val CapsuleShape = RoundedCornerShape(percent = 50)
