package com.saferouteai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.saferouteai.data.local.SafeRouteDatabase
import com.saferouteai.data.local.datastore.ConsentDataStore
import com.saferouteai.data.repository.DataStoreConsentRepository
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

    // ViewModels
    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.provideFactory(journeyRepository)
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
}
