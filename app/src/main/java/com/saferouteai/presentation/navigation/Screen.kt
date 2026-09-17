package com.saferouteai.presentation.navigation

/**
 * Navigation routes for the SafeRoute application, matching the M7B reference flow.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Journey : Screen("journey")
    data object PlanJourney : Screen("plan_journey")
    data object ActiveJourney : Screen("active_journey")
    data object SafetyStatus : Screen("safety_status")
    data object SignalDetails : Screen("signal_details")
    data object AcousticObservation : Screen("acoustic_observation")
    data object JourneyCompleted : Screen("journey_completed")
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
    data object TrustedContacts : Screen("trusted_contacts")
    data object PrivacyConsent : Screen("privacy_consent")
}
