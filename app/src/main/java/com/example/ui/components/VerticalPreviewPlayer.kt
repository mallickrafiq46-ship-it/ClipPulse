package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.model.CaptionConfig
import com.example.model.CaptionLanguage
import com.example.model.CaptionPosition
import com.example.model.CaptionStylePreset
import com.example.model.CropMode
import com.example.model.ViralClip
import com.example.model.formatTimecode
import com.example.ui.theme.AcidLime
import com.example.ui.theme.AntonCaptionFontFamily
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.HairlineBorderBright
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.OnAcidLime
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.video.ActiveCaptionFrame
import com.example.video.CaptionTimelineHelper

@Composable
fun VerticalPreviewPlayer(
    exoPlayer: ExoPlayer,
    clip: ViralClip,
    cropMode: CropMode,
    captionConfig: CaptionConfig,
    blurBackdropBitmap: Bitmap?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var videoWidth by remember { mutableIntStateOf(1280) }
    var videoHeight by remember { mutableIntStateOf(720) }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        val existing = exoPlayer.videoSize
        if (existing.width > 0 && existing.height > 0) {
            videoWidth = existing.width
            videoHeight = existing.height
        }
    }

    val captionChunks = remember(clip.id, clip.startMs, clip.endMs, captionConfig.language) {
        CaptionTimelineHelper.buildChunks(clip.captionsFor(captionConfig.language))
    }
    val activeFrame = remember(captionChunks, currentPositionMs) {
        CaptionTimelineHelper.findActiveFrame(captionChunks, currentPositionMs)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 9:16 Phone-Bezel Live Preview Frame
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianBlack,
            border = BorderStroke(1.5.dp, HairlineBorderBright),
            shadowElevation = 12.dp,
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onTogglePlay)
                .testTag("vertical_preview_surface")
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // 1. Blurred ambient backdrop (visible behind video in BLUR_FILL mode)
                if (cropMode == CropMode.BLUR_FILL && blurBackdropBitmap != null) {
                    Image(
                        bitmap = blurBackdropBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Real TextureView video surface (respects rounded clip & blur backdrop Z-order)
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            isOpaque = false
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    applyVideoMatrix(this@apply, width, height, videoWidth, videoHeight, cropMode)
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    applyVideoMatrix(this@apply, width, height, videoWidth, videoHeight, cropMode)
                                }

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                            }
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                    update = { textureView ->
                        if (textureView.width > 0 && textureView.height > 0) {
                            applyVideoMatrix(
                                textureView,
                                textureView.width,
                                textureView.height,
                                videoWidth,
                                videoHeight,
                                cropMode
                            )
                        }
                    }
                )

                // 3. Top Format & Rank Badge HUD
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianBlack.copy(alpha = 0.68f),
                        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = if (cropMode == CropMode.CENTER_CROP) "9:16 CROP" else "9:16 BLUR",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianBlack.copy(alpha = 0.68f)
                    ) {
                        Text(
                            text = "${clip.viralityScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanSpark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 4. Live Synchronized Kinetic Captions Overlay
                if (captionConfig.enabled && activeFrame != null) {
                    val alignment = when (captionConfig.position) {
                        CaptionPosition.TOP -> Alignment.TopCenter
                        CaptionPosition.CENTER -> Alignment.Center
                        CaptionPosition.BOTTOM -> Alignment.BottomCenter
                    }
                    val verticalPadding = when (captionConfig.position) {
                        CaptionPosition.TOP -> 46.dp
                        CaptionPosition.CENTER -> 0.dp
                        CaptionPosition.BOTTOM -> 36.dp
                    }
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = verticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        KineticCaptionPreviewOverlay(
                            activeFrame = activeFrame,
                            config = captionConfig
                        )
                    }
                }

                // 5. Paused Center Play Indicator
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.68f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = AcidLime,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        // Compact Transport Scrubber Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorderBright.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = AcidLime
                    )
                }

                val clipRelative = (currentPositionMs - clip.startMs).coerceIn(0L, clip.durationMs)
                val fraction = (clipRelative.toFloat() / clip.durationMs.coerceAtLeast(1L).toFloat())
                    .coerceIn(0f, 1f)

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = fraction,
                        onValueChange = { newFrac ->
                            val targetMs = clip.startMs + (newFrac * clip.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = AcidLime,
                            activeTrackColor = AcidLime,
                            inactiveTrackColor = ObsidianBlack
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimecode(clipRelative),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = formatTimecode(clip.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = stringResource(R.string.cd_mute_toggle),
                        tint = if (isMuted) TextSecondary else TextPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KineticCaptionPreviewOverlay(
    activeFrame: ActiveCaptionFrame,
    config: CaptionConfig
) {
    val isDevanagari = config.language == CaptionLanguage.HINDI
    val fontFamily = when {
        isDevanagari -> FontFamily.SansSerif
        config.style == CaptionStylePreset.HORMOZI_POP -> AntonCaptionFontFamily
        else -> SpaceGroteskFontFamily
    }
    val fontSize = when (config.style) {
        CaptionStylePreset.HORMOZI_POP -> if (isDevanagari) 16.sp else 20.sp
        CaptionStylePreset.NEON_PULSE -> if (isDevanagari) 15.sp else 17.sp
        CaptionStylePreset.STUDIO_MINIMAL -> if (isDevanagari) 14.sp else 16.sp
    }

    val containerModifier = when (config.style) {
        CaptionStylePreset.HORMOZI_POP -> Modifier
        CaptionStylePreset.NEON_PULSE -> Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0C0C16))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        CaptionStylePreset.STUDIO_MINIMAL -> Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xB008080C))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    }

    FlowRow(
        modifier = containerModifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        activeFrame.chunk.words.forEachIndexed { idx, word ->
            val isActive = idx == activeFrame.activeWordInChunk
            val label = if (!isDevanagari && config.style == CaptionStylePreset.HORMOZI_POP) {
                word.word.uppercase()
            } else {
                word.word
            }

            when (config.style) {
                CaptionStylePreset.HORMOZI_POP -> {
                    Box(
                        modifier = if (isActive) {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AcidLime)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        } else {
                            Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                        }
                    ) {
                        Text(
                            text = label,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            textAlign = TextAlign.Center,
                            color = if (isActive) OnAcidLime else Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                shadow = if (isActive) null else Shadow(
                                    color = Color.Black,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }
                }

                CaptionStylePreset.NEON_PULSE -> {
                    Box(
                        modifier = if (isActive) {
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanSpark)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        } else {
                            Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                        }
                    ) {
                        Text(
                            text = label,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = fontSize,
                            color = if (isActive) Color(0xFF001C24) else Color(0xFFF0F4FF)
                        )
                    }
                }

                CaptionStylePreset.STUDIO_MINIMAL -> {
                    Text(
                        text = label,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = if (isActive) AcidLime else Color.White
                    )
                }
            }
        }
    }
}

private fun applyVideoMatrix(
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    cropMode: CropMode
) {
    if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

    val scaleX: Float
    val scaleY: Float

    if (cropMode == CropMode.CENTER_CROP) {
        // Fill the 9:16 TextureView completely without stretching
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        }
    } else {
        // Fit the full frame inside the 9:16 TextureView (blurred backdrop shows top/bottom)
        if (videoRatio > viewRatio) {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        } else {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        }
    }

    val pivotX = viewWidth / 2f
    val pivotY = viewHeight / 2f
    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, pivotX, pivotY)
    }
    textureView.setTransform(matrix)
}
