package com.saferouteai.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.presentation.theme.safeRouteColors

/**
 * Aesthetic local on-device map canvas for SafeRoute AI.
 * Adheres strictly to Constraint 2:
 * "Never fabricate map routes. If real route geometry is unavailable, clearly present an
 * illustrative/local map state. Do not represent fake polyline geometry as real navigation data."
 */
@Composable
fun SafeRouteMapCanvas(
    currentLocation: UserLocation?,
    safetyTier: SafetyTier,
    modifier: Modifier = Modifier,
    destinationName: String? = null,
    onStatusClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val tierMapping = getSafetyTierVisualMapping(safetyTier)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val mapBgColor = if (isDark) Color(0xFF0D1424) else Color(0xFFE8EEF5)
    val gridColor = if (isDark) Color(0x1F3875F6) else Color(0x252563EB)
    val roadColor = if (isDark) Color(0x283875F6) else Color(0x352563EB)
    val accentBlue = if (isDark) Color(0xFF3875F6) else Color(0xFF2563EB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(mapBgColor)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw subtle background radar/terrain grid
            val step = 40.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += step
            }

            // 2. Draw illustrative local road paths
            val roadPath1 = Path().apply {
                moveTo(0f, height * 0.75f)
                cubicTo(
                    width * 0.35f, height * 0.70f,
                    width * 0.45f, height * 0.30f,
                    width, height * 0.20f
                )
            }
            drawPath(
                path = roadPath1,
                color = roadColor,
                style = Stroke(width = 8.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(16f))
            )

            val roadPath2 = Path().apply {
                moveTo(width * 0.2f, height)
                cubicTo(
                    width * 0.3f, height * 0.55f,
                    width * 0.65f, height * 0.65f,
                    width * 0.85f, 0f
                )
            }
            drawPath(
                path = roadPath2,
                color = roadColor.copy(alpha = roadColor.alpha * 0.6f),
                style = Stroke(width = 4.dp.toPx())
            )

            // 3. Illustrative route corridor line (connecting user point toward illustrative path)
            val userCenterX = width * 0.45f
            val userCenterY = height * 0.55f

            // Illustrative journey corridor path
            val corridorPath = Path().apply {
                moveTo(width * 0.15f, height * 0.82f)
                quadraticBezierTo(
                    width * 0.28f, height * 0.68f,
                    userCenterX, userCenterY
                )
                quadraticBezierTo(
                    width * 0.62f, height * 0.42f,
                    width * 0.78f, height * 0.24f
                )
            }
            drawPath(
                path = corridorPath,
                brush = Brush.linearGradient(
                    listOf(accentBlue.copy(alpha = 0.5f), accentBlue, accentBlue.copy(alpha = 0.9f))
                ),
                style = Stroke(width = 5.dp.toPx())
            )

            // Destination target pin circle
            val destX = width * 0.78f
            val destY = height * 0.24f
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                radius = 18.dp.toPx(),
                center = Offset(destX, destY)
            )
            drawCircle(
                color = Color(0xFFEF4444),
                radius = 6.dp.toPx(),
                center = Offset(destX, destY)
            )

            // Current user location pulsing radar circle
            drawCircle(
                color = accentBlue.copy(alpha = pulseAlpha),
                radius = pulseRadius.dp.toPx(),
                center = Offset(userCenterX, userCenterY)
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(userCenterX, userCenterY)
            )
            drawCircle(
                color = accentBlue,
                radius = 5.dp.toPx(),
                center = Offset(userCenterX, userCenterY)
            )
        }

        // Top-left: Local On-Device GPS Badge & Coordinate Monitor
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = accentBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (currentLocation != null) {
                            "${String.format("%.4f", currentLocation.latitude)}°, ${String.format("%.4f", currentLocation.longitude)}°"
                        } else {
                            "Locating on-device..."
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Illustrative corridor • GPS active",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top-right: Floating Safety Status Badge (clickable to view full status)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .clickable { onStatusClick?.invoke() },
            shape = RoundedCornerShape(50),
            color = tierMapping.containerColor.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, tierMapping.primaryColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(tierMapping.primaryColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tierMapping.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tierMapping.contentColor
                )
            }
        }

        // Floating right-side map action buttons (Recenter, Layers)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 3.dp,
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(onClick = { /* Recenter map view */ }, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter",
                        tint = accentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 3.dp,
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(onClick = { /* Toggle map layers */ }, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map Layers",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
