package com.faceanalyzer.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faceanalyzer.app.analyzer.FaceAnalysisResult
import com.faceanalyzer.app.analyzer.toHealthLevel
import com.faceanalyzer.app.ui.theme.*

@Composable
fun FaceAnalysisCard(
    modifier: Modifier = Modifier,
    analysisResult: FaceAnalysisResult,
    expanded: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with emotion
            EmotionHeader(analysisResult)

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = expanded && analysisResult.faceDetected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    // Overall Score
                    OverallScoreSection(analysisResult.faceCondition.overallScore)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed Metrics
                    MetricsGrid(analysisResult)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggestions
                    SuggestionsSection(analysisResult.faceCondition.suggestions)
                }
            }

            if (!analysisResult.faceDetected) {
                NoFaceDetected()
            }
        }
    }
}

@Composable
private fun EmotionHeader(analysisResult: FaceAnalysisResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (analysisResult.faceDetected) "Status Detected" else "Scanning...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            if (analysisResult.faceDetected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = analysisResult.emotion.emoji,
                        fontSize = 32.sp
                    )
                    Text(
                        text = analysisResult.emotion.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (analysisResult.faceDetected) {
            ConfidenceBadge(
                confidence = (analysisResult.emotionConfidence * 100).toInt()
            )
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$confidence%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun OverallScoreSection(score: Int) {
    val healthLevel = score.toHealthLevel()
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000),
        label = "score"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Face Perfection Score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedScore / 100f },
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp,
                color = Color(healthLevel.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animatedScore.toInt()}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = healthLevel.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(healthLevel.color)
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(analysisResult: FaceAnalysisResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Balance,
                label = "Symmetry",
                value = analysisResult.faceCondition.symmetryScore,
                color = analysisResult.faceCondition.symmetryScore.toHealthLevel().let { Color(it.color) }
            )
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Visibility,
                label = "Eye Health",
                value = analysisResult.faceCondition.eyeHealthScore,
                color = analysisResult.faceCondition.eyeHealthScore.toHealthLevel().let { Color(it.color) }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.FaceRetouchingNatural,
                label = "Skin Health",
                value = analysisResult.faceCondition.skinHealthEstimate,
                color = analysisResult.faceCondition.skinHealthEstimate.toHealthLevel().let { Color(it.color) }
            )
            MetricItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Straighten,
                label = "Proportions",
                value = analysisResult.faceCondition.facialProportionScore,
                color = analysisResult.faceCondition.facialProportionScore.toHealthLevel().let { Color(it.color) }
            )
        }
    }
}

@Composable
private fun MetricItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(800),
        label = "metric"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${animatedValue.toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SuggestionsSection(suggestions: List<String>) {
    Column {
        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TipsAndUpdates,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GradientStart
                )
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NoFaceDetected() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.FaceRetouchingOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No face detected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Text(
            text = "Position your face in the frame",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
