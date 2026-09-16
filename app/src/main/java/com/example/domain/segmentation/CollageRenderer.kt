package com.example.domain.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.BlendMode
import com.example.data.model.CollageLayout
import com.example.data.model.CollageState
import com.example.data.model.SlotState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object CollageRenderer {

    /**
     * Helper to load a Uri into a Bitmap safely while honoring EXIF orientation
     * so that photos keep their original direction and are not rotated.
     */
    fun loadUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Read EXIF orientation to correct any unintended rotation
            var rotationDegrees = 0
            var flipHorizontal = false
            try {
                contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipHorizontal = true
                        ExifInterface.ORIENTATION_TRANSVERSE -> {
                            rotationDegrees = 270
                            flipHorizontal = true
                        }
                        ExifInterface.ORIENTATION_TRANSPOSE -> {
                            rotationDegrees = 90
                            flipHorizontal = true
                        }
                    }
                }
            } catch (exifErr: Exception) {
                exifErr.printStackTrace()
            }

            if (rotationDegrees != 0 || flipHorizontal) {
                val matrix = Matrix()
                if (rotationDegrees != 0) {
                    matrix.postRotate(rotationDegrees.toFloat())
                }
                if (flipHorizontal) {
                    matrix.postScale(-1f, 1f)
                }
                val transformedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (transformedBitmap != bitmap) {
                    bitmap.recycle()
                }
                transformedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Renders the captured photos into a single unified Bitmap of 1080x1080 resolution.
     * When compiled, professionally blends all captured pictures with seamless seam cross-dissolves,
     * tonal exposure harmonization, and cohesive photographic styling.
     */
    suspend fun renderCollage(
        context: Context,
        state: CollageState,
        fallbackPlaceholder: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val size = 1080

        // Get all captured photos
        val capturedPhotos = state.slots.filterIsInstance<SlotState.Photo>()
        val photoCount = capturedPhotos.size

        // Select the layout: if user-selected layout matches photoCount, preserve it directly!
        val effectiveLayout = if (state.layout.slotCount == photoCount) {
            state.layout
        } else {
            when (photoCount) {
                2 -> {
                    if (state.layout == CollageLayout.TWO_SPLIT_HORIZONTAL ||
                        state.layout == CollageLayout.THREE_ROWS ||
                        state.layout == CollageLayout.FOUR_ROWS
                    ) {
                        CollageLayout.TWO_SPLIT_HORIZONTAL
                    } else {
                        CollageLayout.TWO_SPLIT_VERTICAL
                    }
                }
                3 -> {
                    when (state.layout) {
                        CollageLayout.THREE_ROWS, CollageLayout.FOUR_ROWS -> CollageLayout.THREE_ROWS
                        CollageLayout.THREE_COLUMNS, CollageLayout.FOUR_COLUMNS -> CollageLayout.THREE_COLUMNS
                        else -> CollageLayout.THREE_GRID
                    }
                }
                4 -> {
                    if (state.layout.slotCount == 4) state.layout else CollageLayout.FOUR_GRID
                }
                else -> state.layout
            }
        }

        val rectangles = getLayoutRects(effectiveLayout, size)

        if (state.blendMode == BlendMode.CLEAN_GRID || state.blendFeatherPx <= 0) {
            renderCleanGridCollage(context, state, capturedPhotos, rectangles, size)
        } else {
            renderProfessionallyBlendedCollage(context, state, capturedPhotos, rectangles, size)
        }
    }

    private fun renderCleanGridCollage(
        context: Context,
        state: CollageState,
        capturedPhotos: List<SlotState.Photo>,
        rectangles: List<Rect>,
        size: Int
    ): Bitmap {
        val collageBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collageBitmap)

        if (state.isCircularStyle) {
            val colors = getGradientColors(state.selectedGradientIndex)
            val shader = android.graphics.LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                colors[0], colors[1],
                android.graphics.Shader.TileMode.CLAMP
            )
            val bgPaint = Paint().apply {
                this.shader = shader
                this.isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
        }

        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        rectangles.forEachIndexed { index, rect ->
            val photoSlot = (state.slots.getOrNull(index) as? SlotState.Photo) ?: capturedPhotos.getOrNull(index)
            if (photoSlot != null) {
                val loadedBitmap = loadUriToBitmap(context, photoSlot.uri)
                if (loadedBitmap != null) {
                    if (state.isCircularStyle) {
                        canvas.save()
                        val path = android.graphics.Path().apply {
                            addOval(RectF(rect), android.graphics.Path.Direction.CW)
                        }
                        canvas.clipPath(path)
                        drawCroppedBitmapIntoRect(canvas, loadedBitmap, rect, paint)
                        canvas.restore()
                    } else {
                        drawCroppedBitmapIntoRect(canvas, loadedBitmap, rect, paint)
                    }
                }
            }
        }
        return collageBitmap
    }

    /**
     * Professionally blends all captured photos into a seamless photographic montage.
     * Features:
     * - Multi-photo tonal harmonization to eliminate exposure mismatch across captured pictures
     * - Seam feathering with separable smoothstep distance weight computation
     * - Normalized weighted pixel accumulation ensuring zero dark lines, zero gaps, and smooth transitions
     * - Cohesive photographic finishing (Studio Harmony, Dreamy Glow, Editorial Film, Soft Dissolve)
     */
    private fun renderProfessionallyBlendedCollage(
        context: Context,
        state: CollageState,
        capturedPhotos: List<SlotState.Photo>,
        rectangles: List<Rect>,
        size: Int
    ): Bitmap {
        val slotCount = rectangles.size
        val loadedBitmaps = mutableListOf<Bitmap?>()
        for (index in 0 until slotCount) {
            val photoSlot = (state.slots.getOrNull(index) as? SlotState.Photo) ?: capturedPhotos.getOrNull(index)
            if (photoSlot != null) {
                loadedBitmaps.add(loadUriToBitmap(context, photoSlot.uri))
            } else {
                loadedBitmaps.add(null)
            }
        }

        // 1. Multi-photo tonal analysis & exposure harmonization
        val exposureMultipliers = FloatArray(slotCount) { 1.0f }
        if (state.blendMode == BlendMode.STUDIO_HARMONY || state.blendMode == BlendMode.EDITORIAL_FILM) {
            val luminances = mutableListOf<Float>()
            loadedBitmaps.forEach { bmp ->
                if (bmp != null) {
                    luminances.add(calculateAverageLuminance(bmp))
                }
            }
            if (luminances.isNotEmpty()) {
                val avgLum = luminances.average().toFloat()
                loadedBitmaps.forEachIndexed { idx, bmp ->
                    if (bmp != null && luminances.size > idx) {
                        val slotLum = luminances[idx]
                        if (slotLum > 10f) {
                            val diff = (avgLum - slotLum) / 255f
                            exposureMultipliers[idx] = (1.0f + diff * 0.45f).coerceIn(0.75f, 1.30f)
                        }
                    }
                }
            }
        }

        // 2. Determine feather distance in pixels
        val baseFeather = state.blendFeatherPx.coerceIn(16, 120)
        val feather = when (state.blendMode) {
            BlendMode.DREAMY_VIGNETTE -> (baseFeather * 1.35f).toInt()
            BlendMode.SEAMLESS_FEATHER -> (baseFeather * 1.15f).toInt()
            BlendMode.EDITORIAL_FILM -> (baseFeather * 0.90f).toInt()
            else -> baseFeather
        }

        // 3. Accumulation buffers for 1080x1080 canvas
        val rAcc = FloatArray(size * size)
        val gAcc = FloatArray(size * size)
        val bAcc = FloatArray(size * size)
        val wAcc = FloatArray(size * size)

        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }

        // 4. Process each slot with extended bounds and smoothstep blending
        rectangles.forEachIndexed { index, nominalRect ->
            val srcBmp = loadedBitmaps.getOrNull(index) ?: return@forEachIndexed

            val hasLeft = nominalRect.left > 0
            val hasTop = nominalRect.top > 0
            val hasRight = nominalRect.right < size
            val hasBottom = nominalRect.bottom < size

            val extLeft = if (hasLeft) (nominalRect.left - feather).coerceAtLeast(0) else nominalRect.left
            val extTop = if (hasTop) (nominalRect.top - feather).coerceAtLeast(0) else nominalRect.top
            val extRight = if (hasRight) (nominalRect.right + feather).coerceAtMost(size) else nominalRect.right
            val extBottom = if (hasBottom) (nominalRect.bottom + feather).coerceAtMost(size) else nominalRect.bottom

            val extW = extRight - extLeft
            val extH = extBottom - extTop
            if (extW <= 0 || extH <= 0) return@forEachIndexed

            // Center-crop into extended rect
            val slotBmp = Bitmap.createBitmap(extW, extH, Bitmap.Config.ARGB_8888)
            val slotCanvas = Canvas(slotBmp)
            drawCroppedBitmapIntoRect(slotCanvas, srcBmp, Rect(0, 0, extW, extH), paint)

            val slotPixels = IntArray(extW * extH)
            slotBmp.getPixels(slotPixels, 0, extW, 0, 0, extW, extH)
            slotBmp.recycle()

            // Precompute 1D separable weights along X and Y
            val transitionSpan = (2 * feather).toFloat().coerceAtLeast(1f)
            val wxArray = FloatArray(extW)
            for (x in 0 until extW) {
                val gx = extLeft + x
                val tLeft = if (hasLeft) ((gx - (nominalRect.left - feather)).toFloat() / transitionSpan).coerceIn(0f, 1f) else 1f
                val tRight = if (hasRight) (((nominalRect.right + feather) - gx).toFloat() / transitionSpan).coerceIn(0f, 1f) else 1f
                wxArray[x] = smoothStep(tLeft) * smoothStep(tRight)
            }

            val wyArray = FloatArray(extH)
            for (y in 0 until extH) {
                val gy = extTop + y
                val tTop = if (hasTop) ((gy - (nominalRect.top - feather)).toFloat() / transitionSpan).coerceIn(0f, 1f) else 1f
                val tBottom = if (hasBottom) (((nominalRect.bottom + feather) - gy).toFloat() / transitionSpan).coerceIn(0f, 1f) else 1f
                wyArray[y] = smoothStep(tTop) * smoothStep(tBottom)
            }

            val exposure = exposureMultipliers.getOrElse(index) { 1.0f }

            // Accumulate weighted contributions
            for (y in 0 until extH) {
                val gy = extTop + y
                val wy = wyArray[y]
                val slotRowOffset = y * extW
                val canvasRowOffset = gy * size

                for (x in 0 until extW) {
                    val gx = extLeft + x
                    val w = if (state.isCircularStyle) {
                        val cx = nominalRect.centerX().toFloat()
                        val cy = nominalRect.centerY().toFloat()
                        val rx = nominalRect.width() / 2f
                        val ry = nominalRect.height() / 2f
                        
                        val dx = (gx - cx) / rx
                        val dy = (gy - cy) / ry
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        
                        val avgR = (rx + ry) / 2f
                        val featherRatio = (feather.toFloat() / avgR).coerceIn(0.1f, 0.45f)
                        
                        if (dist <= 1.0f - featherRatio) {
                            1.0f
                        } else if (dist >= 1.0f + featherRatio) {
                            0.0f
                        } else {
                            val t = ((1.0f + featherRatio) - dist) / (2f * featherRatio)
                            smoothStep(t)
                        }
                    } else {
                        wxArray[x] * wy
                    }

                    if (w <= 0.0001f) continue
                    val canvasIdx = canvasRowOffset + gx
                    val pixel = slotPixels[slotRowOffset + x]

                    val r = (((pixel shr 16) and 0xFF) * exposure).coerceIn(0f, 255f)
                    val g = (((pixel shr 8) and 0xFF) * exposure).coerceIn(0f, 255f)
                    val b = ((pixel and 0xFF) * exposure).coerceIn(0f, 255f)

                    rAcc[canvasIdx] += r * w
                    gAcc[canvasIdx] += g * w
                    bAcc[canvasIdx] += b * w
                    wAcc[canvasIdx] += w
                }
            }
        }

        // 5. Finalize colors, normalize weights, and apply tone curve / finishing grade
        val outputPixels = IntArray(size * size)
        val isDreamy = state.blendMode == BlendMode.DREAMY_VIGNETTE
        val isHarmony = state.blendMode == BlendMode.STUDIO_HARMONY
        val isEditorial = state.blendMode == BlendMode.EDITORIAL_FILM

        val halfSize = size / 2f
        val maxDistSq = halfSize * halfSize * 2f

        val bgColors = getGradientColors(state.selectedGradientIndex)

        for (y in 0 until size) {
            val rowOffset = y * size
            val dy = y - halfSize

            for (x in 0 until size) {
                val idx = rowOffset + x
                val totalW = wAcc[idx]

                var r = rAcc[idx]
                var g = gAcc[idx]
                var b = bAcc[idx]

                if (state.isCircularStyle) {
                    if (totalW < 1.0f) {
                        val bgColor = getGradientColorAt(x, y, size, bgColors)
                        val bgR = (bgColor shr 16) and 0xFF
                        val bgG = (bgColor shr 8) and 0xFF
                        val bgB = bgColor and 0xFF

                        val remainingW = 1.0f - totalW
                        r += bgR * remainingW
                        g += bgG * remainingW
                        b += bgB * remainingW
                    } else {
                        val invW = 1f / totalW
                        r *= invW
                        g *= invW
                        b *= invW
                    }
                } else {
                    val invW = if (totalW > 0.0001f) 1f / totalW else 1f
                    r *= invW
                    g *= invW
                    b *= invW
                }

                // Dreamy vignette falloff
                if (isDreamy) {
                    val dx = x - halfSize
                    val distSq = dx * dx + dy * dy
                    val vig = (1.0f - 0.22f * (distSq / maxDistSq)).coerceIn(0.75f, 1.0f)
                    r *= vig
                    g *= vig
                    b *= vig
                }

                // Studio harmony: Warmth in highlights, depth in shadows, subtle S-curve
                if (isHarmony) {
                    r = applyStudioTone(r, 1.03f)
                    g = applyStudioTone(g, 1.01f)
                    b = applyStudioTone(b, 0.98f)
                }

                // Editorial film: Rich contrast, cinematic warmth
                if (isEditorial) {
                    r = applyEditorialTone(r, 1.05f)
                    g = applyEditorialTone(g, 1.00f)
                    b = applyEditorialTone(b, 0.95f)
                }

                val ir = r.toInt().coerceIn(0, 255)
                val ig = g.toInt().coerceIn(0, 255)
                val ib = b.toInt().coerceIn(0, 255)

                outputPixels[idx] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }

        val resultBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, size, 0, 0, size, size)
        return resultBitmap
    }

    private fun smoothStep(t: Float): Float {
        val ct = t.coerceIn(0f, 1f)
        return ct * ct * (3f - 2f * ct)
    }

    private fun calculateAverageLuminance(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val stepX = (w / 32).coerceAtLeast(1)
        val stepY = (h / 32).coerceAtLeast(1)
        var totalLum = 0.0
        var count = 0
        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalLum += (0.299 * r + 0.587 * g + 0.114 * b)
                count++
            }
        }
        return if (count > 0) (totalLum / count).toFloat() else 128f
    }

    private fun applyStudioTone(value: Float, channelScale: Float): Float {
        val norm = (value / 255f).coerceIn(0f, 1f)
        val sCurved = norm * norm * (3f - 2f * norm)
        val blended = norm * 0.4f + sCurved * 0.6f
        return (blended * 255f * channelScale).coerceIn(0f, 255f)
    }

    private fun applyEditorialTone(value: Float, channelScale: Float): Float {
        val norm = (value / 255f).coerceIn(0f, 1f)
        val sCurved = norm * norm * (3f - 2f * norm)
        val filmic = norm * 0.25f + sCurved * 0.75f
        return (filmic * 255f * channelScale).coerceIn(0f, 255f)
    }

    private fun getLayoutRects(layout: CollageLayout, size: Int): List<Rect> {
        val half = size / 2
        val third = size / 3
        val fourth = size / 4
        return when (layout) {
            CollageLayout.TWO_SPLIT_VERTICAL -> listOf(
                Rect(0, 0, half, size),
                Rect(half, 0, size, size)
            )
            CollageLayout.TWO_SPLIT_HORIZONTAL -> listOf(
                Rect(0, 0, size, half),
                Rect(0, half, size, size)
            )
            CollageLayout.THREE_GRID -> listOf(
                Rect(0, 0, half, size),
                Rect(half, 0, size, half),
                Rect(half, half, size, size)
            )
            CollageLayout.THREE_ROWS -> listOf(
                Rect(0, 0, size, third),
                Rect(0, third, size, third * 2),
                Rect(0, third * 2, size, size)
            )
            CollageLayout.THREE_COLUMNS -> listOf(
                Rect(0, 0, third, size),
                Rect(third, 0, third * 2, size),
                Rect(third * 2, 0, size, size)
            )
            CollageLayout.FOUR_GRID -> listOf(
                Rect(0, 0, half, half),
                Rect(half, 0, size, half),
                Rect(0, half, half, size),
                Rect(half, half, size, size)
            )
            CollageLayout.FOUR_ROWS -> listOf(
                Rect(0, 0, size, fourth),
                Rect(0, fourth, size, fourth * 2),
                Rect(0, fourth * 2, size, fourth * 3),
                Rect(0, fourth * 3, size, size)
            )
            CollageLayout.FOUR_COLUMNS -> listOf(
                Rect(0, 0, fourth, size),
                Rect(fourth, 0, fourth * 2, size),
                Rect(fourth * 2, 0, fourth * 3, size),
                Rect(fourth * 3, 0, size, size)
            )
            CollageLayout.FOUR_FEATURED -> listOf(
                Rect(0, 0, half, size),
                Rect(half, 0, size, third),
                Rect(half, third, size, third * 2),
                Rect(half, third * 2, size, size)
            )
        }
    }

    /**
     * Draws a bitmap into a target rectangle, performing "Center Crop" scaling.
     */
    private fun drawCroppedBitmapIntoRect(canvas: Canvas, src: Bitmap, dest: Rect, paint: Paint) {
        val srcW = src.width
        val srcH = src.height
        val destW = dest.width()
        val destH = dest.height()

        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val destRatio = destW.toFloat() / destH.toFloat()

        val cropRect = if (srcRatio > destRatio) {
            // Source is wider, crop horizontally
            val targetW = srcH * destRatio
            val offset = (srcW - targetW) / 2
            Rect(offset.toInt(), 0, (offset + targetW).toInt(), srcH)
        } else {
            // Source is taller, crop vertically
            val targetH = srcW / destRatio
            val offset = (srcH - targetH) / 2
            Rect(0, offset.toInt(), srcW, (offset + targetH).toInt())
        }

        canvas.drawBitmap(src, cropRect, dest, paint)
    }

    private fun drawPlaceholderInRect(canvas: Canvas, rect: Rect, index: Int, paint: Paint) {
        // Soft colorful pastel background placeholders
        val colors = listOf(
            0xFFFCE4EC.toInt(), // Soft pink
            0xFFE0F2F1.toInt(), // Soft mint
            0xFFFFF3E0.toInt(), // Soft peach
            0xFFEDE7F6.toInt()  // Soft lavender
        )
        val color = colors[index % colors.size]
        paint.color = color
        canvas.drawRect(rect, paint)

        // Draw a light decorative "+" sign in the center of empty slot
        paint.color = 0x88FFFFFF.toInt()
        val cx = rect.centerX().toFloat()
        val cy = rect.centerY().toFloat()
        val length = 32f
        val stroke = 8f
        canvas.drawRect(cx - length, cy - stroke/2, cx + length, cy + stroke/2, paint)
        canvas.drawRect(cx - stroke/2, cy - length, cx + stroke/2, cy + length, paint)
    }

    private fun getGradientColors(index: Int): IntArray {
        return when (index) {
            0 -> intArrayOf(0xFFECEFF1.toInt(), 0xFFCFD8DC.toInt()) // None
            1 -> intArrayOf(0xFFE0F2F1.toInt(), 0xFFEDE7F6.toInt()) // Mint
            2 -> intArrayOf(0xFFFFF3E0.toInt(), 0xFFFFFDE7.toInt()) // Peach
            3 -> intArrayOf(0xFFFCE4EC.toInt(), 0xFFFFF3E0.toInt()) // Pink
            4 -> intArrayOf(0xFFE1F5FE.toInt(), 0xFFE0F2F1.toInt()) // Sky
            5 -> intArrayOf(0xFFF3E5F5.toInt(), 0xFFFCE4EC.toInt()) // Sunset
            else -> intArrayOf(0xFFECEFF1.toInt(), 0xFFCFD8DC.toInt())
        }
    }

    private fun getGradientColorAt(x: Int, y: Int, size: Int, colors: IntArray): Int {
        if (colors.size < 2) return colors.getOrElse(0) { 0xFFFFFFFF.toInt() }
        val ratio = (x + y).toFloat() / (2f * size)
        val r = ratio.coerceIn(0f, 1f)
        
        val startColor = colors[0]
        val endColor = colors[1]
        
        val startA = (startColor shr 24) and 0xFF
        val startR = (startColor shr 16) and 0xFF
        val startG = (startColor shr 8) and 0xFF
        val startB = startColor and 0xFF
        
        val endA = (endColor shr 24) and 0xFF
        val endR = (endColor shr 16) and 0xFF
        val endG = (endColor shr 8) and 0xFF
        val endB = endColor and 0xFF
        
        val a = (startA + (endA - startA) * r).toInt().coerceIn(0, 255)
        val red = (startR + (endR - startR) * r).toInt().coerceIn(0, 255)
        val green = (startG + (endG - startG) * r).toInt().coerceIn(0, 255)
        val blue = (startB + (endB - startB) * r).toInt().coerceIn(0, 255)
        
        return (a shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
