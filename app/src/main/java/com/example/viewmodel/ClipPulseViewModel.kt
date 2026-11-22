package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlaceholderVideoAnalysisRepository
import com.example.data.VideoAnalysisRepository
import com.example.model.CaptionConfig
import com.example.model.CaptionLanguage
import com.example.model.CaptionPosition
import com.example.model.CaptionStylePreset
import com.example.model.CropMode
import com.example.model.ExportState
import com.example.model.VideoMetadata
import com.example.model.ViralClip
import com.example.video.VerticalVideoExporter
import com.example.video.VideoMetadataExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppStage {
    UPLOAD,
    ANALYZING,
    STUDIO
}

enum class StudioInspectorTab(val label: String) {
    LAYOUT_TRIM("Layout & Trim"),
    CAPTIONS("Captions"),
    INSIGHTS("Virality")
}

data class ClipPulseUiState(
    val stage: AppStage = AppStage.UPLOAD,
    val videoMetadata: VideoMetadata? = null,
    val blurBackdropBitmap: Bitmap? = null,
    val analysisStepIndex: Int = 0,
    val analysisStatusText: String = "",
    val isPlaceholderAi: Boolean = false,
    val aiNoticeMessage: String? = null,
    val clips: List<ViralClip> = emptyList(),
    val selectedClipId: String? = null,
    val cropMode: CropMode = CropMode.CENTER_CROP,
    val captionConfig: CaptionConfig = CaptionConfig(),
    val inspectorTab: StudioInspectorTab = StudioInspectorTab.LAYOUT_TRIM,
    val exportState: ExportState = ExportState.Idle,
    val errorBanner: String? = null,
    val durationWarningShown: Boolean = false
) {
    val selectedClip: ViralClip?
        get() = clips.firstOrNull { it.id == selectedClipId } ?: clips.firstOrNull()
}

class ClipPulseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: VideoAnalysisRepository = PlaceholderVideoAnalysisRepository()
    private val exporter = VerticalVideoExporter(application.applicationContext, viewModelScope)

    private val _uiState = MutableStateFlow(ClipPulseUiState())
    val uiState: StateFlow<ClipPulseUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null
    private var backdropJob: Job? = null

    fun onVideoSelected(uri: Uri) {
        analysisJob?.cancel()
        backdropJob?.cancel()
        exporter.cancel()

        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = AppStage.ANALYZING,
                    analysisStepIndex = 0,
                    analysisStatusText = "Inspecting selected video file...",
                    errorBanner = null,
                    exportState = ExportState.Idle
                )
            }

            val metaResult = VideoMetadataExtractor.extract(getApplication(), uri)
            val metadata = metaResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        stage = AppStage.UPLOAD,
                        errorBanner = error.localizedMessage
                            ?: "Could not open this video file. Please pick an MP4 or MOV video."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    videoMetadata = metadata,
                    durationWarningShown = metadata.exceedsMaxRecommendedDuration
                )
            }

            // Load blurred preview backdrop asynchronously so Studio is instant
            loadBackdropForTime(uri, metadata.usableDurationMs / 3L)

            val analysisResult = repository.analyzeVideo(metadata) { stepIdx, statusText ->
                _uiState.update {
                    it.copy(
                        analysisStepIndex = stepIdx,
                        analysisStatusText = statusText
                    )
                }
            }

            val firstClip = analysisResult.clips.firstOrNull()
            _uiState.update {
                it.copy(
                    stage = AppStage.STUDIO,
                    clips = analysisResult.clips,
                    selectedClipId = firstClip?.id,
                    isPlaceholderAi = analysisResult.isPlaceholderAi,
                    aiNoticeMessage = analysisResult.noticeMessage,
                    inspectorTab = StudioInspectorTab.LAYOUT_TRIM
                )
            }

            if (firstClip != null) {
                loadBackdropForTime(uri, (firstClip.startMs + firstClip.endMs) / 2L)
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _uiState.update {
            it.copy(
                stage = AppStage.UPLOAD,
                analysisStepIndex = 0,
                analysisStatusText = ""
            )
        }
    }

    fun returnToUpload() {
        analysisJob?.cancel()
        backdropJob?.cancel()
        exporter.cancel()
        _uiState.update {
            ClipPulseUiState()
        }
    }

    fun dismissErrorBanner() {
        _uiState.update { it.copy(errorBanner = null) }
    }

    fun selectClip(clipId: String) {
        val state = _uiState.value
        val clip = state.clips.firstOrNull { it.id == clipId } ?: return
        _uiState.update { it.copy(selectedClipId = clipId) }
        state.videoMetadata?.let { meta ->
            loadBackdropForTime(meta.uri, (clip.startMs + clip.endMs) / 2L)
        }
    }

    fun selectInspectorTab(tab: StudioInspectorTab) {
        _uiState.update { it.copy(inspectorTab = tab) }
    }

    fun setCropMode(mode: CropMode) {
        _uiState.update { it.copy(cropMode = mode) }
    }

    fun toggleCaptionsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(captionConfig = it.captionConfig.copy(enabled = enabled)) }
    }

    fun setCaptionLanguage(language: CaptionLanguage) {
        _uiState.update { it.copy(captionConfig = it.captionConfig.copy(language = language)) }
    }

    fun setCaptionStyle(style: CaptionStylePreset) {
        _uiState.update { it.copy(captionConfig = it.captionConfig.copy(style = style)) }
    }

    fun setCaptionPosition(position: CaptionPosition) {
        _uiState.update { it.copy(captionConfig = it.captionConfig.copy(position = position)) }
    }

    fun updateSelectedClipTrim(newStartMs: Long, newEndMs: Long) {
        val meta = _uiState.value.videoMetadata ?: return
        val maxMs = meta.usableDurationMs
        val clampedStart = newStartMs.coerceIn(0L, (maxMs - 1_500L).coerceAtLeast(0L))
        val clampedEnd = newEndMs.coerceIn((clampedStart + 1_500L).coerceAtMost(maxMs), maxMs)

        _uiState.update { state ->
            val targetId = state.selectedClipId ?: return@update state
            val updatedClips = state.clips.map { clip ->
                if (clip.id == targetId) {
                    val oldDuration = (clip.endMs - clip.startMs).coerceAtLeast(1L).toDouble()
                    val newDuration = (clampedEnd - clampedStart).coerceAtLeast(1L).toDouble()
                    // Re-time word captions smoothly to stay synced inside the new trim window
                    val remappedCaptions = clip.captionsByLanguage.mapValues { (_, words) ->
                        words.map { w ->
                            val relStart = ((w.startMs - clip.startMs) / oldDuration).coerceIn(0.0, 1.0)
                            val relEnd = ((w.endMs - clip.startMs) / oldDuration).coerceIn(relStart, 1.0)
                            w.copy(
                                startMs = clampedStart + (relStart * newDuration).toLong(),
                                endMs = clampedStart + (relEnd * newDuration).toLong()
                            )
                        }
                    }
                    clip.copy(
                        startMs = clampedStart,
                        endMs = clampedEnd,
                        captionsByLanguage = remappedCaptions
                    )
                } else {
                    clip
                }
            }
            state.copy(clips = updatedClips)
        }
    }

    fun startExport() {
        val state = _uiState.value
        val metadata = state.videoMetadata ?: return
        val clip = state.selectedClip ?: return

        _uiState.update {
            it.copy(exportState = ExportState.Exporting(4, "Initializing hardware encoder..."))
        }

        exporter.exportClip(
            metadata = metadata,
            clip = clip,
            cropMode = state.cropMode,
            captionConfig = state.captionConfig,
            onProgress = { percent, stageLabel ->
                _uiState.update {
                    it.copy(exportState = ExportState.Exporting(percent, stageLabel))
                }
            },
            onCompleted = { savedUri, fileName, fileSizeBytes ->
                _uiState.update {
                    it.copy(
                        exportState = ExportState.Completed(
                            savedUri = savedUri,
                            fileName = fileName,
                            fileSizeBytes = fileSizeBytes
                        )
                    )
                }
            },
            onError = { message ->
                _uiState.update {
                    it.copy(exportState = ExportState.Error(message))
                }
            }
        )
    }

    fun cancelExport() {
        exporter.cancel()
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun dismissExportSheet() {
        if (_uiState.value.exportState is ExportState.Exporting) {
            exporter.cancel()
        }
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    private fun loadBackdropForTime(uri: Uri, timeMs: Long) {
        backdropJob?.cancel()
        backdropJob = viewModelScope.launch {
            val bitmap = VideoMetadataExtractor.extractBlurredBackdrop(
                context = getApplication(),
                uri = uri,
                timeMs = timeMs
            )
            if (bitmap != null) {
                _uiState.update { it.copy(blurBackdropBitmap = bitmap) }
            }
        }
    }

    override fun onCleared() {
        exporter.cancel()
        super.onCleared()
    }
}
