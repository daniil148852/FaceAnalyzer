package com.faceanalyzer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceanalyzer.app.analyzer.FaceAnalysisResult
import com.faceanalyzer.app.analyzer.FaceAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceAnalyzerViewModel : ViewModel() {
    
    private val _analysisResult = MutableStateFlow(FaceAnalysisResult())
    val analysisResult: StateFlow<FaceAnalysisResult> = _analysisResult.asStateFlow()

    private val _isMeshEnabled = MutableStateFlow(false)
    val isMeshEnabled: StateFlow<Boolean> = _isMeshEnabled.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(true)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _showContours = MutableStateFlow(true)
    val showContours: StateFlow<Boolean> = _showContours.asStateFlow()

    private val _showLandmarks = MutableStateFlow(true)
    val showLandmarks: StateFlow<Boolean> = _showLandmarks.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(true)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private var faceAnalyzer: FaceAnalyzer? = null

    fun createFaceAnalyzer(): FaceAnalyzer {
        faceAnalyzer?.close()
        faceAnalyzer = FaceAnalyzer { result ->
            viewModelScope.launch {
                _analysisResult.value = result
            }
        }
        faceAnalyzer?.setMeshEnabled(_isMeshEnabled.value)
        return faceAnalyzer!!
    }

    fun toggleMesh() {
        _isMeshEnabled.value = !_isMeshEnabled.value
        faceAnalyzer?.setMeshEnabled(_isMeshEnabled.value)
    }

    fun toggleAnalyzing() {
        _isAnalyzing.value = !_isAnalyzing.value
    }

    fun toggleContours() {
        _showContours.value = !_showContours.value
    }

    fun toggleLandmarks() {
        _showLandmarks.value = !_showLandmarks.value
    }

    fun toggleCamera() {
        _useFrontCamera.value = !_useFrontCamera.value
    }

    override fun onCleared() {
        super.onCleared()
        faceAnalyzer?.close()
    }
}
