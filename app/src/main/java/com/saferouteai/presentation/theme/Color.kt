package com.saferouteai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SafeRoute AI Semantic Color Tokens (for success, warning, error, info, elevatedSurface)
data class SafeRouteColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val elevatedSurface: Color
)

val DarkSafeRouteColors = SafeRouteColors(
    success = Color(0xFF10B981),
    onSuccess = Color(0xFF022C22),
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFA7F3D0),
    warning = Color(0xFFF59E0B),
    onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFDE68A),
    error = Color(0xFFEF4444),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    info = Color(0xFF38BDF8),
    onInfo = Color(0xFF082F49),
    infoContainer = Color(0xFF0369A1),
    onInfoContainer = Color(0xFFBAE6FD),
    elevatedSurface = Color(0xFF1A2234)
)

val LightSafeRouteColors = SafeRouteColors(
    success = Color(0xFF059669),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD1FAE5),
    onSuccessContainer = Color(0xFF064E3B),
    warning = Color(0xFFD97706),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    info = Color(0xFF0284C7),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFE0F2FE),
    onInfoContainer = Color(0xFF0369A1),
    elevatedSurface = Color(0xFFFFFFFF)
)

val LocalSafeRouteColors = staticCompositionLocalOf { DarkSafeRouteColors }

val androidx.compose.material3.MaterialTheme.safeRouteColors: SafeRouteColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSafeRouteColors.current

// Material 3 Dark Color Palette (Deep navy, electric blue accent, calm dark surfaces)
val DarkBackground = Color(0xFF090D16)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkSurface = Color(0xFF111726)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkSurfaceVariant = Color(0xFF172033)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkPrimary = Color(0xFF3875F6) // Electric vibrant blue
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF1E3A8A)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)
val DarkSecondary = Color(0xFF0EA5E9) // Sky cyan accent
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkSecondaryContainer = Color(0xFF0369A1)
val DarkOnSecondaryContainer = Color(0xFFE0F2FE)
val DarkTertiary = Color(0xFF818CF8) // Indigo accent
val DarkOnTertiary = Color(0xFF0F172A)
val DarkOutline = Color(0xFF1E2C4A)
val DarkOutlineVariant = Color(0xFF152037)

// Material 3 Light Color Palette (Clean soft slate, high contrast, matching electric blue)
val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightPrimary = Color(0xFF2563EB)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E3A8A)
val LightSecondary = Color(0xFF0284C7)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE0F2FE)
val LightOnSecondaryContainer = Color(0xFF075985)
val LightTertiary = Color(0xFF6366F1)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightOutline = Color(0xFFE2E8F0)
val LightOutlineVariant = Color(0xFFCBD5E1)

// Aesthetic Gradients & Accents for SafeRoute Reference Visuals
val PrimaryButtonGradient = listOf(Color(0xFF4F46E5), Color(0xFF2563EB))
val PrimaryButtonLightGradient = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
val DestructiveButtonGradient = listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
val WaveformCyanPurple = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))

// Backward compatibility constants for existing code
val StatusIdle = Color(0xFF64748B)
val StatusActive = Color(0xFF10B981)
val StatusCompleted = Color(0xFF3875F6)
val ErrorRed = Color(0xFFEF4444)
