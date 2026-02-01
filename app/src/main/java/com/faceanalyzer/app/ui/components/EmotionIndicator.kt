package com.faceanalyzer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faceanalyzer.app.analyzer.Emotion
import com.faceanalyzer.app.ui.theme.*

@Composable
fun EmotionIndicator(
    modifier: Modifier = Modifier,
    emotion: Emotion,
    confidence: Float
) {
    val emotionColor = when (emotion) {
        Emotion.HAPPY -> HappyColor
        Emotion.SAD -> SadColor
        Emotion.ANGRY -> AngryColor
        Emotion.SURPRISED -> SurprisedColor
        Emotion.NEUTRAL -> NeutralColor
        Emotion.FEAR -> FearColor
        Emotion.DISGUST -> DisgustColor
        Emotion.WINK -> HappyColor
    }

    val animatedColor by animateColorAsState(
        targetValue = emotionColor,
        animationSpec = tween(500),
        label = "emotionColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.3f),
                        animatedColor.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 2.dp,
                color = animatedColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emotion.emoji,
            fontSize = 28.sp,
            modifier = Modifier.scale(scale)
        )
        
        Column {
            Text(
                text = emotion.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
            
            Text(
                text = "${(confidence * 100).toInt()}% confident",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmotionChip(
    emotion: Emotion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (emotion) {
                Emotion.HAPPY -> HappyColor
                Emotion.SAD -> SadColor
                Emotion.ANGRY -> AngryColor
                Emotion.SURPRISED -> SurprisedColor
                Emotion.NEUTRAL -> NeutralColor
                Emotion.FEAR -> FearColor
                Emotion.DISGUST -> DisgustColor
                Emotion.WINK -> HappyColor
            }
        } else {
            Color.Transparent
        },
        animationSpec = tween(300),
        label = "chipBackground"
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.2f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = backgroundColor.copy(alpha = if (isSelected) 1f else 0.3f),
                shape = CircleShape
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emotion.emoji,
            fontSize = 24.sp
        )
    }
}
