package com.saferouteai.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.presentation.common.SectionCard
import com.saferouteai.presentation.common.StatusBadge
import com.saferouteai.presentation.home.components.CheckpointStatusCard
import com.saferouteai.presentation.home.components.JourneySignalsCard
import com.saferouteai.presentation.home.components.LocationPreflightDialog
import com.saferouteai.presentation.home.components.LocationStatusCard
import com.saferouteai.presentation.home.components.SafetyCheckInDialog
import com.saferouteai.presentation.permission.rememberLocationPermissionLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JourneyViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val requestPermissionLauncher = rememberLocationPermissionLauncher { status ->
        viewModel.onPermissionResult(status)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SafeRoute AI",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "SafeRoute AI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A personal safety journey application designed to accompany you during daily travels with proactive, intelligent protection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Journey State Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusBadge(journeyState = uiState.journeyState)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            uiState.sessionStatus == SessionStatus.CHECKPOINT_DUE ->
                                "Safety Check-In is due! Tap below to confirm you are safe."
                            uiState.isJourneyActive ->
                                "Safe Journey session is active. Foreground tracking and periodic safety checkpoints armed."
                            uiState.isJourneyCompleted ->
                                "Your Safe Journey session has concluded normally."
                            uiState.isJourneyCancelled ->
                                "Your Safe Journey session was cancelled."
                            else ->
                                "Ready to accompany you. Tap below to begin a safe journey."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (uiState.isJourneyActive && uiState.elapsedDurationMs > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val elapsedSeconds = uiState.elapsedDurationMs / 1000L
                        val minutes = elapsedSeconds / 60
                        val seconds = elapsedSeconds % 60
                        Text(
                            text = "Elapsed Time: ${String.format("%02d:%02d", minutes, seconds)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Real-Time Location Card during Active Journey
            if (uiState.isJourneyActive) {
                Spacer(modifier = Modifier.height(16.dp))
                LocationStatusCard(
                    trackingState = uiState.locationTrackingState,
                    location = uiState.currentLocation
                )

                Spacer(modifier = Modifier.height(16.dp))
                // Local Safety Checkpoint Card
                CheckpointStatusCard(
                    checkpointState = uiState.checkpointState,
                    nextCheckpointRemainingMs = uiState.nextCheckpointRemainingMs,
                    onCheckInClicked = { viewModel.onAcknowledgeCheckpoint() }
                )

                // Local Journey Anomaly Observations Card
                if (uiState.activeAnomalies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    JourneySignalsCard(signals = uiState.activeAnomalies)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Buttons
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                when {
                    !uiState.isJourneyActive && !uiState.isJourneyCompleted && !uiState.isJourneyCancelled -> {
                        Button(
                            onClick = { viewModel.onStartJourneyClicked() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Start Safe Journey",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    uiState.isJourneyActive -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.endJourney() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "End Journey",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { viewModel.cancelJourney() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cancel Journey",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    uiState.isJourneyCompleted || uiState.isJourneyCancelled -> {
                        Button(
                            onClick = { viewModel.resetJourney() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Start Another Journey",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Action: Settings Button
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Privacy & Architecture Foundation Notice
            SectionCard(
                title = "Local Safety Engine",
                description = "Strictly foreground-only tracking and local periodic check-ins. Session metadata is stored locally; zero location history or cloud telemetry.",
                icon = Icons.Default.Info,
                trailingTag = "Local Only"
            )
        }
    }

    // Safety Check-In Dialog
    if (uiState.showSafetyCheckInDialog) {
        SafetyCheckInDialog(
            onAcknowledge = { viewModel.onAcknowledgeCheckpoint() },
            onEndJourney = { viewModel.endJourney() },
            onDismiss = { viewModel.dismissSafetyCheckInDialog() }
        )
    }

    // Preflight Dialog for Dual-Gated Permissions
    if (uiState.showPreflightDialog) {
        LocationPreflightDialog(
            isConsentGranted = uiState.isConsentGranted,
            isPermissionGranted = uiState.isPermissionGranted,
            onDismiss = { viewModel.dismissPreflightDialog() },
            onNavigateToSettings = onNavigateToSettings,
            onRequestPermission = { requestPermissionLauncher() }
        )
    }
}
