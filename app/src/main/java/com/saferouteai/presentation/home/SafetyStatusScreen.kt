package com.saferouteai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferouteai.domain.model.risk.RiskAssessment
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.presentation.common.SafeRouteCard
import com.saferouteai.presentation.common.SecondaryButton
import com.saferouteai.presentation.common.StatusShieldBadge
import com.saferouteai.presentation.common.getSafetyTierVisualMapping
import com.saferouteai.presentation.theme.safeRouteColors

/**
 * Safety Status Screen matching Option 1 & 2 Screens 5, 6, 7 from the SafeRoute reference design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyStatusScreen(
    riskAssessment: RiskAssessment,
    onNavigateBack: () -> Unit,
    onViewDetails: () -> Unit,
    onViewAcousticDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = riskAssessment.tier
    val mapping = getSafetyTierVisualMapping(tier)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Safety Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Glowing Status Shield Badge
            StatusShieldBadge(
                tier = tier,
                score = riskAssessment.score,
                updatedAgoText = "Last updated just now"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contributing or Recent Signals Card
            SafeRouteCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (riskAssessment.factors.isEmpty()) "Recent Signals" else "Contributing Signals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (riskAssessment.factors.isEmpty()) {
                        // Baseline Normal signals
                        SignalStatusRow(
                            title = "Route within expected path",
                            subtitle = "Corridor tracking verified",
                            isNormal = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SignalStatusRow(
                            title = "Movement looks normal",
                            subtitle = "Heading and velocity consistent",
                            isNormal = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SignalStatusRow(
                            title = "Duration on track",
                            subtitle = "No prolonged stops detected",
                            isNormal = true
                        )
                    } else {
                        // Real contributing factors from RiskFusionEngine
                        riskAssessment.factors.forEachIndexed { index, factor ->
                            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                            SignalStatusRow(
                                title = factor.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                subtitle = factor.explanation,
                                pointDelta = factor.contribution,
                                isNormal = false,
                                tintColor = mapping.primaryColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // View Details Primary Action Button
            SecondaryButton(
                text = "View Signal Details",
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onViewDetails
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Acoustic Observation shortcut
            SafeRouteCard(
                onClick = onViewAcousticDetails
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Acoustic Observation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Inspect audio waveform & local telemetry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignalStatusRow(
    title: String,
    subtitle: String,
    isNormal: Boolean,
    pointDelta: Int? = null,
    tintColor: Color = MaterialTheme.safeRouteColors.success
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (isNormal) MaterialTheme.safeRouteColors.successContainer else tintColor.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isNormal) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isNormal) MaterialTheme.safeRouteColors.success else tintColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pointDelta != null) {
            Text(
                text = "+$pointDelta",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}
