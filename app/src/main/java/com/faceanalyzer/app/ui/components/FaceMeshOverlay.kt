package com.faceanalyzer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.faceanalyzer.app.analyzer.FaceAnalysisResult
import com.faceanalyzer.app.analyzer.FaceContour
import com.faceanalyzer.app.analyzer.ContourType
import com.faceanalyzer.app.ui.theme.MeshColor
import com.faceanalyzer.app.ui.theme.MeshColorSecondary

@Composable
fun FaceMeshOverlay(
    modifier: Modifier = Modifier,
    analysisResult: FaceAnalysisResult,
    showMesh: Boolean = false,
    showContours: Boolean = true,
    showLandmarks: Boolean = true,
    imageWidth: Int,
    imageHeight: Int
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight

        if (analysisResult.faceDetected) {
            // Draw bounding box
            analysisResult.boundingBox?.let { box ->
                drawRoundedBoundingBox(
                    left = box.left * scaleX,
                    top = box.top * scaleY,
                    right = box.right * scaleX,
                    bottom = box.bottom * scaleY
                )
            }

            // Draw contours
            if (showContours) {
                analysisResult.contours.forEach { contour ->
                    drawContour(contour, scaleX, scaleY)
                }
            }

            // Draw landmarks
            if (showLandmarks) {
                analysisResult.landmarks.forEach { landmark ->
                    drawCircle(
                        color = Color.Cyan,
                        radius = 8f,
                        center = Offset(
                            landmark.position.x * scaleX,
                            landmark.position.y * scaleY
                        )
                    )
                }
            }

            // Draw mesh if enabled
            if (showMesh && analysisResult.meshTriangles.isNotEmpty()) {
                analysisResult.meshTriangles.forEach { triangle ->
                    drawMeshTriangle(triangle, scaleX, scaleY)
                }
            }
        }
    }
}

private fun DrawScope.drawRoundedBoundingBox(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val cornerLength = 40f
    val strokeWidth = 4f
    val color = MeshColor

    // Top-left corner
    drawLine(color, Offset(left, top + cornerLength), Offset(left, top), strokeWidth = strokeWidth)
    drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = strokeWidth)

    // Top-right corner
    drawLine(color, Offset(right - cornerLength, top), Offset(right, top), strokeWidth = strokeWidth)
    drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = strokeWidth)

    // Bottom-left corner
    drawLine(color, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth = strokeWidth)
    drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = strokeWidth)

    // Bottom-right corner
    drawLine(color, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth = strokeWidth)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = strokeWidth)
}

private fun DrawScope.drawContour(
    contour: FaceContour,
    scaleX: Float,
    scaleY: Float
) {
    if (contour.points.size < 2) return

    val color = when (contour.type) {
        ContourType.FACE -> MeshColor.copy(alpha = 0.7f)
        ContourType.LEFT_EYE, ContourType.RIGHT_EYE -> Color.Cyan.copy(alpha = 0.8f)
        ContourType.LEFT_EYEBROW_TOP, ContourType.LEFT_EYEBROW_BOTTOM,
        ContourType.RIGHT_EYEBROW_TOP, ContourType.RIGHT_EYEBROW_BOTTOM -> Color.Magenta.copy(alpha = 0.7f)
        ContourType.UPPER_LIP_TOP, ContourType.UPPER_LIP_BOTTOM,
        ContourType.LOWER_LIP_TOP, ContourType.LOWER_LIP_BOTTOM -> Color.Red.copy(alpha = 0.7f)
        ContourType.NOSE_BRIDGE, ContourType.NOSE_BOTTOM -> Color.Yellow.copy(alpha = 0.7f)
    }

    val path = Path().apply {
        val firstPoint = contour.points.first()
        moveTo(firstPoint.x * scaleX, firstPoint.y * scaleY)
        
        contour.points.drop(1).forEach { point ->
            lineTo(point.x * scaleX, point.y * scaleY)
        }
        
        if (contour.type == ContourType.FACE || 
            contour.type == ContourType.LEFT_EYE || 
            contour.type == ContourType.RIGHT_EYE) {
            close()
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawMeshTriangle(
    triangle: com.faceanalyzer.app.analyzer.MeshTriangle,
    scaleX: Float,
    scaleY: Float
) {
    val path = Path().apply {
        moveTo(triangle.point1.x * scaleX, triangle.point1.y * scaleY)
        lineTo(triangle.point2.x * scaleX, triangle.point2.y * scaleY)
        lineTo(triangle.point3.x * scaleX, triangle.point3.y * scaleY)
        close()
    }

    drawPath(
        path = path,
        color = MeshColorSecondary.copy(alpha = 0.3f),
        style = Stroke(width = 1f)
    )
}
