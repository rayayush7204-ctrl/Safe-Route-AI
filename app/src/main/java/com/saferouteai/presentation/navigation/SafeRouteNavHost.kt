package com.saferouteai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saferouteai.presentation.consent.ConsentViewModel
import com.saferouteai.presentation.consent.PrivacyConsentScreen
import com.saferouteai.presentation.contacts.TrustedContactsScreen
import com.saferouteai.presentation.contacts.TrustedContactsViewModel
import com.saferouteai.presentation.home.HomeScreen
import com.saferouteai.presentation.home.JourneyViewModel
import com.saferouteai.presentation.profile.ProfileScreen
import com.saferouteai.presentation.profile.ProfileViewModel
import com.saferouteai.presentation.settings.SettingsScreen

@Composable
fun SafeRouteNavHost(
    journeyViewModel: JourneyViewModel,
    profileViewModel: ProfileViewModel,
    trustedContactsViewModel: TrustedContactsViewModel,
    consentViewModel: ConsentViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = journeyViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
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
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TrustedContacts.route) {
            TrustedContactsScreen(
                viewModel = trustedContactsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

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
