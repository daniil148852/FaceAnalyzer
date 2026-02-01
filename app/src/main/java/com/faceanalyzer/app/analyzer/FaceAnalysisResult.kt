package com.faceanalyzer.app.analyzer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

data class FaceAnalysisResult(
    val faceDetected: Boolean = false,
    val boundingBox: Rect? = null,
    val emotion: Emotion = Emotion.NEUTRAL,
    val emotionConfidence: Float = 0f,
    val smilingProbability: Float = 0f,
    val leftEyeOpenProbability: Float = 0f,
    val rightEyeOpenProbability: Float = 0f,
    val headRotationX: Float = 0f,
    val headRotationY: Float = 0f,
    val headRotationZ: Float = 0f,
    val faceCondition: FaceCondition = FaceCondition(),
    val landmarks: List<FaceLandmark> = emptyList(),
    val contours: List<FaceContour> = emptyList(),
    val meshPoints: List<Offset> = emptyList(),
    val meshTriangles: List<MeshTriangle> = emptyList()
)

data class FaceCondition(
    val overallScore: Int = 0,
    val symmetryScore: Int = 0,
    val skinHealthEstimate: Int = 0,
    val eyeHealthScore: Int = 0,
    val facialProportionScore: Int = 0,
    val suggestions: List<String> = emptyList()
)

data class FaceLandmark(
    val type: LandmarkType,
    val position: Offset
)

data class FaceContour(
    val type: ContourType,
    val points: List<Offset>
)

data class MeshTriangle(
    val point1: Offset,
    val point2: Offset,
    val point3: Offset
)

enum class Emotion(val emoji: String, val displayName: String) {
    HAPPY("😊", "Happy"),
    SAD("😢", "Sad"),
    ANGRY("😠", "Angry"),
    SURPRISED("😲", "Surprised"),
    NEUTRAL("😐", "Neutral"),
    FEAR("😨", "Fearful"),
    DISGUST("🤢", "Disgusted"),
    WINK("😉", "Winking")
}

enum class LandmarkType {
    LEFT_EYE, RIGHT_EYE, NOSE_BASE, LEFT_EAR, RIGHT_EAR,
    LEFT_MOUTH, RIGHT_MOUTH, MOUTH_BOTTOM, LEFT_CHEEK, RIGHT_CHEEK
}

enum class ContourType {
    FACE, LEFT_EYEBROW_TOP, LEFT_EYEBROW_BOTTOM,
    RIGHT_EYEBROW_TOP, RIGHT_EYEBROW_BOTTOM,
    LEFT_EYE, RIGHT_EYE, UPPER_LIP_TOP, UPPER_LIP_BOTTOM,
    LOWER_LIP_TOP, LOWER_LIP_BOTTOM, NOSE_BRIDGE, NOSE_BOTTOM
}

enum class HealthLevel(val color: Long, val label: String) {
    EXCELLENT(0xFF00E676, "Excellent"),
    GOOD(0xFF69F0AE, "Good"),
    FAIR(0xFFFFD54F, "Fair"),
    POOR(0xFFFF5252, "Needs Attention")
}

fun Int.toHealthLevel(): HealthLevel = when {
    this >= 85 -> HealthLevel.EXCELLENT
    this >= 70 -> HealthLevel.GOOD
    this >= 50 -> HealthLevel.FAIR
    else -> HealthLevel.POOR
}
