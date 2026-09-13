package com.saferouteai.presentation.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.domain.model.audio.AudioCaptureState

/**
 * Displays the current audio capture pipeline status during an active Safe Journey.
 * Shows capture state, consent/permission readiness, and a pulsing indicator when actively capturing.
 */
@Composable
fun AudioSafetyStatusCard(
    audioCaptureState: AudioCaptureState,
    isAudioConsentGranted: Boolean,
    isAudioPermissionGranted: Boolean,
    onRequestPermission: () -> Unit = {},
    onStartCapture: () -> Unit = {},
    onStopCapture: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCapturing = audioCaptureState.isCapturing
    val isStarting = audioCaptureState == AudioCaptureState.STARTING

    val containerColor by animateColorAsState(
        targetValue = when {
            isCapturing -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            isStarting -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            audioCaptureState == AudioCaptureState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "audioCardColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing indicator when capturing
                    if (isCapturing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "audioPulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "audioPulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .alpha(pulseAlpha)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Icon(
                        imageVector = if (isCapturing || isStarting) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isCapturing) "Audio capturing" else "Audio idle",
                        tint = if (isCapturing) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio Safety Pipeline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = when (audioCaptureState) {
                        AudioCaptureState.IDLE -> "Idle"
                        AudioCaptureState.STARTING -> "Starting…"
                        AudioCaptureState.CAPTURING -> "Active"
                        AudioCaptureState.STOPPING -> "Stopping…"
                        AudioCaptureState.STOPPED -> "Stopped"
                        AudioCaptureState.ERROR -> "Error"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isCapturing -> MaterialTheme.colorScheme.tertiary
                        audioCaptureState == AudioCaptureState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Readiness gates status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GateStatusChip(
                    label = "Consent",
                    isReady = isAudioConsentGranted
                )
                GateStatusChip(
                    label = "Permission",
                    isReady = isAudioPermissionGranted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isCapturing -> "On-device audio capture active. All processing is local and volatile."
                    !isAudioConsentGranted -> "Enable Audio Processing Consent in Privacy settings to activate."
                    !isAudioPermissionGranted -> "Grant microphone permission to activate audio capture."
                    audioCaptureState == AudioCaptureState.ERROR -> "Audio capture encountered an error. Restart journey to retry."
                    else -> "Audio pipeline ready. Will activate with the next journey start."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isCapturing -> {
                    OutlinedButton(
                        onClick = onStopCapture,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop Audio Monitoring",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                !isAudioPermissionGranted -> {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isAudioConsentGranted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Audio Monitoring",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onStartCapture,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isAudioConsentGranted && isAudioPermissionGranted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Audio Monitoring",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GateStatusChip(
    label: String,
    isReady: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isReady) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isReady) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
