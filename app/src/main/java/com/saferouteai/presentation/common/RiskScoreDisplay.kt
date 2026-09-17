package com.saferouteai.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferouteai.presentation.theme.NumericScoreStyle

/**
 * Reusable display for a numeric risk score (0-100).
 */
@Composable
fun RiskScoreDisplay(
    score: Int,
    modifier: Modifier = Modifier,
    scoreColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "$score",
            style = NumericScoreStyle,
            color = scoreColor
        )
        Text(
            text = " / 100",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )
    }
}
