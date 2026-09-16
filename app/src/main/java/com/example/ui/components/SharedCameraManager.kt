package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Shared camera manager that drives multiple active camera viewfinders simultaneously.
 * Camera hardware can only bind one camera stream per device; this manager multiplexes
 * the live stream so all available collage slots remain active and responsive concurrently.
 */
object SharedCameraManager {
    private const val TAG = "SharedCameraManager"

    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame: StateFlow<Bitmap?> = _currentFrame.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null

    private var boundLifecycleOwner: LifecycleOwner? = null
    private var appContext: Context? = null
    private var isStarting = false

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        appContext = context.applicationContext
        boundLifecycleOwner = lifecycleOwner

        if (camera != null && _isReady.value) {
            return
        }

        if (isStarting) return
        isStarting = true

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
                bindUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraProvider", e)
            } finally {
                isStarting = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera() {
        val currentLens = _lensFacing.value
        _lensFacing.value = if (currentLens == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindUseCases()
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        val lifecycleOwner = boundLifecycleOwner ?: return

        try {
            provider.unbindAll()

            if (cameraExecutor == null || cameraExecutor?.isShutdown == true) {
                cameraExecutor = Executors.newSingleThreadExecutor()
            }

            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(720, 720))
                .build()

            analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                try {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val bmp = imageProxy.toBitmap()
                    val finalBmp = if (rotationDegrees != 0) {
                        val matrix = android.graphics.Matrix().apply {
                            postRotate(rotationDegrees.toFloat())
                        }
                        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        if (rotated != bmp) {
                            bmp.recycle()
                        }
                        rotated
                    } else {
                        bmp
                    }
                    _currentFrame.value = finalBmp
                } catch (e: Exception) {
                    Log.e(TAG, "Frame analyzer error", e)
                } finally {
                    imageProxy.close()
                }
            }
            imageAnalysis = analysis

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(_lensFacing.value)
                .build()

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                analysis,
                capture
            )
            _isReady.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
            _isReady.value = false
        }
    }

    fun focus(normX: Float, normY: Float) {
        val cam = camera ?: return
        try {
            val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
            val point = factory.createPoint(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.d(TAG, "Autofocus failed: ${e.message}")
        }
    }

    fun takePicture(
        context: Context,
        slotIndex: Int,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError(IllegalStateException("Camera capture is not ready"))
            return
        }

        val photoFile = File(
            context.cacheDir,
            "quadsnap_cam${slotIndex + 1}_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSuccess(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            imageAnalysis = null
            imageCapture = null
            _isReady.value = false
            _currentFrame.value = null
            cameraExecutor?.shutdown()
            cameraExecutor = null
        } catch (e: Exception) {
            Log.e(TAG, "Stop camera error", e)
        }
    }
}
