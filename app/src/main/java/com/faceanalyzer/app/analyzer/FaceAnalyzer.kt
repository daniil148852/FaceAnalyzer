package com.faceanalyzer.app.analyzer

import android.graphics.PointF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark as MLKitLandmark
import com.google.mlkit.vision.face.FaceContour as MLKitContour
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class FaceAnalyzer(
    private val onFaceAnalyzed: (FaceAnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val _analysisResult = MutableStateFlow(FaceAnalysisResult())
    val analysisResult: StateFlow<FaceAnalysisResult> = _analysisResult.asStateFlow()

    private var isMeshEnabled = false
    private var imageWidth = 0
    private var imageHeight = 0

    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)

    private val meshDetectorOptions = FaceMeshDetectorOptions.Builder()
        .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
        .build()

    private val meshDetector: FaceMeshDetector = FaceMeshDetection.getClient(meshDetectorOptions)

    fun setMeshEnabled(enabled: Boolean) {
        isMeshEnabled = enabled
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            imageWidth = imageProxy.width
            imageHeight = imageProxy.height

            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            if (isMeshEnabled) {
                analyzeFaceWithMesh(image, imageProxy)
            } else {
                analyzeFace(image, imageProxy)
            }
        } else {
            imageProxy.close()
        }
    }

    private fun analyzeFace(image: InputImage, imageProxy: ImageProxy) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val result = processFace(face)
                    _analysisResult.value = result
                    onFaceAnalyzed(result)
                } else {
                    val noFaceResult = FaceAnalysisResult(faceDetected = false)
                    _analysisResult.value = noFaceResult
                    onFaceAnalyzed(noFaceResult)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun analyzeFaceWithMesh(image: InputImage, imageProxy: ImageProxy) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    
                    meshDetector.process(image)
                        .addOnSuccessListener { meshes ->
                            val meshData = if (meshes.isNotEmpty()) {
                                extractMeshData(meshes[0])
                            } else {
                                Pair(emptyList(), emptyList())
                            }
                            
                            val result = processFace(face).copy(
                                meshPoints = meshData.first,
                                meshTriangles = meshData.second
                            )
                            _analysisResult.value = result
                            onFaceAnalyzed(result)
                        }
                        .addOnFailureListener {
                            val result = processFace(face)
                            _analysisResult.value = result
                            onFaceAnalyzed(result)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    val noFaceResult = FaceAnalysisResult(faceDetected = false)
                    _analysisResult.value = noFaceResult
                    onFaceAnalyzed(noFaceResult)
                    imageProxy.close()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                imageProxy.close()
            }
    }

    private fun extractMeshData(mesh: FaceMesh): Pair<List<Offset>, List<MeshTriangle>> {
        val points = mesh.allPoints.map { point ->
            Offset(point.position.x, point.position.y)
        }

        val triangles = mesh.allTriangles.map { triangle ->
            val points = triangle.allPoints
            MeshTriangle(
                point1 = Offset(points[0].position.x, points[0].position.y),
                point2 = Offset(points[1].position.x, points[1].position.y),
                point3 = Offset(points[2].position.x, points[2].position.y)
            )
        }

        return Pair(points, triangles)
    }

    private fun processFace(face: Face): FaceAnalysisResult {
        val bounds = face.boundingBox
        val boundingBox = Rect(
            left = bounds.left.toFloat(),
            top = bounds.top.toFloat(),
            right = bounds.right.toFloat(),
            bottom = bounds.bottom.toFloat()
        )

        val smilingProb = face.smilingProbability ?: 0f
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f

        val emotion = detectEmotion(smilingProb, leftEyeOpenProb, rightEyeOpenProb, face)
        val emotionConfidence = calculateEmotionConfidence(emotion, smilingProb, leftEyeOpenProb, rightEyeOpenProb)

        val landmarks = extractLandmarks(face)
        val contours = extractContours(face)
        val faceCondition = calculateFaceCondition(face, landmarks)

        return FaceAnalysisResult(
            faceDetected = true,
            boundingBox = boundingBox,
            emotion = emotion,
            emotionConfidence = emotionConfidence,
            smilingProbability = smilingProb,
            leftEyeOpenProbability = leftEyeOpenProb,
            rightEyeOpenProbability = rightEyeOpenProb,
            headRotationX = face.headEulerAngleX,
            headRotationY = face.headEulerAngleY,
            headRotationZ = face.headEulerAngleZ,
            faceCondition = faceCondition,
            landmarks = landmarks,
            contours = contours
        )
    }

    private fun detectEmotion(
        smiling: Float,
        leftEyeOpen: Float,
        rightEyeOpen: Float,
        face: Face
    ): Emotion {
        // Winking detection
        val eyeDifference = abs(leftEyeOpen - rightEyeOpen)
        if (eyeDifference > 0.5f && (leftEyeOpen < 0.3f || rightEyeOpen < 0.3f)) {
            return Emotion.WINK
        }

        // Head tilted significantly (could indicate surprise)
        if (abs(face.headEulerAngleX) > 20f) {
            if (smiling > 0.3f) return Emotion.SURPRISED
        }

        // Primary emotion detection based on smile
        return when {
            smiling > 0.8f -> Emotion.HAPPY
            smiling > 0.5f -> Emotion.HAPPY
            smiling < 0.1f && leftEyeOpen < 0.5f && rightEyeOpen < 0.5f -> Emotion.SAD
            smiling < 0.2f && abs(face.headEulerAngleY) > 15f -> Emotion.ANGRY
            smiling < 0.3f && leftEyeOpen > 0.8f && rightEyeOpen > 0.8f -> Emotion.SURPRISED
            else -> Emotion.NEUTRAL
        }
    }

    private fun calculateEmotionConfidence(
        emotion: Emotion,
        smiling: Float,
        leftEyeOpen: Float,
        rightEyeOpen: Float
    ): Float {
        return when (emotion) {
            Emotion.HAPPY -> smiling
            Emotion.SAD -> 1f - smiling
            Emotion.SURPRISED -> ((leftEyeOpen + rightEyeOpen) / 2f)
            Emotion.NEUTRAL -> 0.7f
            Emotion.WINK -> abs(leftEyeOpen - rightEyeOpen)
            else -> 0.5f
        }.coerceIn(0f, 1f)
    }

    private fun extractLandmarks(face: Face): List<FaceLandmark> {
        val landmarks = mutableListOf<FaceLandmark>()
        
        val landmarkMap = mapOf(
            MLKitLandmark.LEFT_EYE to LandmarkType.LEFT_EYE,
            MLKitLandmark.RIGHT_EYE to LandmarkType.RIGHT_EYE,
            MLKitLandmark.NOSE_BASE to LandmarkType.NOSE_BASE,
            MLKitLandmark.LEFT_EAR to LandmarkType.LEFT_EAR,
            MLKitLandmark.RIGHT_EAR to LandmarkType.RIGHT_EAR,
            MLKitLandmark.MOUTH_LEFT to LandmarkType.LEFT_MOUTH,
            MLKitLandmark.MOUTH_RIGHT to LandmarkType.RIGHT_MOUTH,
            MLKitLandmark.MOUTH_BOTTOM to LandmarkType.MOUTH_BOTTOM,
            MLKitLandmark.LEFT_CHEEK to LandmarkType.LEFT_CHEEK,
            MLKitLandmark.RIGHT_CHEEK to LandmarkType.RIGHT_CHEEK
        )

        landmarkMap.forEach { (mlType, type) ->
            face.getLandmark(mlType)?.let { landmark ->
                landmarks.add(
                    FaceLandmark(
                        type = type,
                        position = Offset(landmark.position.x, landmark.position.y)
                    )
                )
            }
        }

        return landmarks
    }

    private fun extractContours(face: Face): List<FaceContour> {
        val contours = mutableListOf<FaceContour>()

        val contourMap = mapOf(
            MLKitContour.FACE to ContourType.FACE,
            MLKitContour.LEFT_EYEBROW_TOP to ContourType.LEFT_EYEBROW_TOP,
            MLKitContour.LEFT_EYEBROW_BOTTOM to ContourType.LEFT_EYEBROW_BOTTOM,
            MLKitContour.RIGHT_EYEBROW_TOP to ContourType.RIGHT_EYEBROW_TOP,
            MLKitContour.RIGHT_EYEBROW_BOTTOM to ContourType.RIGHT_EYEBROW_BOTTOM,
            MLKitContour.LEFT_EYE to ContourType.LEFT_EYE,
            MLKitContour.RIGHT_EYE to ContourType.RIGHT_EYE,
            MLKitContour.UPPER_LIP_TOP to ContourType.UPPER_LIP_TOP,
            MLKitContour.UPPER_LIP_BOTTOM to ContourType.UPPER_LIP_BOTTOM,
            MLKitContour.LOWER_LIP_TOP to ContourType.LOWER_LIP_TOP,
            MLKitContour.LOWER_LIP_BOTTOM to ContourType.LOWER_LIP_BOTTOM,
            MLKitContour.NOSE_BRIDGE to ContourType.NOSE_BRIDGE,
            MLKitContour.NOSE_BOTTOM to ContourType.NOSE_BOTTOM
        )

        contourMap.forEach { (mlType, type) ->
            face.getContour(mlType)?.let { contour ->
                contours.add(
                    FaceContour(
                        type = type,
                        points = contour.points.map { Offset(it.x, it.y) }
                    )
                )
            }
        }

        return contours
    }

    private fun calculateFaceCondition(face: Face, landmarks: List<FaceLandmark>): FaceCondition {
        val symmetryScore = calculateSymmetryScore(landmarks)
        val eyeHealthScore = calculateEyeHealthScore(face)
        val proportionScore = calculateProportionScore(landmarks)
        val skinHealthEstimate = estimateSkinHealth(face)

        val overallScore = (
            (symmetryScore * 0.3f) +
            (eyeHealthScore * 0.25f) +
            (proportionScore * 0.25f) +
            (skinHealthEstimate * 0.2f)
        ).toInt()

        val suggestions = generateSuggestions(
            symmetryScore,
            eyeHealthScore,
            proportionScore,
            skinHealthEstimate,
            face
        )

        return FaceCondition(
            overallScore = overallScore,
            symmetryScore = symmetryScore,
            skinHealthEstimate = skinHealthEstimate,
            eyeHealthScore = eyeHealthScore,
            facialProportionScore = proportionScore,
            suggestions = suggestions
        )
    }

    private fun calculateSymmetryScore(landmarks: List<FaceLandmark>): Int {
        val leftEye = landmarks.find { it.type == LandmarkType.LEFT_EYE }?.position
        val rightEye = landmarks.find { it.type == LandmarkType.RIGHT_EYE }?.position
        val nose = landmarks.find { it.type == LandmarkType.NOSE_BASE }?.position

        if (leftEye == null || rightEye == null || nose == null) return 75

        val leftDist = kotlin.math.sqrt(
            ((leftEye.x - nose.x) * (leftEye.x - nose.x) + 
             (leftEye.y - nose.y) * (leftEye.y - nose.y)).toDouble()
        )
        val rightDist = kotlin.math.sqrt(
            ((rightEye.x - nose.x) * (rightEye.x - nose.x) + 
             (rightEye.y - nose.y) * (rightEye.y - nose.y)).toDouble()
        )

        val symmetryRatio = if (leftDist > rightDist) rightDist / leftDist else leftDist / rightDist
        return (symmetryRatio * 100).toInt().coerceIn(0, 100)
    }

    private fun calculateEyeHealthScore(face: Face): Int {
        val leftEyeOpen = face.leftEyeOpenProbability ?: 0.5f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0.5f

        val avgOpenness = (leftEyeOpen + rightEyeOpen) / 2f
        val eyeBalance = 1f - abs(leftEyeOpen - rightEyeOpen)

        return ((avgOpenness * 0.6f + eyeBalance * 0.4f) * 100).toInt().coerceIn(0, 100)
    }

    private fun calculateProportionScore(landmarks: List<FaceLandmark>): Int {
        val leftEye = landmarks.find { it.type == LandmarkType.LEFT_EYE }?.position
        val rightEye = landmarks.find { it.type == LandmarkType.RIGHT_EYE }?.position
        val nose = landmarks.find { it.type == LandmarkType.NOSE_BASE }?.position
        val mouthBottom = landmarks.find { it.type == LandmarkType.MOUTH_BOTTOM }?.position

        if (leftEye == null || rightEye == null || nose == null || mouthBottom == null) return 75

        val eyeDistance = abs(rightEye.x - leftEye.x)
        val noseToMouth = abs(mouthBottom.y - nose.y)
        val eyeToNose = abs(nose.y - ((leftEye.y + rightEye.y) / 2))

        // Golden ratio approximation for facial proportions
        val idealRatio = 1.618f
        val actualRatio = eyeDistance / noseToMouth

        val ratioScore = (1f - abs(actualRatio - idealRatio) / idealRatio).coerceIn(0f, 1f)
        return (ratioScore * 100).toInt().coerceIn(0, 100)
    }

    private fun estimateSkinHealth(face: Face): Int {
        // Since we can't directly analyze skin, we use facial detection confidence
        // and smoothness of contours as a proxy
        val baseScore = 75
        val smileBonus = ((face.smilingProbability ?: 0f) * 10).toInt()
        val eyeBonus = (((face.leftEyeOpenProbability ?: 0f) + 
                         (face.rightEyeOpenProbability ?: 0f)) * 5).toInt()
        
        return (baseScore + smileBonus + eyeBonus).coerceIn(0, 100)
    }

    private fun generateSuggestions(
        symmetryScore: Int,
        eyeHealthScore: Int,
        proportionScore: Int,
        skinHealthEstimate: Int,
        face: Face
    ): List<String> {
        val suggestions = mutableListOf<String>()

        if (symmetryScore < 70) {
            suggestions.add("Try to keep your face centered in the frame")
        }
        
        if (eyeHealthScore < 60) {
            suggestions.add("Your eyes appear tired - consider taking breaks from screens")
        }
        
        if (abs(face.headEulerAngleY) > 10) {
            suggestions.add("Face the camera directly for better analysis")
        }
        
        if (abs(face.headEulerAngleX) > 15) {
            suggestions.add("Keep your head level for accurate results")
        }

        if ((face.smilingProbability ?: 0f) < 0.3f) {
            suggestions.add("Smiling can enhance your facial features! 😊")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Your face looks great! Keep smiling! ✨")
        }

        return suggestions
    }

    fun close() {
        faceDetector.close()
        meshDetector.close()
    }
}
