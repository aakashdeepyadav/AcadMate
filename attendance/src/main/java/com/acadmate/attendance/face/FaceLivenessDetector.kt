package com.acadmate.attendance.face

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.acadmate.attendance.data.FaceDetectionData
import com.acadmate.attendance.data.LivenessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class FaceLivenessDetector(context: Context) : ImageAnalysis.Analyzer {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    private val _faceDetectionFlow = MutableStateFlow<FaceDetectionData?>(null)
    val faceDetectionFlow: StateFlow<FaceDetectionData?> = _faceDetectionFlow

    private val _livenessResultFlow = MutableStateFlow<LivenessResult?>(null)
    val livenessResultFlow: StateFlow<LivenessResult?> = _livenessResultFlow

    private var frameCount = 0
    private var blinksDetected = 0
    private var eyesWereOpen = true

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        _faceDetectionFlow.value = FaceDetectionData(
                            eyesOpen = false,
                            faceCentered = false,
                            isLive = false
                        )
                    } else {
                        val face = faces[0]  // Assume single face
                        val detectionData = analyzeFace(face, image)
                        _faceDetectionFlow.value = detectionData

                        // Check liveness continuously
                        checkLiveness(detectionData)
                    }
                }
                .addOnFailureListener { e ->
                    _faceDetectionFlow.value = FaceDetectionData(
                        eyesOpen = false,
                        faceCentered = false,
                        isLive = false
                    )
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }

    private fun analyzeFace(face: Face, imageProxy: ImageProxy): FaceDetectionData {
        val imageWidth = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()

        // Check if face is centered (within center 60% of frame)
        val boundingBox = face.boundingBox
        val centerX = (boundingBox.left + boundingBox.right) / 2f
        val centerY = (boundingBox.top + boundingBox.bottom) / 2f

        val centerThreshold = 0.3f  // 30% tolerance from center
        val isCentered = abs(centerX - imageWidth / 2) < imageWidth * centerThreshold &&
                abs(centerY - imageHeight / 2) < imageHeight * centerThreshold

        // Check if eyes are open
        val leftEyeOpenProbability = face.leftEyeOpenProbability
        val rightEyeOpenProbability = face.rightEyeOpenProbability
        val eyesOpen = (leftEyeOpenProbability ?: 0f) > 0.5f && (rightEyeOpenProbability ?: 0f) > 0.5f

        // Track blinks for liveness detection
        if (eyesOpen != eyesWereOpen) {
            blinksDetected++
            eyesWereOpen = eyesOpen
        }

        // Head pose for anti-spoofing (no extreme angles)
        val headEulerZ = face.headEulerAngleZ
        val isReasonableHeadPose = abs(headEulerZ) < 45f  // Not turned too much

        frameCount++
        val blinkRate = (blinksDetected / frameCount.coerceAtLeast(1)).toFloat()

        return FaceDetectionData(
            eyesOpen = eyesOpen,
            faceCentered = isCentered,
            isLive = eyesOpen && isCentered && isReasonableHeadPose && blinkRate > 0.02f,
            headPoseZ = headEulerZ,
            blinkRate = blinkRate
        )
    }

    private fun checkLiveness(detectionData: FaceDetectionData) {
        // Collect data for ~2.5 seconds (75 frames at 30fps)
        if (frameCount > 75) {  
            val result = if (detectionData.faceCentered && abs(detectionData.headPoseZ) < 45) {
                // One full blink (close + open) results in blinksDetected >= 2
                if (blinksDetected >= 1) {
                    LivenessResult.Passed
                } else {
                    LivenessResult.Failed("Blink naturally to confirm you're present")
                }
            } else {
                when {
                    !detectionData.faceCentered -> LivenessResult.Failed("Center your face in the frame")
                    abs(detectionData.headPoseZ) > 45 -> LivenessResult.Failed("Keep your head straight")
                    else -> LivenessResult.Failed("No movement detected")
                }
            }
            _livenessResultFlow.value = result
        }
    }

    fun reset() {
        frameCount = 0
        blinksDetected = 0
        eyesWereOpen = true
        _livenessResultFlow.value = null
        _faceDetectionFlow.value = null
    }
}
