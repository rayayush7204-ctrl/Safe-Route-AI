package com.saferouteai.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.presentation.theme.cornerRadius
import com.saferouteai.presentation.theme.spacing

/**
 * Reusable badge for SafetyTier (NORMAL, ELEVATED, HIGH)
 * Combines text, icon, and color for accessibility.
 */
@Composable
fun RiskTierIndicator(
    tier: SafetyTier,
    modifier: Modifier = Modifier
) {
    val visual = SafetyTierVisualMapping.getVisual(tier)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.cornerRadius.sm))
            .background(visual.containerColor)
            .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xxs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.label,
                tint = visual.color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xxs))
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = visual.color
            )
        }
    }
}
