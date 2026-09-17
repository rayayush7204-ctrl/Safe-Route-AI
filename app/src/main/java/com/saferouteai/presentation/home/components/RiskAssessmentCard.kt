package com.saferouteai.presentation.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.risk.RiskAssessment
import com.saferouteai.domain.model.risk.RiskFactor
import com.saferouteai.presentation.common.RiskScoreDisplay
import com.saferouteai.presentation.common.RiskTierIndicator
import com.saferouteai.presentation.common.SafeRouteCard
import com.saferouteai.presentation.common.SafetyTierVisualMapping
import com.saferouteai.presentation.theme.cornerRadius
import com.saferouteai.presentation.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calm, explainable risk assessment summary card displayed during an active Safe Journey.
 *
 * Invariants:
 * - Observational awareness only.
 * - No sirens, no flashing emergency UI, no automated SOS triggers.
 * - Displays factual contributing factors and conservative qualitative safety tiers.
 */
@Composable
fun RiskAssessmentCard(
    riskAssessment: RiskAssessment,
    modifier: Modifier = Modifier
) {
    val visual = SafetyTierVisualMapping.getVisual(riskAssessment.tier)

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val assessedTimeStr = if (riskAssessment.assessedAtEpochMs > 0) {
        timeFormatter.format(Date(riskAssessment.assessedAtEpochMs))
    } else {
        "Active"
    }

    SafeRouteCard(
        modifier = modifier.animateContentSize()
    ) {
        // Header: Title & Safety Tier Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = "Safety Tier: ${visual.label}",
                    tint = visual.color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                Text(
                    text = "Safety Assessment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Accessible Tier Indicator with Text + Icon + Semantic Color
            RiskTierIndicator(tier = riskAssessment.tier)
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // Score & Assessed Timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RiskScoreDisplay(score = riskAssessment.score, scoreColor = visual.color)

            Text(
                text = "Updated: $assessedTimeStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // Conservative, explainable narrative summary
        Text(
            text = visual.narrative,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Contributing Factors List
        if (riskAssessment.factors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Text(
                text = "Contributing observations:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            riskAssessment.factors.forEach { factor ->
                RiskFactorRow(factor = factor, accentColor = visual.color)
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxs))
            }
        }
    }
}

@Composable
private fun RiskFactorRow(
    factor: RiskFactor,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.cornerRadius.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
            Text(
                text = factor.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))

        Text(
            text = "+${factor.contribution}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}
