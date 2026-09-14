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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.risk.RiskAssessment
import com.saferouteai.domain.model.risk.RiskFactor
import com.saferouteai.domain.model.risk.SafetyTier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calm, explainable risk assessment summary card displayed during an active Safe Journey.
 *
 * Invariants:
 * - Observational awareness only.
 * - No sirens, no flashing emergency UI, no automated SOS triggers.
 * - Displays factual contributing factors and qualitative safety tiers.
 */
@Composable
fun RiskAssessmentCard(
    riskAssessment: RiskAssessment,
    modifier: Modifier = Modifier
) {
    val tierColor = when (riskAssessment.tier) {
        SafetyTier.NORMAL -> MaterialTheme.colorScheme.primary
        SafetyTier.ELEVATED -> Color(0xFFE65100) // Calm amber
        SafetyTier.HIGH -> Color(0xFFC62828) // Deep terra cotta / calm red
    }

    val tierContainerColor = when (riskAssessment.tier) {
        SafetyTier.NORMAL -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        SafetyTier.ELEVATED -> Color(0xFFFFF3E0)
        SafetyTier.HIGH -> Color(0xFFFFEBEE)
    }

    val tierIcon: ImageVector = when (riskAssessment.tier) {
        SafetyTier.NORMAL -> Icons.Outlined.CheckCircle
        SafetyTier.ELEVATED -> Icons.Outlined.Info
        SafetyTier.HIGH -> Icons.Outlined.Warning
    }

    val tierLabel = when (riskAssessment.tier) {
        SafetyTier.NORMAL -> "Normal Safety Tier"
        SafetyTier.ELEVATED -> "Elevated Observations"
        SafetyTier.HIGH -> "High Multi-Signal Level"
    }

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val assessedTimeStr = if (riskAssessment.assessedAtEpochMs > 0) {
        timeFormatter.format(Date(riskAssessment.assessedAtEpochMs))
    } else {
        "Active"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & Safety Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = tierIcon,
                        contentDescription = "Safety Tier: $tierLabel",
                        tint = tierColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tier Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tierContainerColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = riskAssessment.tier.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score & Assessed Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fused risk score: ${riskAssessment.score} / 100",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Updated: $assessedTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Narrative summary
            val summaryText = when (riskAssessment.tier) {
                SafetyTier.NORMAL -> "Baseline safety observed. No anomalous conditions detected."
                SafetyTier.ELEVATED -> "Elevated safety signals detected. Monitoring situation locally."
                SafetyTier.HIGH -> "Multiple safety signals were detected across sensors."
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contributing Factors List
            if (riskAssessment.factors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Contributing observations:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                riskAssessment.factors.forEach { factor ->
                    RiskFactorRow(factor = factor, accentColor = tierColor)
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = factor.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "+${factor.contribution}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}
