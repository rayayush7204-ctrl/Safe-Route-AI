package com.saferouteai.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferouteai.presentation.common.DestructiveButton
import com.saferouteai.presentation.common.SafeRouteCard
import com.saferouteai.presentation.common.SafeRouteMapCanvas
import com.saferouteai.presentation.common.SecondaryButton
import com.saferouteai.presentation.home.components.CheckpointStatusCard
import com.saferouteai.presentation.theme.safeRouteColors

/**
 * Active Journey Screen matching Option 1 & 2 Screen 4 from the SafeRoute reference design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveJourneyScreen(
    uiState: JourneyUiState,
    onNavigateBack: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onEndJourney: () -> Unit,
    onCancelJourney: () -> Unit,
    onToggleAudio: () -> Unit,
    onAcknowledgeCheckpoint: () -> Unit,
    modifier: Modifier = Modifier,
    originLabel: String = "Origin",
    destinationLabel: String = "Destination"
) {
    val elapsedSeconds = uiState.elapsedDurationMs / 1000L
    val elapsedMinutes = elapsedSeconds / 60
    val elapsedRemainderSeconds = elapsedSeconds % 60
    val elapsedDisplay = if (elapsedMinutes >= 60) {
        "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
    } else {
        String.format("%02d:%02d", elapsedMinutes, elapsedRemainderSeconds)
    }

    val checkpointDisplay = if (uiState.nextCheckpointRemainingMs != null && uiState.nextCheckpointRemainingMs > 0) {
        val remSec = uiState.nextCheckpointRemainingMs / 1000L
        "${remSec / 60}m ${remSec % 60}s"
    } else {
        "Armed"
    }

    val accuracyDisplay = if (uiState.currentLocation?.accuracyMeters != null) {
        "±${uiState.currentLocation.accuracyMeters.toInt()} m"
    } else {
        "Standby"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Active Journey",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$originLabel → $destinationLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Summary route pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$originLabel → $destinationLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Monitoring Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.safeRouteColors.success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Map Canvas Area
            SafeRouteMapCanvas(
                currentLocation = uiState.currentLocation,
                safetyTier = uiState.riskAssessment.tier,
                destinationName = destinationLabel,
                onStatusClick = onNavigateToStatus
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Journey Metrics Row (3-column layout matching reference)
            SafeRouteCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Elapsed Metric
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = elapsedDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elapsed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    // GPS Accuracy Metric
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = accuracyDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "GPS Accuracy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    // Checkpoint / Remaining Metric
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = checkpointDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Next Checkpoint",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safety Status Tap Card
            SafeRouteCard(
                onClick = onNavigateToStatus
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Safety Status: ${uiState.riskAssessment.tier.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.riskAssessment.score}/100 • Tap to view signals",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "View →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Audio Safety Pipeline Card
            SafeRouteCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.isAudioCapturing) MaterialTheme.safeRouteColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.isAudioCapturing) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (uiState.isAudioCapturing) MaterialTheme.safeRouteColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Audio Pipeline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isAudioCapturing) "Monitoring active locally" else "Idle • Consent verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.clip(RoundedCornerShape(50))
                    ) {
                        IconButton(onClick = onToggleAudio) {
                            Icon(
                                imageVector = if (uiState.isAudioCapturing) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Toggle Audio Capture",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Checkpoint Card if due
            if (uiState.showSafetyCheckInDialog || uiState.nextCheckpointRemainingMs != null) {
                Spacer(modifier = Modifier.height(14.dp))
                CheckpointStatusCard(
                    checkpointState = uiState.checkpointState,
                    nextCheckpointRemainingMs = uiState.nextCheckpointRemainingMs,
                    onCheckInClicked = onAcknowledgeCheckpoint
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Destructive Action: End Journey Button
            DestructiveButton(
                text = "End Journey",
                icon = Icons.Default.StopCircle,
                onClick = onEndJourney
            )

            Spacer(modifier = Modifier.height(10.dp))

            SecondaryButton(
                text = "Cancel Journey",
                onClick = onCancelJourney
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
