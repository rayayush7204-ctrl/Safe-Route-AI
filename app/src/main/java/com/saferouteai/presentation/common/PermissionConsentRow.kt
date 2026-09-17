package com.saferouteai.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.presentation.theme.safeRouteColors
import com.saferouteai.presentation.theme.spacing

/**
 * Reusable row for displaying permission or consent states with optional toggle.
 */
@Composable
fun PermissionConsentRow(
    title: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier,
    onToggle: ((Boolean) -> Unit)? = null
) {
    SafeRouteCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked
                val iconTint = if (isGranted) MaterialTheme.safeRouteColors.success else MaterialTheme.colorScheme.outline

                Icon(
                    imageVector = icon,
                    contentDescription = if (isGranted) "Granted" else "Not granted",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onToggle != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Switch(
                    checked = isGranted,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
