package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.VideoMetadata
import com.example.model.formatFileSize
import com.example.model.formatShortDuration
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

private val ANALYSIS_STEPS = listOf(
    "Inspecting video stream & resolution",
    "Detecting high-retention viral hooks",
    "Syncing Hinglish, Hindi & English words",
    "Scoring & ranking top 9:16 moments"
)

@Composable
fun AnalyzingScreen(
    metadata: VideoMetadata?,
    stepIndex: Int,
    statusText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Animated Neon Waveform Visualizer
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianSurfaceContainer,
                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                    ) {
                        val barCount = 28
                        val gap = size.width / (barCount * 1.9f)
                        val barWidth = (size.width - gap * (barCount - 1)) / barCount
                        val centerY = size.height / 2f

                        for (i in 0 until barCount) {
                            val norm = i.toFloat() / barCount
                            val wave = (sin(phase + i * 0.48f) * 0.5f + 0.5f)
                            val envelope = sin(norm * Math.PI).toFloat().coerceAtLeast(0.18f)
                            val barHeight = (size.height * (0.18f + 0.78f * wave * envelope))
                            val left = i * (barWidth + gap)

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(AcidLime, CyanSpark)
                                ),
                                topLeft = Offset(left, centerY - barHeight / 2f),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    Text(
                        text = "Analyzing Your Video",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = statusText.ifBlank { "Scanning scenes & viral rhythm..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AcidLime
                    )
                }
            }

            // Video Metadata Pill
            if (metadata != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyanSpark.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = null,
                                tint = CyanSpark
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = metadata.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatShortDuration(metadata.durationMs)} • ${metadata.effectiveWidth}×${metadata.effectiveHeight} • ${formatFileSize(metadata.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Staged Checklist
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurfaceContainer,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ANALYSIS_STEPS.forEachIndexed { idx, label ->
                        val isDone = idx < stepIndex
                        val isCurrent = idx == stepIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when {
                                isDone -> Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessMint,
                                    modifier = Modifier.size(20.dp)
                                )
                                isCurrent -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.2.dp,
                                    color = AcidLime
                                )
                                else -> Icon(
                                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isDone -> TextPrimary
                                    isCurrent -> AcidLime
                                    else -> TextMuted
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("cancel_analysis_button")
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }
        }
    }
}
