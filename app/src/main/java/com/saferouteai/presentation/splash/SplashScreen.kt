package com.saferouteai.presentation.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferouteai.presentation.common.PrimaryButton
import com.saferouteai.presentation.theme.PrimaryButtonGradient
import com.saferouteai.presentation.theme.spacing

/**
 * Splash & Onboarding Screen matching Option 1 & 2 Screen 1 from the SafeRoute reference design.
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val skyTopColor = if (isDark) Color(0xFF090D16) else Color(0xFFE2E8F0)
    val skyBottomColor = if (isDark) Color(0xFF131C2E) else Color(0xFFF1F5F9)
    val mountainBack = if (isDark) Color(0xFF1E2C4A) else Color(0xFF94A3B8)
    val mountainFront = if (isDark) Color(0xFF142036) else Color(0xFF64748B)
    val roadColor = if (isDark) Color(0xFF2563EB) else Color(0xFF1D4ED8)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: SafeRoute AI Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "SafeRoute Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SafeRoute AI",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Safer Journeys. Brighter Tomorrows.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Middle Section: Hero travel atmosphere canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(listOf(skyTopColor, skyBottomColor))
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(32.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Distant mountains
                    val backMountain = Path().apply {
                        moveTo(0f, h * 0.70f)
                        lineTo(w * 0.25f, h * 0.40f)
                        lineTo(w * 0.55f, h * 0.65f)
                        lineTo(w * 0.85f, h * 0.35f)
                        lineTo(w, h * 0.60f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(backMountain, mountainBack)

                    // Foreground mountains
                    val frontMountain = Path().apply {
                        moveTo(0f, h * 0.75f)
                        lineTo(w * 0.40f, h * 0.50f)
                        lineTo(w * 0.70f, h * 0.80f)
                        lineTo(w, h * 0.55f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(frontMountain, mountainFront)

                    // Curving road into horizon
                    val road = Path().apply {
                        moveTo(w * 0.48f, h * 0.52f)
                        cubicTo(
                            w * 0.45f, h * 0.65f,
                            w * 0.30f, h * 0.80f,
                            w * 0.20f, h
                        )
                        lineTo(w * 0.80f, h)
                        cubicTo(
                            w * 0.65f, h * 0.80f,
                            w * 0.55f, h * 0.65f,
                            w * 0.52f, h * 0.52f
                        )
                        close()
                    }
                    drawPath(road, Color(0xFF0F172A))

                    // Road center dashed line
                    val centerLine = Path().apply {
                        moveTo(w * 0.50f, h * 0.52f)
                        cubicTo(
                            w * 0.49f, h * 0.68f,
                            w * 0.48f, h * 0.85f,
                            w * 0.50f, h
                        )
                    }
                    drawPath(
                        centerLine,
                        roadColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 15f))
                        )
                    )
                }

                // Ambient travel quote banner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "100% Local On-Device Safety Intelligence",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom Section: Primary CTA "Get Started"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton(
                    text = "Get Started",
                    trailingIcon = Icons.Default.ArrowForward,
                    onClick = onGetStarted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No cloud tracking • Privacy-first companion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
