package com.saferouteai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saferouteai.data.audio.AndroidAudioPermissionChecker
import com.saferouteai.data.audio.AndroidAudioRecordDataSource
import com.saferouteai.data.local.SafeRouteDatabase
import com.saferouteai.data.local.datastore.ConsentDataStore
import com.saferouteai.data.local.datastore.ThemePreferencesDataStore
import com.saferouteai.data.location.AndroidLocationPermissionChecker
import com.saferouteai.data.location.FusedLocationDataSource
import com.saferouteai.data.repository.AndroidAudioRepository
import com.saferouteai.data.repository.DataStoreConsentRepository
import com.saferouteai.data.repository.DataStoreThemePreferencesRepository
import com.saferouteai.data.repository.FusedLocationRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.RoomTrustedContactsRepository
import com.saferouteai.data.repository.RoomUserProfileRepository
import com.saferouteai.domain.audio.DeterministicAcousticSignalDetector
import com.saferouteai.domain.model.audio.AudioDetectionPolicy
import com.saferouteai.domain.model.theme.ThemeMode
import com.saferouteai.presentation.consent.ConsentViewModel
import com.saferouteai.presentation.contacts.TrustedContactsViewModel
import com.saferouteai.presentation.home.JourneyViewModel
import com.saferouteai.presentation.navigation.SafeRouteNavHost
import com.saferouteai.presentation.profile.ProfileViewModel
import com.saferouteai.presentation.settings.SettingsViewModel
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

    // DataStore Preferences repository for Theme / Appearance
    private val themePreferencesDataStore by lazy { ThemePreferencesDataStore(applicationContext) }
    private val themePreferencesRepository by lazy { DataStoreThemePreferencesRepository(themePreferencesDataStore) }

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

    // Milestone 5A & 5B: Audio Capture & Acoustic Signal Detection Engine
    private val audioPermissionChecker by lazy { AndroidAudioPermissionChecker(applicationContext) }
    private val audioRecordDataSource by lazy { AndroidAudioRecordDataSource(clock) }
    private val audioDetectionPolicy by lazy { AudioDetectionPolicy() }
    private val acousticSignalDetector by lazy { DeterministicAcousticSignalDetector(audioDetectionPolicy) }
    private val audioRepository by lazy {
        AndroidAudioRepository(
            audioRecordDataSource = audioRecordDataSource,
            permissionChecker = audioPermissionChecker,
            consentRepository = consentRepository,
            journeySessionRepository = journeySessionRepository,
            policy = audioDetectionPolicy,
            detector = acousticSignalDetector
        )
    }

    // Milestone 6: Multi-Signal Risk Fusion Engine
    private val riskFusionEngine by lazy { com.saferouteai.domain.risk.RiskFusionEngine() }

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
            anomalyDetector = anomalyDetector,
            audioRepository = audioRepository,
            audioPermissionChecker = audioPermissionChecker,
            riskFusionEngine = riskFusionEngine
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

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.provideFactory(themePreferencesRepository)
    }

    private val testAnomalyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val isDebuggable = (context?.applicationInfo?.flags?.and(android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) ?: 0) != 0
            if (!isDebuggable) return

            if (intent?.action == "com.saferouteai.ACTION_INJECT_TEST_ANOMALY") {
                val typeStr = intent.getStringExtra("type") ?: "PROLONGED_STOP"
                val anomalyType = try {
                    com.saferouteai.domain.model.anomaly.AnomalyType.valueOf(typeStr)
                } catch (e: Exception) {
                    com.saferouteai.domain.model.anomaly.AnomalyType.PROLONGED_STOP
                }
                journeyViewModel.injectTestLocationAnomaly(anomalyType)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            val filter = android.content.IntentFilter("com.saferouteai.ACTION_INJECT_TEST_ANOMALY")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(testAnomalyReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(testAnomalyReceiver, filter)
            }
        }

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val isSystemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SafeRouteTheme(darkTheme = isDarkTheme) {
                SafeRouteNavHost(
                    journeyViewModel = journeyViewModel,
                    profileViewModel = profileViewModel,
                    trustedContactsViewModel = trustedContactsViewModel,
                    consentViewModel = consentViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            try {
                unregisterReceiver(testAnomalyReceiver)
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionChecker.refreshStatus()
        audioPermissionChecker.refreshStatus()
        journeyViewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        // Strict foreground-only invariant: pause location tracking when activity leaves foreground
        journeyViewModel.onAppBackgrounded()
    }
}
