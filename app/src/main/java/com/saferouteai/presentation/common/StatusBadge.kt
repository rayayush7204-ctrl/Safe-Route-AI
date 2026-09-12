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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.presentation.theme.StatusActive
import com.saferouteai.presentation.theme.StatusCompleted
import com.saferouteai.presentation.theme.StatusIdle

@Composable
fun StatusBadge(
    journeyState: JourneyState,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (journeyState) {
        JourneyState.IDLE -> StatusIdle to "No active journey"
        JourneyState.ACTIVE -> StatusActive to "Journey started"
        JourneyState.COMPLETED -> StatusCompleted to "Journey completed"
    }

    Box(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = statusColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}
