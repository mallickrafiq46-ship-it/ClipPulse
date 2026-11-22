package com.example.model

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val fileSizeBytes: Long
) {
    val effectiveWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    val effectiveHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height

    val exceedsMaxRecommendedDuration: Boolean
        get() = durationMs > MAX_RECOMMENDED_DURATION_MS

    val usableDurationMs: Long
        get() = durationMs.coerceAtMost(MAX_RECOMMENDED_DURATION_MS).coerceAtLeast(1_000L)

    companion object {
        const val MAX_RECOMMENDED_DURATION_MS = 15L * 60L * 1000L
    }
}

data class WordCaption(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

enum class CaptionLanguage(val label: String, val badge: String) {
    HINGLISH("Hinglish", "HI-EN"),
    HINDI("हिन्दी", "HI"),
    ENGLISH("English", "EN")
}

enum class CaptionStylePreset(val label: String, val subtitle: String) {
    HORMOZI_POP("Hormozi Pop", "Bold lime active word"),
    NEON_PULSE("Neon Pulse", "Cyan glowing pill"),
    STUDIO_MINIMAL("Studio Clean", "High-contrast white")
}

enum class CaptionPosition(val label: String, val verticalBias: Float) {
    BOTTOM("Lower Third", 0.78f),
    CENTER("Center", 0.52f),
    TOP("Upper Safe", 0.22f)
}

enum class CropMode(val label: String, val description: String) {
    CENTER_CROP("9:16 Center Crop", "Full-bleed vertical fill"),
    BLUR_FILL("9:16 Blur Fill", "Full frame + blurred backdrop")
}

data class ViralClip(
    val id: String,
    val rank: Int,
    val title: String,
    val hookReason: String,
    val startMs: Long,
    val endMs: Long,
    val viralityScore: Int,
    val hookScore: Int,
    val pacingScore: Int,
    val clarityScore: Int,
    val hashtags: List<String>,
    val captionsByLanguage: Map<CaptionLanguage, List<WordCaption>>
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(1_000L)

    fun captionsFor(language: CaptionLanguage): List<WordCaption> =
        captionsByLanguage[language].orEmpty()
}

data class VideoAnalysisResult(
    val clips: List<ViralClip>,
    val isPlaceholderAi: Boolean,
    val noticeMessage: String?
)

data class CaptionConfig(
    val enabled: Boolean = true,
    val language: CaptionLanguage = CaptionLanguage.HINGLISH,
    val style: CaptionStylePreset = CaptionStylePreset.HORMOZI_POP,
    val position: CaptionPosition = CaptionPosition.BOTTOM
)

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(
        val progressPercent: Int,
        val stageLabel: String
    ) : ExportState
    data class Completed(
        val savedUri: Uri,
        val fileName: String,
        val fileSizeBytes: Long
    ) : ExportState
    data class Error(val message: String) : ExportState
}

fun formatTimecode(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val totalSeconds = clamped / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val tenths = (clamped % 1000L) / 100L
    return String.format("%02d:%02d.%d", minutes, seconds, tenths)
}

fun formatShortDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) + 500L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format("%.0f KB", kb)
    }
}
