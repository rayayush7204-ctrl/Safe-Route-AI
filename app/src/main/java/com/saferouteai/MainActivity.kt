package com.saferouteai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.saferouteai.data.local.SafeRouteDatabase
import com.saferouteai.data.local.datastore.ConsentDataStore
import com.saferouteai.data.location.AndroidLocationPermissionChecker
import com.saferouteai.data.location.FusedLocationDataSource
import com.saferouteai.data.repository.DataStoreConsentRepository
import com.saferouteai.data.repository.FusedLocationRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.RoomTrustedContactsRepository
import com.saferouteai.data.repository.RoomUserProfileRepository
import com.saferouteai.presentation.consent.ConsentViewModel
import com.saferouteai.presentation.contacts.TrustedContactsViewModel
import com.saferouteai.presentation.home.JourneyViewModel
import com.saferouteai.presentation.navigation.SafeRouteNavHost
import com.saferouteai.presentation.profile.ProfileViewModel
import com.saferouteai.presentation.theme.SafeRouteTheme

/**
 * Main application entry point for SafeRoute AI.
 *
 * Keeps business logic entirely delegated to the presentation and domain layers.
 * Forwards Activity lifecycle events to ensure location tracking is strictly foreground-only.
 */
class MainActivity : ComponentActivity() {

    // Journey repository (In-memory for current milestones)
    private val journeyRepository by lazy { InMemoryJourneyRepository() }

    // Room Database and repositories
    private val database by lazy { SafeRouteDatabase.getInstance(applicationContext) }
    private val userProfileRepository by lazy { RoomUserProfileRepository(database.userProfileDao()) }
    private val trustedContactsRepository by lazy { RoomTrustedContactsRepository(database.trustedContactDao()) }

    // DataStore Preferences repository for Consent
    private val consentDataStore by lazy { ConsentDataStore(applicationContext) }
    private val consentRepository by lazy { DataStoreConsentRepository(consentDataStore) }

    // Location components (Play Services Fused Location + Permission Checker)
    private val permissionChecker by lazy { AndroidLocationPermissionChecker(applicationContext) }
    private val locationDataSource by lazy { FusedLocationDataSource(applicationContext) }
    private val locationRepository by lazy {
        FusedLocationRepository(locationDataSource, permissionChecker)
    }

    // Session Engine & Clock
    private val clock by lazy { com.saferouteai.data.time.SystemClock() }
    private val journeySessionRepository by lazy {
        com.saferouteai.data.repository.RoomJourneySessionRepository(
            dao = database.journeySessionDao(),
            locationRepository = locationRepository,
            clock = clock
        )
    }

    // Anomaly Detection Engine
    private val anomalyRepository by lazy { com.saferouteai.data.repository.InMemoryAnomalyRepository() }
    private val anomalyDetector by lazy { com.saferouteai.domain.anomaly.JourneyAnomalyDetector() }

    // ViewModels
    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.provideFactory(
            journeyRepository = journeyRepository,
            locationRepository = locationRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker,
            journeySessionRepository = journeySessionRepository,
            clock = clock,
            anomalyRepository = anomalyRepository,
            anomalyDetector = anomalyDetector
        )
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModel.provideFactory(userProfileRepository)
    }

    private val trustedContactsViewModel: TrustedContactsViewModel by viewModels {
        TrustedContactsViewModel.provideFactory(trustedContactsRepository)
    }

    private val consentViewModel: ConsentViewModel by viewModels {
        ConsentViewModel.provideFactory(consentRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeRouteTheme {
                SafeRouteNavHost(
                    journeyViewModel = journeyViewModel,
                    profileViewModel = profileViewModel,
                    trustedContactsViewModel = trustedContactsViewModel,
                    consentViewModel = consentViewModel
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionChecker.refreshStatus()
        journeyViewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        // Strict foreground-only invariant: pause location tracking when activity leaves foreground
        journeyViewModel.onAppBackgrounded()
    }
}
