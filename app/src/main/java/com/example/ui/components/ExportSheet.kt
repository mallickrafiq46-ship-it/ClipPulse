package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.ExportState
import com.example.model.formatFileSize
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.OnAcidLime
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViralCoral

@Composable
fun ExportProgressDialog(
    exportState: ExportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit
) {
    if (exportState is ExportState.Idle) return

    Dialog(
        onDismissRequest = {
            if (exportState !is ExportState.Exporting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = exportState !is ExportState.Exporting,
            dismissOnClickOutside = exportState !is ExportState.Exporting
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (exportState) {
                    is ExportState.Exporting -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (exportState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(74.dp),
                                color = AcidLime,
                                trackColor = ObsidianSurfaceContainer,
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "${exportState.progressPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = AcidLime
                            )
                        }

                        Text(
                            text = "Exporting 9:16 Vertical MP4",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = exportState.stageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanSpark,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { (exportState.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = AcidLime,
                            trackColor = ObsidianBlack
                        )

                        Text(
                            text = "720×1280 HD • H.264 + AAC • On-Device Hardware Encoder",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("cancel_export_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                color = TextSecondary
                            )
                        }
                    }

                    is ExportState.Completed -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SuccessMint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SuccessMint,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Text(
                            text = "Reel Saved to Gallery!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianSurfaceContainer,
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = exportState.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Movies/ClipPulse • ${formatFileSize(exportState.fileSizeBytes)} • 9:16 MP4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AcidLime
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, HairlineBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("export_done_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.action_done),
                                    color = TextPrimary
                                )
                            }

                            Button(
                                onClick = { onShare(exportState.savedUri) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidLime,
                                    contentColor = OnAcidLime
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(50.dp)
                                    .testTag("share_clip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_share_clip),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    is ExportState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(ViralCoral.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = ViralCoral,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Text(
                            text = "Export Failed",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )

                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AcidLime,
                                contentColor = OnAcidLime
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(text = stringResource(R.string.action_done))
                        }
                    }

                    ExportState.Idle -> Unit
                }
            }
        }
    }
}
