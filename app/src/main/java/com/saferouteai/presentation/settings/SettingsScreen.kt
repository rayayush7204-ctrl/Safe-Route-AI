package com.saferouteai.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saferouteai.presentation.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTrustedContacts: () -> Unit,
    onNavigateToPrivacyConsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account & Safeguards",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Profile Section
            SectionCard(
                title = "User Profile",
                description = "Configure your display name, contact phone, and alert email.",
                icon = Icons.Default.Person,
                trailingTag = "Configure",
                onClick = onNavigateToProfile
            )

            // Trusted Contacts Section
            SectionCard(
                title = "Trusted Contacts",
                description = "Manage emergency contacts notified during critical safety events.",
                icon = Icons.Default.ContactEmergency,
                trailingTag = "Manage",
                onClick = onNavigateToTrustedContacts
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Privacy & Governance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Privacy & Consent Section
            SectionCard(
                title = "Privacy & Consent",
                description = "Manage explicit consent for location and emergency sharing.",
                icon = Icons.Default.Lock,
                trailingTag = "Review",
                onClick = onNavigateToPrivacyConsent
            )

            // Permissions Status
            SectionCard(
                title = "System Permissions",
                description = "Zero runtime permissions requested. All data remains 100% on-device.",
                icon = Icons.Default.Shield,
                trailingTag = "0 Required"
            )
        }
    }
}
