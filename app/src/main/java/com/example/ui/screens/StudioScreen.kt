package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.model.CaptionLanguage
import com.example.model.CaptionPosition
import com.example.model.CaptionStylePreset
import com.example.model.CropMode
import com.example.model.ExportState
import com.example.model.ViralClip
import com.example.model.formatShortDuration
import com.example.model.formatTimecode
import com.example.ui.components.CaptionsEditorPanel
import com.example.ui.components.ExportProgressDialog
import com.example.ui.components.LayoutAndTrimPanel
import com.example.ui.components.VerticalPreviewPlayer
import com.example.ui.components.ViralityInsightsPanel
import com.example.ui.theme.AcidLime
import com.example.ui.theme.AmberScore
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnAcidLime
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViralCoral
import com.example.viewmodel.ClipPulseUiState
import com.example.viewmodel.StudioInspectorTab
import kotlinx.coroutines.delay

@Composable
fun StudioScreen(
    uiState: ClipPulseUiState,
    onBackToUpload: () -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectTab: (StudioInspectorTab) -> Unit,
    onSelectCropMode: (CropMode) -> Unit,
    onToggleCaptions: (Boolean) -> Unit,
    onSelectLanguage: (CaptionLanguage) -> Unit,
    onSelectStyle: (CaptionStylePreset) -> Unit,
    onSelectPosition: (CaptionPosition) -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismissExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToUpload)

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val metadata = uiState.videoMetadata ?: return
    val activeClip = uiState.selectedClip ?: return

    // Real ExoPlayer bound cleanly to the user's selected video URI
    val exoPlayer = remember(metadata.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(metadata.uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(activeClip.startMs) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Pause live preview during export so hardware decoders are freed for Media3 Transformer
    LaunchedEffect(uiState.exportState) {
        if (uiState.exportState is ExportState.Exporting) {
            exoPlayer.pause()
        }
    }

    // Seek immediately when user switches clips or drags start beyond current position
    LaunchedEffect(activeClip.id, activeClip.startMs) {
        if (exoPlayer.currentPosition < activeClip.startMs || exoPlayer.currentPosition >= activeClip.endMs) {
            exoPlayer.seekTo(activeClip.startMs)
            currentPositionMs = activeClip.startMs
        }
    }

    // Smooth 30fps playback position poller + seamless clip-window loop
    LaunchedEffect(exoPlayer, activeClip.startMs, activeClip.endMs) {
        while (true) {
            val pos = exoPlayer.currentPosition
            if (pos >= activeClip.endMs) {
                exoPlayer.seekTo(activeClip.startMs)
                currentPositionMs = activeClip.startMs
            } else if (pos < activeClip.startMs - 250L) {
                exoPlayer.seekTo(activeClip.startMs)
                currentPositionMs = activeClip.startMs
            } else {
                currentPositionMs = pos
            }
            delay(33L)
        }
    }

    val shareVideo: (Uri) -> Unit = { uri ->
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${activeClip.title}\n${activeClip.hashtags.joinToString(" ")}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share 9:16 Viral Clip"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Studio Toolbar
        StudioTopBar(
            videoTitle = metadata.displayName,
            durationLabel = formatShortDuration(metadata.durationMs),
            onBack = onBackToUpload,
            onExport = onStartExport
        )

        // Adaptive Layout: side-by-side on tablets/wide screens, vertical studio on compact phones
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isExpanded = maxWidth >= 720.dp

            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Pane: Full-height 9:16 Preview Stage
                    Box(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalPreviewPlayer(
                            exoPlayer = exoPlayer,
                            clip = activeClip,
                            cropMode = uiState.cropMode,
                            captionConfig = uiState.captionConfig,
                            blurBackdropBitmap = uiState.blurBackdropBitmap,
                            currentPositionMs = currentPositionMs,
                            isPlaying = isPlaying,
                            isMuted = isMuted,
                            onTogglePlay = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            onToggleMute = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            onSeekTo = { target ->
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier.width(300.dp)
                        )
                    }

                    // Right Pane: Clips Rail + Studio Inspector
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StudioControlsBody(
                            uiState = uiState,
                            activeClip = activeClip,
                            currentPositionMs = currentPositionMs,
                            onSelectClip = onSelectClip,
                            onSelectTab = onSelectTab,
                            onSelectCropMode = onSelectCropMode,
                            onToggleCaptions = onToggleCaptions,
                            onSelectLanguage = onSelectLanguage,
                            onSelectStyle = onSelectStyle,
                            onSelectPosition = onSelectPosition,
                            onUpdateTrim = onUpdateTrim,
                            onSeekToWord = { wordMs ->
                                exoPlayer.seekTo(wordMs)
                                currentPositionMs = wordMs
                            },
                            onCopyTitleAndTags = {
                                val text = "${activeClip.title}\n\n${activeClip.hashtags.joinToString(" ")}"
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Copied viral title & hashtags", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Compact Mobile Vertical Studio
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered 9:16 Vertical Preview sized for comfortable mobile scrolling
                    VerticalPreviewPlayer(
                        exoPlayer = exoPlayer,
                        clip = activeClip,
                        cropMode = uiState.cropMode,
                        captionConfig = uiState.captionConfig,
                        blurBackdropBitmap = uiState.blurBackdropBitmap,
                        currentPositionMs = currentPositionMs,
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        onTogglePlay = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onToggleMute = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        onSeekTo = { target ->
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                        },
                        modifier = Modifier.width(232.dp)
                    )

                    StudioControlsBody(
                        uiState = uiState,
                        activeClip = activeClip,
                        currentPositionMs = currentPositionMs,
                        onSelectClip = onSelectClip,
                        onSelectTab = onSelectTab,
                        onSelectCropMode = onSelectCropMode,
                        onToggleCaptions = onToggleCaptions,
                        onSelectLanguage = onSelectLanguage,
                        onSelectStyle = onSelectStyle,
                        onSelectPosition = onSelectPosition,
                        onUpdateTrim = onUpdateTrim,
                        onSeekToWord = { wordMs ->
                            exoPlayer.seekTo(wordMs)
                            currentPositionMs = wordMs
                        },
                        onCopyTitleAndTags = {
                            val text = "${activeClip.title}\n\n${activeClip.hashtags.joinToString(" ")}"
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Copied viral title & hashtags", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    ExportProgressDialog(
        exportState = uiState.exportState,
        onCancel = onCancelExport,
        onDismiss = onDismissExport,
        onShare = shareVideo
    )
}

@Composable
private fun StudioTopBar(
    videoTitle: String,
    durationLabel: String,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(0.5.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_to_upload_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_upload),
                    tint = TextPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Source $durationLabel • 9:16 Studio",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onExport,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidLime,
                    contentColor = OnAcidLime
                ),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("export_mp4_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.cd_export_video),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Export MP4",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun StudioControlsBody(
    uiState: ClipPulseUiState,
    activeClip: ViralClip,
    currentPositionMs: Long,
    onSelectClip: (String) -> Unit,
    onSelectTab: (StudioInspectorTab) -> Unit,
    onSelectCropMode: (CropMode) -> Unit,
    onToggleCaptions: (Boolean) -> Unit,
    onSelectLanguage: (CaptionLanguage) -> Unit,
    onSelectStyle: (CaptionStylePreset) -> Unit,
    onSelectPosition: (CaptionPosition) -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onSeekToWord: (Long) -> Unit,
    onCopyTitleAndTags: () -> Unit
) {
    val metadata = uiState.videoMetadata ?: return

    // Transparent notice when running in placeholder AI mode (Firebase declined)
    if (uiState.isPlaceholderAi) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyanSpark.copy(alpha = 0.09f),
            border = BorderStroke(1.dp, CyanSpark.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("placeholder_ai_notice")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = CyanSpark,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
                Text(
                    text = stringResource(R.string.notice_placeholder_ai),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }

    if (uiState.durationWarningShown) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AmberScore.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AmberScore.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = AmberScore,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.notice_max_duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }

    // Detected Viral Clips Carousel
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AcidLime,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "TOP ${uiState.clips.size} VIRAL MOMENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${formatTimecode(activeClip.startMs)} – ${formatTimecode(activeClip.endMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = AcidLime
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(uiState.clips, key = { _, item -> item.id }) { idx, clip ->
                val isSelected = clip.id == activeClip.id
                ViralClipCard(
                    clip = clip,
                    selected = isSelected,
                    onClick = { onSelectClip(clip.id) },
                    modifier = Modifier.testTag("clip_card_$idx")
                )
            }
        }
    }

    // Inspector Segmented Tabs
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurfaceContainer,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StudioInspectorTab.entries.forEach { tab ->
                val selected = uiState.inspectorTab == tab
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) ObsidianSurfaceElevated else ObsidianSurfaceContainer,
                    border = if (selected) BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("tab_${tab.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AcidLime else TextSecondary
                        )
                    }
                }
            }
        }
    }

    // Active Inspector Panel
    when (uiState.inspectorTab) {
        StudioInspectorTab.LAYOUT_TRIM -> LayoutAndTrimPanel(
            metadata = metadata,
            clip = activeClip,
            cropMode = uiState.cropMode,
            onSelectCropMode = onSelectCropMode,
            onUpdateTrim = onUpdateTrim
        )

        StudioInspectorTab.CAPTIONS -> CaptionsEditorPanel(
            clip = activeClip,
            config = uiState.captionConfig,
            currentPositionMs = currentPositionMs,
            onToggleEnabled = onToggleCaptions,
            onSelectLanguage = onSelectLanguage,
            onSelectStyle = onSelectStyle,
            onSelectPosition = onSelectPosition,
            onSeekToWord = onSeekToWord
        )

        StudioInspectorTab.INSIGHTS -> ViralityInsightsPanel(
            clip = activeClip,
            onCopyTitleAndTags = onCopyTitleAndTags
        )
    }
}

@Composable
private fun ViralClipCard(
    clip: ViralClip,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        clip.viralityScore >= 92 -> AcidLime
        clip.viralityScore >= 86 -> SuccessMint
        else -> CyanSpark
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ObsidianSurfaceElevated else ObsidianSurfaceContainer,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) AcidLime else HairlineBorder
        ),
        modifier = modifier
            .width(214.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) AcidLime else ObsidianSurface
                ) {
                    Text(
                        text = "#${clip.rank} CLIP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) OnAcidLime else TextSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = scoreColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${clip.viralityScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = clip.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatShortDuration(clip.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "${formatTimecode(clip.startMs)} →",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) AcidLime else ViralCoral
                )
            }
        }
    }
}
