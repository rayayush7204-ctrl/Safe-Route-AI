package com.saferouteai.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saferouteai.domain.model.theme.ThemeMode
import com.saferouteai.presentation.common.SafeRouteCard
import com.saferouteai.presentation.common.SafeRouteDivider
import com.saferouteai.presentation.common.SafeRouteTopBar
import com.saferouteai.presentation.common.SectionHeader
import com.saferouteai.presentation.common.SettingsRow
import com.saferouteai.presentation.theme.cornerRadius
import com.saferouteai.presentation.theme.spacing

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTrustedContacts: () -> Unit,
    onNavigateToPrivacyConsent: () -> Unit,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToRoute: ((String) -> Unit)? = null
) {
    val currentThemeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SafeRouteTopBar(
                title = "Settings",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (onNavigateToRoute != null) {
                com.saferouteai.presentation.common.SafeRouteBottomNavigation(
                    currentRoute = com.saferouteai.presentation.navigation.Screen.Settings.route,
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            // General Settings List matching Option 2 Screen 7
            SettingsRow(
                title = "Profile",
                description = "Configure your display name, contact phone, and alert email.",
                icon = Icons.Default.Person,
                trailingContent = {
                    Text(
                        text = "Edit >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = onNavigateToProfile
            )

            SettingsRow(
                title = "Journey Preferences",
                description = "Local checkpoint intervals, corridor tolerances, and audio sensitivity.",
                icon = Icons.Default.Shield,
                trailingContent = {
                    Text(
                        text = "Standard >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            SettingsRow(
                title = "Notifications",
                description = "Foreground service notifications and periodic checkpoint alerts.",
                icon = Icons.Default.Lock,
                trailingContent = {
                    Text(
                        text = "Enabled >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )

            // Appearance Section
            SectionHeader(
                title = "Appearance",
                subtitle = "Choose how SafeRoute AI looks on your device"
            )

            SafeRouteCard {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                    ThemeOptionRow(
                        title = "System default",
                        description = "Matches your device's light or dark mode setting",
                        icon = Icons.Default.BrightnessAuto,
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { settingsViewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )

                    SafeRouteDivider()

                    ThemeOptionRow(
                        title = "Dark theme",
                        description = "Deep navy, safety-focused dark mode (Recommended)",
                        icon = Icons.Default.Brightness4,
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = { settingsViewModel.setThemeMode(ThemeMode.DARK) }
                    )

                    SafeRouteDivider()

                    ThemeOptionRow(
                        title = "Light theme",
                        description = "Clean, soft high-contrast light mode",
                        icon = Icons.Default.Brightness7,
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { settingsViewModel.setThemeMode(ThemeMode.LIGHT) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Account & Safeguards Section
            SectionHeader(
                title = "Account & Safeguards",
                subtitle = "Emergency contacts and identity verification"
            )

            SettingsRow(
                title = "Trusted Contacts",
                description = "Manage emergency contacts notified during critical safety events.",
                icon = Icons.Default.ContactEmergency,
                trailingContent = {
                    Text(
                        text = "Manage >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = onNavigateToTrustedContacts
            )

            SettingsRow(
                title = "Privacy & Consent",
                description = "Manage explicit consent for location and audio analysis.",
                icon = Icons.Default.Lock,
                trailingContent = {
                    Text(
                        text = "Review >",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = onNavigateToPrivacyConsent
            )

            SettingsRow(
                title = "About SafeRoute AI",
                description = "Version 1.0.0 (Milestone 7B) • 100% On-Device Local Processing",
                icon = Icons.Default.Shield,
                trailingContent = {
                    Text(
                        text = "v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.cornerRadius.sm))
            .clickable { onClick() }
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
