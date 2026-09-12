package com.saferouteai.presentation.navigation

/**
 * Navigation routes for the SafeRoute application.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
    data object TrustedContacts : Screen("trusted_contacts")
    data object PrivacyConsent : Screen("privacy_consent")
}
