package com.example.domain.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Lightweight coroutine await extension for GMS Tasks
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

class SubjectSegmenterHelper(private val context: Context) {

    private val options = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap()
        .enableForegroundConfidenceMask()
        .build()

    private val segmenter = SubjectSegmentation.getClient(options)

    /**
     * Removes the background from the [inputBitmap] using ML Kit Subject Segmentation.
     * Returns a new transparent Bitmap containing only the subject.
     */
    suspend fun removeBackground(inputBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(inputBitmap, 0)
        try {
            val result = segmenter.process(image).await()
            val foreground = result.foregroundBitmap
            if (foreground != null) {
                foreground
            } else {
                // Fallback: If no subject detected, return a copy of the input
                inputBitmap.copy(inputBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback in case of error or missing play-services segmentation model
            inputBitmap.copy(inputBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Generates a colorful linear gradient background based on predefined indices.
     */
    fun getGradientShader(width: Float, height: Float, index: Int): Shader {
        val colors = when (index) {
            1 -> intArrayOf(0xFFE0F2F1.toInt(), 0xFFEDE7F6.toInt()) // Soft Mint & Lavender
            2 -> intArrayOf(0xFFFFF3E0.toInt(), 0xFFFFFDE7.toInt()) // Peach & Soft Gold
            3 -> intArrayOf(0xFFFCE4EC.toInt(), 0xFFFFF3E0.toInt()) // Pink Lemonade
            4 -> intArrayOf(0xFFE1F5FE.toInt(), 0xFFE0F2F1.toInt()) // Sky & Cool Teal
            5 -> intArrayOf(0xFFF3E5F5.toInt(), 0xFFFCE4EC.toInt()) // Sunset Lilac
            else -> intArrayOf(0xFFFFFFFF.toInt(), 0xFFF5F5F5.toInt()) // Crisp White & Light Gray
        }
        return LinearGradient(
            0f, 0f, width, height,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
    }

    /**
     * Draws [foregroundBitmap] on top of a newly generated gradient background of [index].
     */
    fun applyBackgroundReplacement(foregroundBitmap: Bitmap, index: Int): Bitmap {
        val width = foregroundBitmap.width
        val height = foregroundBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw Gradient Background
        val bgPaint = Paint().apply {
            shader = getGradientShader(width.toFloat(), height.toFloat(), index)
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw Foreground
        val fgPaint = Paint().apply {
            isAntiAlias = true
        }
        canvas.drawBitmap(foregroundBitmap, 0f, 0f, fgPaint)

        return output
    }

    /**
     * Draws [foregroundBitmap] on top of a custom background [customBgBitmap].
     * Performs "Center Crop" on the background bitmap to make sure it fills the canvas perfectly.
     */
    fun applyCustomBackgroundReplacement(foregroundBitmap: Bitmap, customBgBitmap: Bitmap): Bitmap {
        val width = foregroundBitmap.width
        val height = foregroundBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw Center-Cropped Custom Background Bitmap
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        val srcW = customBgBitmap.width
        val srcH = customBgBitmap.height
        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val destRatio = width.toFloat() / height.toFloat()

        val cropRect = if (srcRatio > destRatio) {
            val targetW = srcH * destRatio
            val offset = (srcW - targetW) / 2
            android.graphics.Rect(offset.toInt(), 0, (offset + targetW).toInt(), srcH)
        } else {
            val targetH = srcW / destRatio
            val offset = (srcH - targetH) / 2
            android.graphics.Rect(0, offset.toInt(), srcW, (offset + targetH).toInt())
        }

        val destRect = android.graphics.Rect(0, 0, width, height)
        canvas.drawBitmap(customBgBitmap, cropRect, destRect, paint)

        // Draw Foreground
        canvas.drawBitmap(foregroundBitmap, 0f, 0f, paint)

        return output
    }
}
