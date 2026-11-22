package com.example.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object VideoMetadataExtractor {

    suspend fun extract(context: Context, uri: Uri): Result<VideoMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                var displayName = "Selected Video.mp4"
                var fileSize = 0L

                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let {
                                displayName = it
                            }
                        }
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            fileSize = cursor.getLong(sizeIdx)
                        }
                    }
                }

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val durationMs = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.coerceAtLeast(1_000L)
                        ?: throw IllegalArgumentException("Unable to read video duration.")
                    val width = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull()
                        ?.coerceAtLeast(16)
                        ?: 1280
                    val height = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull()
                        ?.coerceAtLeast(16)
                        ?: 720
                    val rotation = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                        ?.toIntOrNull()
                        ?: 0

                    VideoMetadata(
                        uri = uri,
                        displayName = displayName,
                        durationMs = durationMs,
                        width = width,
                        height = height,
                        rotationDegrees = rotation,
                        fileSizeBytes = fileSize
                    )
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

    /**
     * Extracts a lightweight, softly blurred 9:16 backdrop bitmap from the video.
     * Uses fast downscale + multi-pass box blur so it is memory-safe and works on API 24-36.
     */
    suspend fun extractBlurredBackdrop(
        context: Context,
        uri: Uri,
        timeMs: Long,
        outWidth: Int = 360,
        outHeight: Int = 640
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val rawFrame = retriever.getFrameAtTime(
                timeMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(0L)
            rawFrame?.let { frame ->
                val result = buildBlurredVerticalBitmap(frame, outWidth, outHeight)
                if (frame != result && !frame.isRecycled) {
                    frame.recycle()
                }
                result
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Builds a 9:16 blurred backdrop with a transparent cutout matching the exact centered
     * fitted video rectangle, allowing Media3 OverlayEffect to render blurred top/bottom fill
     * bands without custom GLSL shaders.
     */
    suspend fun buildBlurLetterboxOverlay(
        context: Context,
        metadata: VideoMetadata,
        sampleTimeMs: Long,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val blurred = extractBlurredBackdrop(
            context = context,
            uri = metadata.uri,
            timeMs = sampleTimeMs,
            outWidth = targetWidth,
            outHeight = targetHeight
        ) ?: return@withContext null

        val srcW = metadata.effectiveWidth.coerceAtLeast(1).toFloat()
        val srcH = metadata.effectiveHeight.coerceAtLeast(1).toFloat()
        val srcAspect = srcW / srcH
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()

        // If source is already as tall or taller than 9:16, no top/bottom letterbox exists
        if (srcAspect <= targetAspect * 1.03f) {
            blurred.recycle()
            return@withContext null
        }

        val fittedHeight = (targetWidth / srcAspect).coerceAtMost(targetHeight.toFloat())
        val top = ((targetHeight - fittedHeight) / 2f).coerceAtLeast(0f)
        val bottom = (top + fittedHeight).coerceAtMost(targetHeight.toFloat())

        val canvas = Canvas(blurred)
        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        // Punch clean transparent window for the fitted center video (2px inset prevents edge halo)
        canvas.drawRect(
            0f,
            (top + 2f).coerceAtLeast(0f),
            targetWidth.toFloat(),
            (bottom - 2f).coerceAtMost(targetHeight.toFloat()),
            clearPaint
        )

        // Subtle dark studio divider lines at top and bottom of the center frame
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 0, 0, 0)
            strokeWidth = 3f
        }
        canvas.drawLine(0f, top, targetWidth.toFloat(), top, edgePaint)
        canvas.drawLine(0f, bottom, targetWidth.toFloat(), bottom, edgePaint)

        blurred
    }

    private fun buildBlurredVerticalBitmap(
        source: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // Center-crop source to 9:16 at tiny resolution (45x80) for super-fast low-RAM blur
        val smallW = 45
        val smallH = 80
        val small = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
        val smallCanvas = Canvas(small)

        val srcAspect = source.width.toFloat() / source.height.coerceAtLeast(1).toFloat()
        val dstAspect = smallW.toFloat() / smallH.toFloat()

        val srcRect = if (srcAspect > dstAspect) {
            val cropW = (source.height * dstAspect).toInt().coerceAtLeast(1)
            val left = ((source.width - cropW) / 2).coerceAtLeast(0)
            Rect(left, 0, left + cropW, source.height)
        } else {
            val cropH = (source.width / dstAspect).toInt().coerceAtLeast(1)
            val top = ((source.height - cropH) / 2).coerceAtLeast(0)
            Rect(0, top, source.width, top + cropH)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        smallCanvas.drawBitmap(source, srcRect, RectF(0f, 0f, smallW.toFloat(), smallH.toFloat()), paint)

        // 3-pass separable box blur at tiny resolution produces ultra-smooth Gaussian-like bokeh
        val blurredSmall = boxBlur(small, radius = 4, passes = 3)
        small.recycle()

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(output)
        outCanvas.drawBitmap(
            blurredSmall,
            null,
            RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        blurredSmall.recycle()

        // Rich obsidian tint so captions & center video pop cleanly
        outCanvas.drawColor(Color.argb(92, 9, 9, 15))
        return output
    }

    private fun boxBlur(input: Bitmap, radius: Int, passes: Int): Bitmap {
        val w = input.width
        val h = input.height
        var src = IntArray(w * h)
        var dst = IntArray(w * h)
        input.getPixels(src, 0, w, 0, 0, w, h)

        repeat(passes) {
            // Horizontal pass
            for (y in 0 until h) {
                val rowOffset = y * w
                for (x in 0 until w) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    val xStart = max(0, x - radius)
                    val xEnd = min(w - 1, x + radius)
                    for (ix in xStart..xEnd) {
                        val px = src[rowOffset + ix]
                        r += (px shr 16) and 0xFF
                        g += (px shr 8) and 0xFF
                        b += px and 0xFF
                        count++
                    }
                    dst[rowOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                }
            }
            // Vertical pass
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    val yStart = max(0, y - radius)
                    val yEnd = min(h - 1, y + radius)
                    for (iy in yStart..yEnd) {
                        val px = dst[iy * w + x]
                        r += (px shr 16) and 0xFF
                        g += (px shr 8) and 0xFF
                        b += px and 0xFF
                        count++
                    }
                    src[y * w + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                }
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(src, 0, w, 0, 0, w, h)
        }
    }
}
