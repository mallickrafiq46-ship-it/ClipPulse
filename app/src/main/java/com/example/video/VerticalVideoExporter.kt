package com.example.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.CaptionConfig
import com.example.model.CropMode
import com.example.model.VideoMetadata
import com.example.model.ViralClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "VerticalVideoExporter"
private const val EXPORT_WIDTH = 720
private const val EXPORT_HEIGHT = 1280

/**
 * Hardware-accelerated on-device vertical 9:16 MP4 exporter built on Jetpack Media3 Transformer.
 * Supports clipping exact viral windows, 9:16 Center Crop or Blur Fill, burned-in dynamic
 * word-by-word captions, and saving directly to the user's Movies/ClipPulse gallery folder.
 */
@OptIn(UnstableApi::class)
class VerticalVideoExporter(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null
    private var tempOutputFile: File? = null

    fun exportClip(
        metadata: VideoMetadata,
        clip: ViralClip,
        cropMode: CropMode,
        captionConfig: CaptionConfig,
        onProgress: (percent: Int, stageLabel: String) -> Unit,
        onCompleted: (savedUri: Uri, fileName: String, fileSizeBytes: Long) -> Unit,
        onError: (message: String) -> Unit
    ) {
        cancel()

        scope.launch {
            try {
                onProgress(5, "Preparing 9:16 vertical pipeline...")

                // Prepare optional blurred letterbox overlay for BLUR_FILL mode
                val blurBackdropBitmap: Bitmap? = if (cropMode == CropMode.BLUR_FILL) {
                    val midMs = (clip.startMs + clip.endMs) / 2L
                    VideoMetadataExtractor.buildBlurLetterboxOverlay(
                        context = context,
                        metadata = metadata,
                        sampleTimeMs = midMs,
                        targetWidth = EXPORT_WIDTH,
                        targetHeight = EXPORT_HEIGHT
                    )
                } else {
                    null
                }

                val captionWords = clip.captionsFor(captionConfig.language)
                val captionChunks = CaptionTimelineHelper.buildChunks(captionWords)
                val captionRenderer = CaptionBitmapRenderer(context, EXPORT_WIDTH, EXPORT_HEIGHT)

                val overlays = mutableListOf<TextureOverlay>()

                if (blurBackdropBitmap != null) {
                    overlays.add(StaticBackdropOverlay(blurBackdropBitmap))
                }

                if (captionConfig.enabled && captionChunks.isNotEmpty()) {
                    overlays.add(
                        DynamicCaptionOverlay(
                            clipStartMs = clip.startMs,
                            chunks = captionChunks,
                            config = captionConfig,
                            renderer = captionRenderer
                        )
                    )
                }

                val layoutMode = if (cropMode == CropMode.CENTER_CROP) {
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                } else {
                    Presentation.LAYOUT_SCALE_TO_FIT
                }

                val videoEffects = mutableListOf<Effect>(
                    Presentation.createForWidthAndHeight(
                        EXPORT_WIDTH,
                        EXPORT_HEIGHT,
                        layoutMode
                    )
                )
                if (overlays.isNotEmpty()) {
                    videoEffects.add(OverlayEffect(overlays))
                }

                val mediaItem = MediaItem.Builder()
                    .setUri(metadata.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.startMs.coerceAtLeast(0L))
                            .setEndPositionMs(clip.endMs.coerceAtLeast(clip.startMs + 1_000L))
                            .build()
                    )
                    .build()

                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val tempDir = File(context.cacheDir, "clippulse_exports").apply { mkdirs() }
                val safeTitle = clip.title
                    .replace(Regex("[^a-zA-Z0-9]+"), "_")
                    .trim('_')
                    .take(24)
                    .ifBlank { "ViralClip" }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "ClipPulse_${safeTitle}_$stamp.mp4"
                val outFile = File(tempDir, fileName)
                if (outFile.exists()) outFile.delete()
                tempOutputFile = outFile

                withContext(Dispatchers.Main) {
                    onProgress(12, "Encoding 720x1280 MP4...")
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(
                            object : Transformer.Listener {
                                override fun onCompleted(
                                    composition: Composition,
                                    exportResult: ExportResult
                                ) {
                                    stopProgressPolling()
                                    activeTransformer = null
                                    scope.launch {
                                        onProgress(96, "Saving to Movies/ClipPulse...")
                                        saveToMediaStore(outFile, fileName)
                                            .onSuccess { (savedUri, sizeBytes) ->
                                                outFile.delete()
                                                onProgress(100, "Export complete")
                                                onCompleted(savedUri, fileName, sizeBytes)
                                            }
                                            .onFailure { err ->
                                                Log.e(TAG, "MediaStore save failed", err)
                                                onError(
                                                    err.localizedMessage
                                                        ?: "Failed to save exported clip to gallery."
                                                )
                                            }
                                    }
                                }

                                override fun onError(
                                    composition: Composition,
                                    exportResult: ExportResult,
                                    exportException: ExportException
                                ) {
                                    stopProgressPolling()
                                    activeTransformer = null
                                    outFile.delete()
                                    Log.e(TAG, "Media3 Transformer export failed", exportException)
                                    onError(
                                        exportException.localizedMessage
                                            ?: "Video export failed on this device codec."
                                    )
                                }
                            }
                        )
                        .build()

                    activeTransformer = transformer
                    transformer.start(editedMediaItem, outFile.absolutePath)
                    startProgressPolling(transformer, onProgress)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Export setup error", e)
                onError(e.localizedMessage ?: "Could not start video export.")
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            stopProgressPolling()
            runCatching { activeTransformer?.cancel() }
            activeTransformer = null
            runCatching { tempOutputFile?.delete() }
            tempOutputFile = null
        }
    }

    private fun startProgressPolling(
        transformer: Transformer,
        onProgress: (Int, String) -> Unit
    ) {
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                if (activeTransformer !== transformer) return
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    // Map transformer 0..100 into UI 15..95
                    val mapped = (15 + (holder.progress * 0.80f).toInt()).coerceIn(15, 95)
                    val label = when {
                        mapped < 45 -> "Cropping 9:16 frames..."
                        mapped < 80 -> "Burning kinetic captions..."
                        else -> "Muxing H.264 MP4 stream..."
                    }
                    onProgress(mapped, label)
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 180L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 180L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private suspend fun saveToMediaStore(
        sourceFile: File,
        displayName: String
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        runCatching {
            val sizeBytes = sourceFile.length()
            val resolver = context.contentResolver

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/ClipPulse"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val targetUri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Could not create MediaStore video entry.")

            resolver.openOutputStream(targetUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            } ?: throw IllegalStateException("Could not open MediaStore output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(targetUri, completeValues, null, null)
            }

            targetUri to sizeBytes
        }
    }

    private class StaticBackdropOverlay(
        private val backdrop: Bitmap
    ) : BitmapOverlay() {
        override fun getBitmap(presentationTimeUs: Long): Bitmap = backdrop
    }

    private class DynamicCaptionOverlay(
        private val clipStartMs: Long,
        private val chunks: List<CaptionChunk>,
        private val config: CaptionConfig,
        private val renderer: CaptionBitmapRenderer
    ) : BitmapOverlay() {

        private var lastKey: Long = Long.MIN_VALUE
        private var cachedBitmap: Bitmap =
            Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val absoluteMs = clipStartMs + (presentationTimeUs / 1000L)
            val activeFrame = CaptionTimelineHelper.findActiveFrame(chunks, absoluteMs)
            val frameKey = if (activeFrame == null) {
                -1L
            } else {
                (activeFrame.chunkIndex.toLong() shl 16) or activeFrame.activeWordInChunk.toLong()
            }

            if (frameKey != lastKey) {
                val nextBitmap = renderer.renderFrame(activeFrame, config)
                lastKey = frameKey
                cachedBitmap = nextBitmap
            }
            return cachedBitmap
        }
    }
}
