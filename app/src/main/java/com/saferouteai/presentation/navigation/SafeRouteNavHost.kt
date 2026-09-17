package com.saferouteai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saferouteai.presentation.consent.ConsentViewModel
import com.saferouteai.presentation.consent.PrivacyConsentScreen
import com.saferouteai.presentation.contacts.TrustedContactsScreen
import com.saferouteai.presentation.contacts.TrustedContactsViewModel
import com.saferouteai.presentation.home.AcousticObservationScreen
import com.saferouteai.presentation.home.ActiveJourneyScreen
import com.saferouteai.presentation.home.HomeScreen
import com.saferouteai.presentation.home.JourneyCompletedScreen
import com.saferouteai.presentation.home.JourneyViewModel
import com.saferouteai.presentation.home.PlanJourneyScreen
import com.saferouteai.presentation.home.SafetyStatusScreen
import com.saferouteai.presentation.home.SignalDetailsScreen
import com.saferouteai.presentation.profile.ProfileScreen
import com.saferouteai.presentation.profile.ProfileViewModel
import com.saferouteai.presentation.settings.SettingsScreen
import com.saferouteai.presentation.settings.SettingsViewModel
import com.saferouteai.presentation.splash.SplashScreen

/**
 * Navigation host managing all reference screens for SafeRoute AI Milestone 7B.
 * Strictly adheres to Constraint 4:
 * "Bottom navigation remains: Home | Journey | Contacts | Settings. Other screens such as:
 * Plan Journey, Active Journey, Safety Status, Signal Details, Acoustic Observation,
 * Journey Completed, Privacy & Consent, Profile are normal navigation destinations
 * opened from the relevant screens, not additional bottom tabs."
 */
@Composable
fun SafeRouteNavHost(
    journeyViewModel: JourneyViewModel,
    profileViewModel: ProfileViewModel,
    trustedContactsViewModel: TrustedContactsViewModel,
    consentViewModel: ConsentViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val journeyUiState by journeyViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val userName = if (profileUiState.displayName.isNotBlank()) profileUiState.displayName else "Ayush"
    var originLocation by remember { mutableStateOf("Current Location") }
    var destinationLocation by remember { mutableStateOf("Selected Destination") }

    fun navigateBottomTab(route: String) {
        when (route) {
            Screen.Home.route -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            Screen.Journey.route -> {
                if (journeyUiState.isJourneyActive) {
                    navController.navigate(Screen.ActiveJourney.route) {
                        launchSingleTop = true
                    }
                } else if (journeyUiState.isJourneyCompleted) {
                    navController.navigate(Screen.JourneyCompleted.route) {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Screen.PlanJourney.route) {
                        launchSingleTop = true
                    }
                }
            }
            Screen.TrustedContacts.route -> {
                navController.navigate(Screen.TrustedContacts.route) {
                    launchSingleTop = true
                }
            }
            Screen.Settings.route -> {
                navController.navigate(Screen.Settings.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // 1. Splash & Onboarding Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onGetStarted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = journeyViewModel,
                userName = userName,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.TrustedContacts.route)
                },
                onNavigateToPlanJourney = {
                    navController.navigate(Screen.PlanJourney.route)
                },
                onNavigateToActiveJourney = {
                    navController.navigate(Screen.ActiveJourney.route)
                },
                onNavigateToConsent = {
                    navController.navigate(Screen.PrivacyConsent.route)
                }
            )
        }

        // 3. Plan Your Journey Screen
        composable(Screen.PlanJourney.route) {
            PlanJourneyScreen(
                initialOrigin = originLocation,
                initialDestination = if (destinationLocation != "Selected Destination") destinationLocation else "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartJourney = { origin, dest ->
                    originLocation = origin
                    destinationLocation = dest
                    journeyViewModel.onStartJourneyClicked()
                    navController.navigate(Screen.ActiveJourney.route) {
                        popUpTo(Screen.PlanJourney.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Active Journey Screen
        composable(Screen.ActiveJourney.route) {
            ActiveJourneyScreen(
                uiState = journeyUiState,
                originLabel = originLocation,
                destinationLabel = destinationLocation,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStatus = {
                    navController.navigate(Screen.SafetyStatus.route)
                },
                onEndJourney = {
                    journeyViewModel.endJourney()
                    navController.navigate(Screen.JourneyCompleted.route) {
                        popUpTo(Screen.ActiveJourney.route) { inclusive = true }
                    }
                },
                onCancelJourney = {
                    journeyViewModel.cancelJourney()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onToggleAudio = {
                    if (journeyUiState.isAudioCapturing) {
                        journeyViewModel.stopAudioCapture()
                    } else {
                        journeyViewModel.startAudioCapture()
                    }
                },
                onAcknowledgeCheckpoint = {
                    journeyViewModel.onAcknowledgeCheckpoint()
                }
            )
        }

        // 5, 6, 7. Safety Status Screen (NORMAL / ELEVATED / HIGH)
        composable(Screen.SafetyStatus.route) {
            SafetyStatusScreen(
                riskAssessment = journeyUiState.riskAssessment,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onViewDetails = {
                    navController.navigate(Screen.SignalDetails.route)
                },
                onViewAcousticDetails = {
                    navController.navigate(Screen.AcousticObservation.route)
                }
            )
        }

        // 8. Signal Details Screen
        composable(Screen.SignalDetails.route) {
            SignalDetailsScreen(
                currentLocation = journeyUiState.currentLocation,
                anomaly = journeyUiState.activeAnomalies.firstOrNull(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 9. Acoustic Observation Screen
        composable(Screen.AcousticObservation.route) {
            AcousticObservationScreen(
                signal = journeyUiState.acousticSignals.firstOrNull(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 10. Journey Completed Screen
        composable(Screen.JourneyCompleted.route) {
            JourneyCompletedScreen(
                uiState = journeyUiState,
                userName = userName,
                origin = originLocation,
                destination = destinationLocation,
                onViewSummary = {
                    navController.navigate(Screen.SafetyStatus.route)
                },
                onDone = {
                    journeyViewModel.resetJourney()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        // 11. Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToTrustedContacts = {
                    navController.navigate(Screen.TrustedContacts.route)
                },
                onNavigateToPrivacyConsent = {
                    navController.navigate(Screen.PrivacyConsent.route)
                },
                onNavigateToRoute = { route ->
                    navigateBottomTab(route)
                }
            )
        }

        // 12. Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 13. Trusted Contacts Screen
        composable(Screen.TrustedContacts.route) {
            TrustedContactsScreen(
                viewModel = trustedContactsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRoute = { route ->
                    navigateBottomTab(route)
                }
            )
        }

        // 14. Privacy Consent Screen
        composable(Screen.PrivacyConsent.route) {
            PrivacyConsentScreen(
                viewModel = consentViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
