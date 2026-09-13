package com.saferouteai.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.session.CheckpointState

/**
 * Card displaying checkpoint status and periodic reminder time during an active Safe Journey.
 */
@Composable
fun CheckpointStatusCard(
    checkpointState: CheckpointState,
    nextCheckpointRemainingMs: Long?,
    onCheckInClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (checkpointState) {
                CheckpointState.DUE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                CheckpointState.MISSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (checkpointState) {
                            CheckpointState.DUE -> Icons.Default.NotificationsActive
                            CheckpointState.MISSED -> Icons.Default.HourglassTop
                            else -> Icons.Default.VerifiedUser
                        },
                        contentDescription = null,
                        tint = when (checkpointState) {
                            CheckpointState.DUE -> MaterialTheme.colorScheme.error
                            CheckpointState.MISSED -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Checkpoint",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = when (checkpointState) {
                        CheckpointState.SCHEDULED -> "Armed"
                        CheckpointState.DUE -> "Check-In Due"
                        CheckpointState.ACKNOWLEDGED -> "Confirmed"
                        CheckpointState.MISSED -> "Missed"
                        CheckpointState.DISARMED -> "Off"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (checkpointState) {
                        CheckpointState.DUE -> MaterialTheme.colorScheme.error
                        CheckpointState.MISSED -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (checkpointState) {
                CheckpointState.DUE -> {
                    Text(
                        text = "Your periodic check-in is ready. Please confirm you are safe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckInClicked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Check In Now (I'm Okay)")
                    }
                }
                CheckpointState.MISSED -> {
                    Text(
                        text = "A previous check-in window elapsed without confirmation. You can confirm anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckInClicked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm I'm Okay")
                    }
                }
                else -> {
                    val remainingText = if (nextCheckpointRemainingMs != null && nextCheckpointRemainingMs > 0) {
                        val minutes = (nextCheckpointRemainingMs / 60_000L).coerceAtLeast(1)
                        "Next check-in in approx. $minutes min"
                    } else {
                        "Periodic check-ins active"
                    }
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
