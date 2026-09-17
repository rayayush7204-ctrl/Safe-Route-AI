package com.saferouteai.presentation.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.presentation.common.PrimaryButton
import com.saferouteai.presentation.common.SafeRouteBottomNavigation
import com.saferouteai.presentation.common.SafeRouteCard
import com.saferouteai.presentation.common.SecondaryButton
import com.saferouteai.presentation.common.StatusBadge
import com.saferouteai.presentation.home.components.LocationPreflightDialog
import com.saferouteai.presentation.home.components.SafetyCheckInDialog
import com.saferouteai.presentation.navigation.Screen
import com.saferouteai.presentation.permission.rememberAudioPermissionLauncher
import com.saferouteai.presentation.permission.rememberLocationPermissionLauncher
import com.saferouteai.presentation.theme.safeRouteColors
import com.saferouteai.presentation.theme.spacing
import java.util.Calendar

/**
 * Modern Home Screen matching Option 1 & 2 Screen 2 from the SafeRoute reference design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JourneyViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPlanJourney: () -> Unit,
    onNavigateToActiveJourney: () -> Unit,
    onNavigateToConsent: () -> Unit,
    modifier: Modifier = Modifier,
    userName: String = "Ayush"
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

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning,"
        in 12..16 -> "Good afternoon,"
        else -> "Good evening,"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "SafeRoute",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SafeRoute AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
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
        },
        bottomBar = {
            SafeRouteBottomNavigation(
                currentRoute = Screen.Home.route,
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Journey.route -> {
                            if (uiState.isJourneyActive) {
                                onNavigateToActiveJourney()
                            } else {
                                onNavigateToPlanJourney()
                            }
                        }
                        Screen.TrustedContacts.route -> onNavigateToContacts()
                        Screen.Settings.route -> onNavigateToSettings()
                    }
                }
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
            // User Greeting Header (Option 2 Screen 1 / Option 1 Screen 2)
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$userName 👋",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ready for a safer journey?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Journey State Card
            SafeRouteCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    StatusBadge(journeyState = uiState.journeyState)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            uiState.isJourneyActive -> "Safe Journey session is active. Foreground tracking and periodic safety checkpoints armed."
                            uiState.isJourneyCompleted -> "Your Safe Journey session has concluded normally."
                            uiState.isJourneyCancelled -> "Your Safe Journey session was cancelled."
                            else -> "Start a journey to enable local safety monitoring."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.isJourneyActive && uiState.elapsedDurationMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    } else if (uiState.isJourneyActive) {
                        PrimaryButton(
                            text = "View Active Journey",
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = onNavigateToActiveJourney
                        )
                    } else {
                        PrimaryButton(
                            text = "Start Safe Journey",
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = onNavigateToPlanJourney
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trusted Contacts Card Row
            SafeRouteCard(
                onClick = onNavigateToContacts
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Trusted Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Local safety contacts saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Contacts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy First Card Row
            SafeRouteCard(
                onClick = onNavigateToConsent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.safeRouteColors.successContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.safeRouteColors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Privacy First",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "On-device processing • Zero cloud telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Privacy Consent",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
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
