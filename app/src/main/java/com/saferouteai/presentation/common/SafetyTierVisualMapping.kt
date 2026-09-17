package com.saferouteai.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.presentation.theme.safeRouteColors

/**
 * Visual presentation model for a SafetyTier.
 *
 * Enforces conservative, explainable safety language without unsupported claims.
 */
data class SafetyTierVisual(
    val tier: SafetyTier,
    val label: String,
    val narrative: String,
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color
) {
    val primaryColor: Color get() = color
    val contentColor: Color get() = color
    val description: String get() = narrative
}

@Composable
@ReadOnlyComposable
fun getSafetyTierVisualMapping(tier: SafetyTier): SafetyTierVisual = SafetyTierVisualMapping.getVisual(tier)

object SafetyTierVisualMapping {

    fun getLabel(tier: SafetyTier): String = when (tier) {
        SafetyTier.NORMAL -> "NORMAL"
        SafetyTier.ELEVATED -> "ELEVATED"
        SafetyTier.HIGH -> "HIGH"
    }

    fun getNarrative(tier: SafetyTier): String = when (tier) {
        SafetyTier.NORMAL -> "No unusual signals detected."
        SafetyTier.ELEVATED -> "A few unusual patterns were detected."
        SafetyTier.HIGH -> "Multiple unusual signals were detected."
    }

    fun getIcon(tier: SafetyTier): ImageVector = when (tier) {
        SafetyTier.NORMAL -> Icons.Outlined.CheckCircle
        SafetyTier.ELEVATED -> Icons.Outlined.Info
        SafetyTier.HIGH -> Icons.Outlined.Warning
    }

    @Composable
    @ReadOnlyComposable
    fun getVisual(tier: SafetyTier): SafetyTierVisual {
        val safeRouteColors = MaterialTheme.safeRouteColors
        val (color, containerColor) = when (tier) {
            SafetyTier.NORMAL -> safeRouteColors.success to safeRouteColors.successContainer.copy(alpha = 0.35f)
            SafetyTier.ELEVATED -> safeRouteColors.warning to safeRouteColors.warningContainer.copy(alpha = 0.35f)
            SafetyTier.HIGH -> safeRouteColors.error to safeRouteColors.errorContainer.copy(alpha = 0.35f)
        }
        return SafetyTierVisual(
            tier = tier,
            label = getLabel(tier),
            narrative = getNarrative(tier),
            icon = getIcon(tier),
            color = color,
            containerColor = containerColor
        )
    }
}
