package com.saferouteai.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.presentation.theme.spacing

/**
 * Large glowing safety shield header component matching screens 5, 6, 7 in the reference.
 */
@Composable
fun StatusShieldBadge(
    tier: SafetyTier,
    score: Int,
    modifier: Modifier = Modifier,
    updatedAgoText: String = "Last updated just now"
) {
    val mapping = getSafetyTierVisualMapping(tier)

    val auraColors = when (tier) {
        SafetyTier.NORMAL -> listOf(
            mapping.primaryColor.copy(alpha = 0.35f),
            mapping.primaryColor.copy(alpha = 0.10f),
            Color.Transparent
        )
        SafetyTier.ELEVATED -> listOf(
            mapping.primaryColor.copy(alpha = 0.40f),
            mapping.primaryColor.copy(alpha = 0.12f),
            Color.Transparent
        )
        SafetyTier.HIGH -> listOf(
            mapping.primaryColor.copy(alpha = 0.45f),
            mapping.primaryColor.copy(alpha = 0.15f),
            Color.Transparent
        )
    }

    val iconVector = when (tier) {
        SafetyTier.NORMAL -> Icons.Default.Check
        SafetyTier.ELEVATED -> Icons.Default.PriorityHigh
        SafetyTier.HIGH -> Icons.Default.Warning
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing circular shield container
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(Brush.radialGradient(auraColors), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(mapping.containerColor)
                    .border(2.dp, mapping.primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = mapping.label,
                    tint = mapping.primaryColor,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tier Title (e.g. NORMAL, ELEVATED, HIGH)
        Text(
            text = mapping.label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = mapping.primaryColor,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Score display (e.g. 12 / 100)
        Text(
            text = "$score / 100",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Calm narrative explanation
        Text(
            text = mapping.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = updatedAgoText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
