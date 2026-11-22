package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OnAcidLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViralCoral

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    errorBanner: String?,
    onDismissError: () -> Unit,
    onPickFromGallery: () -> Unit,
    onBrowseFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AcidLime.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanSpark.copy(alpha = 0.09f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.35f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AcidLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = OnAcidLime,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AcidLime)
                        )
                        Text(
                            text = "9:16 STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AcidLime
                        )
                    }
                }
            }

            // Error Banner if any
            AnimatedVisibility(visible = !errorBanner.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ViralCoral.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ViralCoral.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ViralCoral
                        )
                        Text(
                            text = errorBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss error",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Hero Studio Card with Generated Artwork
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianSurfaceContainer,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9.5f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero_1795324202435),
                        contentDescription = stringResource(R.string.cd_studio_hero),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianBlack.copy(alpha = 0.25f),
                                        ObsidianBlack.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroTagPill(label = "UP TO 15 MIN", accent = AcidLime)
                        HeroTagPill(label = "HI / HINGLISH / EN", accent = CyanSpark)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.hero_headline),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-crop • Blur fill • Burned-in viral captions",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcidLime
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.hero_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            // Primary Upload Action Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPickFromGallery,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = OnAcidLime
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("pick_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.action_pick_video),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onBrowseFiles,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("browse_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = CyanSpark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.action_browse_files),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Text(
                        text = "Supports MP4, MOV & WebM • Processed locally on your phone with zero watermarks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Feature Highlights Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioCapabilityChip(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Viral Hook Finder",
                    tint = AcidLime
                )
                StudioCapabilityChip(
                    icon = Icons.Rounded.CropPortrait,
                    title = "9:16 Crop & Blur Fill",
                    tint = CyanSpark
                )
                StudioCapabilityChip(
                    icon = Icons.Rounded.ClosedCaption,
                    title = "Kinetic Captions",
                    tint = ViralCoral
                )
                StudioCapabilityChip(
                    icon = Icons.Rounded.MovieCreation,
                    title = "720p H.264 MP4",
                    tint = AcidLime
                )
            }

            // 3-Step Pipeline Footer
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurfaceContainer,
                border = BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PipelineStep("01", "Pick Video", "Gallery or Files", Modifier.weight(1f))
                    PipelineStep("02", "Style Clip", "Crop & Captions", Modifier.weight(1f))
                    PipelineStep("03", "Export MP4", "Saved to Movies", Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeroTagPill(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = ObsidianBlack.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun StudioCapabilityChip(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurfaceContainer,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun PipelineStep(
    number: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall,
            color = AcidLime,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
