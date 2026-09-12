package com.saferouteai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.presentation.home.JourneyViewModel
import com.saferouteai.presentation.navigation.SafeRouteNavHost
import com.saferouteai.presentation.theme.SafeRouteTheme

/**
 * Main application entry point for SafeRoute AI.
 *
 * Keeps business logic entirely delegated to the presentation and domain layers.
 */
class MainActivity : ComponentActivity() {

    // For Milestone 1 foundation, use the thread-safe InMemoryJourneyRepository.
    // In future milestones, this will transition to dependency injection (e.g. Hilt/Koin).
    private val journeyRepository by lazy { InMemoryJourneyRepository() }

    private val journeyViewModel: JourneyViewModel by viewModels {
        JourneyViewModel.provideFactory(journeyRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeRouteTheme {
                SafeRouteNavHost(journeyViewModel = journeyViewModel)
            }
        }
    }
}
