package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CaptionConfig
import com.example.model.CaptionLanguage
import com.example.model.CaptionPosition
import com.example.model.CaptionStylePreset
import com.example.model.CropMode
import com.example.model.VideoMetadata
import com.example.model.ViralClip
import com.example.model.formatShortDuration
import com.example.model.formatTimecode
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViralCoral

@Composable
fun LayoutAndTrimPanel(
    metadata: VideoMetadata,
    clip: ViralClip,
    cropMode: CropMode,
    onSelectCropMode: (CropMode) -> Unit,
    onUpdateTrim: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 9:16 Aspect Crop Mode Picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "9:16 VERTICAL FRAMING",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CropModeCard(
                    mode = CropMode.CENTER_CROP,
                    icon = Icons.Rounded.CropPortrait,
                    selected = cropMode == CropMode.CENTER_CROP,
                    onClick = { onSelectCropMode(CropMode.CENTER_CROP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crop_mode_center")
                )
                CropModeCard(
                    mode = CropMode.BLUR_FILL,
                    icon = Icons.Rounded.BlurOn,
                    selected = cropMode == CropMode.BLUR_FILL,
                    onClick = { onSelectCropMode(CropMode.BLUR_FILL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crop_mode_blur")
                )
            }
        }

        // Precision Clip Window Trimmer
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLIP TRIM WINDOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AcidLime.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = formatShortDuration(clip.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                val maxFloat = metadata.usableDurationMs.toFloat().coerceAtLeast(2_000f)
                val startFloat = clip.startMs.toFloat().coerceIn(0f, maxFloat - 1_500f)
                val endFloat = clip.endMs.toFloat().coerceIn(startFloat + 1_500f, maxFloat)

                RangeSlider(
                    value = startFloat..endFloat,
                    onValueChange = { range ->
                        val s = range.start.toLong()
                        val e = range.endInclusive.toLong()
                        if (e - s >= 1_500L) {
                            onUpdateTrim(s, e)
                        }
                    },
                    valueRange = 0f..maxFloat,
                    colors = SliderDefaults.colors(
                        thumbColor = AcidLime,
                        activeTrackColor = AcidLime,
                        inactiveTrackColor = ObsidianBlack
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrimNudgeControl(
                        label = "START",
                        timecode = formatTimecode(clip.startMs),
                        onMinus = { onUpdateTrim(clip.startMs - 1_000L, clip.endMs) },
                        onPlus = { onUpdateTrim(clip.startMs + 1_000L, clip.endMs) }
                    )
                    TrimNudgeControl(
                        label = "END",
                        timecode = formatTimecode(clip.endMs),
                        onMinus = { onUpdateTrim(clip.startMs, clip.endMs - 1_000L) },
                        onPlus = { onUpdateTrim(clip.startMs, clip.endMs + 1_000L) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CropModeCard(
    mode: CropMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AcidLime.copy(alpha = 0.13f) else ObsidianSurfaceContainer,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) AcidLime else HairlineBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AcidLime else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) AcidLime else TextPrimary
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TrimNudgeControl(
    label: String,
    timecode: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label  $timecode",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallNudgeChip("-1s", onMinus)
            SmallNudgeChip("+1s", onPlus)
        }
    }
}

@Composable
private fun SmallNudgeChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptionsEditorPanel(
    clip: ViralClip,
    config: CaptionConfig,
    currentPositionMs: Long,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectLanguage: (CaptionLanguage) -> Unit,
    onSelectStyle: (CaptionStylePreset) -> Unit,
    onSelectPosition: (CaptionPosition) -> Unit,
    onSeekToWord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Toggle Row
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Burn-In Kinetic Captions",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Word-by-word animated overlay in preview & MP4",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnAcidLime,
                        checkedTrackColor = AcidLime
                    ),
                    modifier = Modifier.testTag("captions_toggle_switch")
                )
            }
        }

        // Language Picker (Hinglish / Hindi / English)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CAPTION LANGUAGE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptionLanguage.entries.forEach { lang ->
                    val selected = config.language == lang
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) AcidLime else ObsidianSurfaceContainer,
                        border = BorderStroke(1.dp, if (selected) AcidLime else HairlineBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(lang) }
                            .testTag("lang_${lang.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = lang.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) OnAcidLime else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Style Preset Picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CAPTION STYLE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptionStylePreset.entries.forEach { preset ->
                    val selected = config.style == preset
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) CyanSpark.copy(alpha = 0.14f) else ObsidianSurfaceContainer,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) CyanSpark else HairlineBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectStyle(preset) }
                            .testTag("style_${preset.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) CyanSpark else TextPrimary
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Position Picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "VERTICAL POSITION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CaptionPosition.entries.forEach { pos ->
                    val selected = config.position == pos
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ObsidianSurfaceElevated else ObsidianSurfaceContainer,
                        border = BorderStroke(1.dp, if (selected) AcidLime else HairlineBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPosition(pos) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = pos.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) AcidLime else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Interactive Word-by-Word Transcript
        val words = clip.captionsFor(config.language)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TIMED WORD TRANSCRIPT (TAP TO SEEK)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    words.forEach { word ->
                        val isSpoken = currentPositionMs in word.startMs..word.endMs
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpoken) AcidLime else ObsidianSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSpoken) AcidLime else HairlineBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeekToWord(word.startMs) }
                        ) {
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSpoken) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSpoken) OnAcidLime else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViralityInsightsPanel(
    clip: ViralClip,
    onCopyTitleAndTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = ViralCoral
                    )
                    Text(
                        text = "Virality Score: ${clip.viralityScore}/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Text(
                    text = clip.hookReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                ScoreMetricBar("Opening Hook", clip.hookScore, AcidLime)
                ScoreMetricBar("Retention Pacing", clip.pacingScore, CyanSpark)
                ScoreMetricBar("Message Clarity", clip.clarityScore, SuccessMint)
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurfaceContainer,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SUGGESTED VIRAL TITLE & TAGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = clip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    clip.hashtags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ObsidianSurfaceElevated
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanSpark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onCopyTitleAndTags,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = AcidLime
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("copy_title_tags_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_copy_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreMetricBar(label: String, score: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "$score/100",
                style = MaterialTheme.typography.labelSmall,
                color = if (score >= 90) accent else AmberScore
            )
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = accent,
            trackColor = ObsidianBlack
        )
    }
}
