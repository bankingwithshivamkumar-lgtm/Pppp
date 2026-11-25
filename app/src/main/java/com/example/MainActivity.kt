package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.appcheck.configureAppCheck
import com.example.ui.components.ExportStatusDialog
import com.example.ui.screens.AiScriptTab
import com.example.ui.screens.AudioExportTab
import com.example.ui.screens.StudioTimelineTab
import com.example.ui.screens.VisualsStyleTab
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SamvadStudioTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.viewmodel.StudioTab
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            SamvadStudioTheme {
                SamvadStudioApp()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
fun SamvadStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val selectedSceneIndex by viewModel.selectedSceneIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTimeSec by viewModel.currentTimeSec.collectAsStateWithLifecycle()
    val visualSettings by viewModel.visualSettings.collectAsStateWithLifecycle()
    val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val defaultVoiceAssignments by viewModel.defaultVoiceAssignments.collectAsStateWithLifecycle()

    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val customTopic by viewModel.customTopic.collectAsStateWithLifecycle()
    val selectedTone by viewModel.selectedTone.collectAsStateWithLifecycle()
    val selectedLength by viewModel.selectedLength.collectAsStateWithLifecycle()
    val scriptUiState by viewModel.scriptUiState.collectAsStateWithLifecycle()
    val draftScenes by viewModel.draftScenes.collectAsStateWithLifecycle()

    val exportResolution by viewModel.exportResolution.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    if (activeTab != StudioTab.STUDIO) {
        BackHandler {
            viewModel.selectTab(StudioTab.STUDIO)
        }
    }

    val totalDurationSec = viewModel.totalDuration()
    val (activePlaybackIdx, _, activeSceneProgress) = viewModel.sceneAtTime(currentTimeSec)

    ExportStatusDialog(
        exportState = exportState,
        onCancel = viewModel::cancelExport,
        onDismiss = viewModel::dismissExportState
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MidnightSlate, Color(0xFF0E1424), MidnightSlate)
                )
            )
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                StudioTopBar(
                    onExportClick = viewModel::startExport
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = StudioSurface,
                        contentColor = IvoryText
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MidnightSlate,
                                    selectedTextColor = SaffronAmber,
                                    indicatorColor = SaffronAmber,
                                    unselectedIconColor = MutedSlateText,
                                    unselectedTextColor = MutedSlateText
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        contentColor = IvoryText,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MidnightSlate,
                                    selectedTextColor = SaffronAmber,
                                    indicatorColor = SaffronAmber,
                                    unselectedIconColor = MutedSlateText,
                                    unselectedTextColor = MutedSlateText
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (activeTab) {
                        StudioTab.STUDIO -> StudioTimelineTab(
                            isWideScreen = isWideScreen,
                            renderer = viewModel.renderer,
                            scenes = scenes,
                            selectedSceneIndex = selectedSceneIndex,
                            activePlaybackSceneIndex = activePlaybackIdx,
                            activeSceneProgress = activeSceneProgress,
                            currentTimeSec = currentTimeSec,
                            totalDurationSec = totalDurationSec,
                            visualSettings = visualSettings,
                            isPlaying = isPlaying,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onRestart = viewModel::restart,
                            onSeek = viewModel::seekTo,
                            onSelectScene = viewModel::selectAndSeekScene,
                            onAddScene = viewModel::addScene,
                            onDeleteScene = viewModel::deleteScene,
                            onMoveScene = viewModel::moveScene,
                            onUpdateScene = viewModel::updateScene,
                            onSetCustomSceneImage = viewModel::setCustomSceneImage,
                            onSetCustomSceneVoiceover = viewModel::setCustomSceneVoiceover
                        )

                        StudioTab.AI_SCRIPT -> AiScriptTab(
                            selectedTopic = selectedTopic,
                            customTopic = customTopic,
                            selectedTone = selectedTone,
                            selectedLength = selectedLength,
                            scriptUiState = scriptUiState,
                            draftScenes = draftScenes,
                            onSelectTopic = viewModel::setTopic,
                            onCustomTopicChange = viewModel::setCustomTopic,
                            onSelectTone = viewModel::setTone,
                            onSelectLength = viewModel::setLength,
                            onGenerateScript = viewModel::generateScript,
                            onUpdateDraftScene = viewModel::updateDraftScene,
                            onAddDraftScene = viewModel::addDraftScene,
                            onRemoveDraftScene = viewModel::removeDraftScene,
                            onApplyToStudio = viewModel::applyDraftToStudio
                        )

                        StudioTab.VISUALS -> VisualsStyleTab(
                            visualSettings = visualSettings,
                            onUpdateVisualSettings = viewModel::updateVisualSettings,
                            onSetCustomCharacterImage = viewModel::setCustomCharacterImage
                        )

                        StudioTab.AUDIO_EXPORT -> AudioExportTab(
                            audioSettings = audioSettings,
                            ttsStatus = ttsStatus,
                            availableVoices = availableVoices,
                            defaultVoiceAssignments = defaultVoiceAssignments,
                            exportResolution = exportResolution,
                            totalDurationSec = totalDurationSec,
                            sceneCount = scenes.size,
                            onUpdateAudioSettings = viewModel::updateAudioSettings,
                            onTestVoiceover = viewModel::testVoiceover,
                            onSetCustomBgm = viewModel::setCustomBgm,
                            onSetFullVideoVoiceover = viewModel::setFullVideoVoiceover,
                            onSelectResolution = viewModel::setExportResolution,
                            onStartExport = viewModel::startExport
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTopBar(
    onExportClick: () -> Unit
) {
    Surface(
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_samvad_studio),
                    contentDescription = "Samvad Studio emblem",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
                Column {
                    Text(
                        text = stringResource(R.string.studio_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.studio_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = DevanagariFontFamily
                        ),
                        color = MutedSlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier.testTag("topbar_export_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaffronAmber,
                    contentColor = MidnightSlate
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " Export",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun tabIcon(tab: StudioTab, selected: Boolean) = when (tab) {
    StudioTab.STUDIO -> if (selected) Icons.Rounded.MovieCreation else Icons.Outlined.MovieCreation
    StudioTab.AI_SCRIPT -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
    StudioTab.VISUALS -> if (selected) Icons.Rounded.Palette else Icons.Outlined.Palette
    StudioTab.AUDIO_EXPORT -> if (selected) Icons.Rounded.GraphicEq else Icons.Outlined.GraphicEq
}
