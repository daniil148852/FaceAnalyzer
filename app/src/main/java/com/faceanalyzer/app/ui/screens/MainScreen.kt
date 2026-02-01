package com.faceanalyzer.app.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faceanalyzer.app.ui.components.*
import com.faceanalyzer.app.ui.theme.GradientEnd
import com.faceanalyzer.app.ui.theme.GradientStart
import com.faceanalyzer.app.viewmodel.FaceAnalyzerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: FaceAnalyzerViewModel = viewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            cameraPermissionState.status.isGranted -> {
                CameraContent(viewModel = viewModel)
            }
            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
            else -> {
                PermissionRequest(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }
    }
}

@Composable
private fun CameraContent(
    viewModel: FaceAnalyzerViewModel
) {
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isMeshEnabled by viewModel.isMeshEnabled.collectAsState()
    val showContours by viewModel.showContours.collectAsState()
    val showLandmarks by viewModel.showLandmarks.collectAsState()
    val useFrontCamera by viewModel.useFrontCamera.collectAsState()

    val faceAnalyzer = remember { viewModel.createFaceAnalyzer() }
    val cameraSelector = if (useFrontCamera) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            imageAnalyzer = faceAnalyzer,
            cameraSelector = cameraSelector
        )

        // Face Mesh Overlay
        FaceMeshOverlay(
            modifier = Modifier.fillMaxSize(),
            analysisResult = analysisResult,
            showMesh = isMeshEnabled,
            showContours = showContours,
            showLandmarks = showLandmarks,
            imageWidth = 480,
            imageHeight = 640
        )

        // Top Bar
        TopBar(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            onSwitchCamera = { viewModel.toggleCamera() },
            useFrontCamera = useFrontCamera
        )

        // Control Panel
        ControlPanel(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            isMeshEnabled = isMeshEnabled,
            showContours = showContours,
            showLandmarks = showLandmarks,
            onToggleMesh = { viewModel.toggleMesh() },
            onToggleContours = { viewModel.toggleContours() },
            onToggleLandmarks = { viewModel.toggleLandmarks() }
        )

        // Emotion Indicator
        AnimatedVisibility(
            visible = analysisResult.faceDetected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 80.dp),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            EmotionIndicator(
                emotion = analysisResult.emotion,
                confidence = analysisResult.emotionConfidence
            )
        }

        // Analysis Card
        FaceAnalysisCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            analysisResult = analysisResult,
            expanded = true
        )
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onSwitchCamera: () -> Unit,
    useFrontCamera: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title
        Column {
            Text(
                text = "Face Analyzer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "AI-Powered Face Analysis",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Camera Switch Button
        FilledIconButton(
            onClick = onSwitchCamera,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.2f)
            )
        ) {
            Icon(
                imageVector = if (useFrontCamera) Icons.Filled.CameraFront else Icons.Filled.CameraRear,
                contentDescription = "Switch Camera",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier = Modifier,
    isMeshEnabled: Boolean,
    showContours: Boolean,
    showLandmarks: Boolean,
    onToggleMesh: () -> Unit,
    onToggleContours: () -> Unit,
    onToggleLandmarks: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControlButton(
            icon = Icons.Outlined.GridOn,
            label = "Mesh",
            isActive = isMeshEnabled,
            onClick = onToggleMesh
        )
        
        ControlButton(
            icon = Icons.Outlined.Timeline,
            label = "Contours",
            isActive = showContours,
            onClick = onToggleContours
        )
        
        ControlButton(
            icon = Icons.Outlined.LocationOn,
            label = "Points",
            isActive = showLandmarks,
            onClick = onToggleLandmarks
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) GradientStart else Color.Transparent,
        label = "controlBg"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = GradientStart
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Camera Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "To analyze your face, we need access to your camera. Your privacy is important to us - all processing happens on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GradientStart
            )
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grant Camera Access",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PrivacyTip,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Permission Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Face Analyzer needs camera permission to detect and analyze your face in real-time. Without this permission, the app cannot function.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Try Again",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
