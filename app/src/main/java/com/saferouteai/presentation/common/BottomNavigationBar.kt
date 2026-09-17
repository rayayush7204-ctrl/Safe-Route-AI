package com.saferouteai.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.saferouteai.presentation.navigation.Screen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val PrimaryBottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = Screen.Journey.route,
        label = "Journey",
        icon = Icons.Default.Navigation
    ),
    BottomNavItem(
        route = Screen.TrustedContacts.route,
        label = "Contacts",
        icon = Icons.Default.People
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        icon = Icons.Default.Settings
    )
)

/**
 * Reusable bottom navigation bar for the core destinations of SafeRoute AI.
 * Strictly uses existing application routes without placeholder destinations.
 */
@Composable
fun SafeRouteBottomNavigation(
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = PrimaryBottomNavItems
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onNavigateToRoute(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
