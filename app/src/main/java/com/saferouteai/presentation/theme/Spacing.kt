package com.saferouteai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard spacing scale for SafeRoute AI.
 */
data class Spacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp
)

/**
 * Standard corner radii scale.
 */
data class CornerRadius(
    val xs: Dp = 6.dp,
    val sm: Dp = 10.dp,
    val md: Dp = 14.dp,
    val lg: Dp = 18.dp,
    val xl: Dp = 24.dp,
    val full: Dp = 999.dp
)

/**
 * Standard card elevation scale.
 */
data class Elevation(
    val none: Dp = 0.dp,
    val low: Dp = 1.dp,
    val medium: Dp = 2.dp,
    val high: Dp = 4.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalCornerRadius = staticCompositionLocalOf { CornerRadius() }
val LocalElevation = staticCompositionLocalOf { Elevation() }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

val MaterialTheme.cornerRadius: CornerRadius
    @Composable
    @ReadOnlyComposable
    get() = LocalCornerRadius.current

val MaterialTheme.elevation: Elevation
    @Composable
    @ReadOnlyComposable
    get() = LocalElevation.current
