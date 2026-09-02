package com.oleksandr.fastflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oleksandr.fastflow.R

/**
 * Inter is bundled as a single variable font; each weight below is an instance
 * of its `wght` axis, which needs API 26+ — the app's minSdk.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(
    interWeight(200),
    interWeight(300),
    interWeight(400),
    interWeight(500),
    interWeight(600),
    interWeight(700),
)

/** Tabular figures, so a ticking timer never shifts its digits sideways. */
const val TABULAR_FIGURES = "tnum"

/** Typography scale from SPEC 5.1. */
val AppTypography = Typography(
    // Large title — screen headers.
    displaySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp,
    ),
    // Section / screen headings.
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    // Body — list rows, sheet copy.
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // Caption.
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

/** The 72sp hero timer on Home (SPEC 5.1). */
val TimerHeroStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.W200,
    fontSize = 72.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

/** The timer inside the ring: 56sp, with seconds set beside it (SPEC 5.2). */
val TimerRingStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.W200,
    fontSize = 56.sp,
    letterSpacing = (-1).sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

val TimerSecondsStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.W300,
    fontSize = 22.sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

/** Uppercase section caption with wide tracking (SPEC 5.3). */
val OverlineStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 1.2.sp,
)

/** Big number on a stat tile. */
val StatNumberStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    fontFeatureSettings = TABULAR_FIGURES,
)

/** Any number that must not jitter as it counts. */
val NumericStyle = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    fontFeatureSettings = TABULAR_FIGURES,
)
