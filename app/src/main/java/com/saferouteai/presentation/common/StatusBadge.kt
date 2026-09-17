package com.saferouteai.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.presentation.theme.cornerRadius
import com.saferouteai.presentation.theme.safeRouteColors
import com.saferouteai.presentation.theme.spacing

@Composable
fun StatusBadge(
    journeyState: JourneyState,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (journeyState) {
        JourneyState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant to "No active journey"
        JourneyState.ACTIVE -> MaterialTheme.safeRouteColors.success to "Journey active"
        JourneyState.COMPLETED -> MaterialTheme.colorScheme.primary to "Journey completed"
    }

    Box(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(MaterialTheme.cornerRadius.full)
            )
            .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = statusColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }
    }
}
