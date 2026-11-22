package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AnalyzingScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.theme.ClipPulseTheme
import com.example.viewmodel.AppStage
import com.example.viewmodel.ClipPulseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ClipPulseTheme {
                ClipPulseApp()
            }
        }
    }
}

@Composable
fun ClipPulseApp(
    viewModel: ClipPulseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoSelected(uri)
        }
    }

    // SAF Document Picker fallback for videos stored in Downloads/Files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoSelected(uri)
        }
    }

    Crossfade(
        targetState = uiState.stage,
        animationSpec = tween(durationMillis = 260),
        label = "appStage",
        modifier = Modifier.fillMaxSize()
    ) { stage ->
        when (stage) {
            AppStage.UPLOAD -> UploadScreen(
                errorBanner = uiState.errorBanner,
                onDismissError = viewModel::dismissErrorBanner,
                onPickFromGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onBrowseFiles = {
                    openDocumentLauncher.launch(arrayOf("video/*"))
                }
            )

            AppStage.ANALYZING -> AnalyzingScreen(
                metadata = uiState.videoMetadata,
                stepIndex = uiState.analysisStepIndex,
                statusText = uiState.analysisStatusText,
                onCancel = viewModel::cancelAnalysis
            )

            AppStage.STUDIO -> StudioScreen(
                uiState = uiState,
                onBackToUpload = viewModel::returnToUpload,
                onSelectClip = viewModel::selectClip,
                onSelectTab = viewModel::selectInspectorTab,
                onSelectCropMode = viewModel::setCropMode,
                onToggleCaptions = viewModel::toggleCaptionsEnabled,
                onSelectLanguage = viewModel::setCaptionLanguage,
                onSelectStyle = viewModel::setCaptionStyle,
                onSelectPosition = viewModel::setCaptionPosition,
                onUpdateTrim = viewModel::updateSelectedClipTrim,
                onStartExport = viewModel::startExport,
                onCancelExport = viewModel::cancelExport,
                onDismissExport = viewModel::dismissExportSheet
            )
        }
    }
}
