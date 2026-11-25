package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CameraShot
import com.example.model.EnvironmentType
import com.example.model.SceneItem
import com.example.model.Speaker
import com.example.model.VisualSettings
import com.example.renderer.MythologicalSceneRenderer
import com.example.ui.components.VideoPreviewPlayer
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import java.util.Locale

@Composable
fun StudioTimelineTab(
    isWideScreen: Boolean,
    renderer: MythologicalSceneRenderer,
    scenes: List<SceneItem>,
    selectedSceneIndex: Int,
    activePlaybackSceneIndex: Int,
    activeSceneProgress: Float,
    currentTimeSec: Float,
    totalDurationSec: Float,
    visualSettings: VisualSettings,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSelectScene: (Int) -> Unit,
    onAddScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (Int, Int) -> Unit,
    onUpdateScene: (String, (SceneItem) -> SceneItem) -> Unit,
    onSetCustomSceneImage: (String, Uri?) -> Unit,
    onSetCustomSceneVoiceover: (String, Uri?) -> Unit
) {
    val activeScene = scenes.getOrElse(activePlaybackSceneIndex) { scenes.firstOrNull() ?: SceneItem() }
    val selectedScene = scenes.getOrElse(selectedSceneIndex) { activeScene }

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VideoPreviewPlayer(
                    renderer = renderer,
                    scene = activeScene,
                    sceneProgress = activeSceneProgress,
                    currentTimeSec = currentTimeSec,
                    totalDurationSec = totalDurationSec,
                    visualSettings = visualSettings,
                    isPlaying = isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onRestart = onRestart,
                    onSeek = onSeek,
                    maxCanvasWidth = 280.dp
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimelineSection(
                    scenes = scenes,
                    selectedSceneIndex = selectedSceneIndex,
                    canDelete = scenes.size > 1,
                    onSelectScene = onSelectScene,
                    onAddScene = onAddScene,
                    onDeleteScene = onDeleteScene,
                    onMoveScene = onMoveScene
                )
                SelectedSceneInspector(
                    scene = selectedScene,
                    sceneNumber = selectedSceneIndex + 1,
                    onUpdateScene = onUpdateScene,
                    onSetCustomSceneImage = onSetCustomSceneImage,
                    onSetCustomSceneVoiceover = onSetCustomSceneVoiceover
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VideoPreviewPlayer(
                renderer = renderer,
                scene = activeScene,
                sceneProgress = activeSceneProgress,
                currentTimeSec = currentTimeSec,
                totalDurationSec = totalDurationSec,
                visualSettings = visualSettings,
                isPlaying = isPlaying,
                onTogglePlayPause = onTogglePlayPause,
                onRestart = onRestart,
                onSeek = onSeek,
                maxCanvasWidth = 215.dp
            )
            TimelineSection(
                scenes = scenes,
                selectedSceneIndex = selectedSceneIndex,
                canDelete = scenes.size > 1,
                onSelectScene = onSelectScene,
                onAddScene = onAddScene,
                onDeleteScene = onDeleteScene,
                onMoveScene = onMoveScene
            )
            SelectedSceneInspector(
                scene = selectedScene,
                sceneNumber = selectedSceneIndex + 1,
                onUpdateScene = onUpdateScene,
                onSetCustomSceneImage = onSetCustomSceneImage,
                onSetCustomSceneVoiceover = onSetCustomSceneVoiceover
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TimelineSection(
    scenes: List<SceneItem>,
    selectedSceneIndex: Int,
    canDelete: Boolean,
    onSelectScene: (Int) -> Unit,
    onAddScene: () -> Unit,
    onDeleteScene: (String) -> Unit,
    onMoveScene: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    imageVector = Icons.Rounded.MovieFilter,
                    contentDescription = null,
                    tint = SaffronAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Scene Timeline (${scenes.size} दृश्य)",
                    style = MaterialTheme.typography.titleMedium,
                    color = IvoryText
                )
            }
            OutlinedButton(
                onClick = onAddScene,
                modifier = Modifier.testTag("add_timeline_scene_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaffronAmber)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Add Scene", style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(scenes, key = { _, item -> item.id }) { index, item ->
                val isSelected = index == selectedSceneIndex
                val borderColor = if (isSelected) SaffronAmber else StudioBorder
                val speakerColor = Color(item.speaker.badgeColorHex)

                Surface(
                    modifier = Modifier
                        .width(196.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectScene(index) }
                        .testTag("timeline_scene_card_$index"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) StudioSurfaceElevated else StudioSurface,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(speakerColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#${index + 1} ${item.speaker.hindiName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DevanagariFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = speakerColor
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1fs", item.durationSec),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGold
                            )
                        }

                        Text(
                            text = item.dialogue.ifBlank { "(Empty dialogue)" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = DevanagariFontFamily
                            ),
                            color = IvoryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(38.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { onMoveScene(index, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Move scene left",
                                        tint = if (index > 0) MutedSlateText else StudioBorder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveScene(index, 1) },
                                    enabled = index < scenes.lastIndex,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Move scene right",
                                        tint = if (index < scenes.lastIndex) MutedSlateText else StudioBorder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (canDelete) {
                                IconButton(
                                    onClick = { onDeleteScene(item.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete scene",
                                        tint = CrimsonAccent.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedSceneInspector(
    scene: SceneItem,
    sceneNumber: Int,
    onUpdateScene: (String, (SceneItem) -> SceneItem) -> Unit,
    onSetCustomSceneImage: (String, Uri?) -> Unit,
    onSetCustomSceneVoiceover: (String, Uri?) -> Unit
) {
    val sceneImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSetCustomSceneImage(scene.id, uri)
    }

    val sceneAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSetCustomSceneVoiceover(scene.id, uri)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scene #$sceneNumber Editor (दृश्य संपादन)",
                style = MaterialTheme.typography.titleMedium,
                color = RoyalGold
            )

            OutlinedTextField(
                value = scene.dialogue,
                onValueChange = { text -> onUpdateScene(scene.id) { it.copy(dialogue = text) } },
                label = { Text("Hindi Dialogue (संवाद)") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DevanagariFontFamily),
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scene_dialogue_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaffronAmber,
                    unfocusedBorderColor = StudioBorder,
                    focusedLabelColor = SaffronAmber,
                    cursorColor = SaffronAmber
                )
            )

            // Speaker chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Speaker (वक्ता)", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Speaker.entries.forEach { sp ->
                        val selected = scene.speaker == sp
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateScene(scene.id) { it.copy(speaker = sp) } },
                            label = {
                                Text(
                                    "${sp.hindiName} (${sp.englishName})",
                                    fontFamily = DevanagariFontFamily
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronAmber,
                                selectedLabelColor = MidnightSlate
                            )
                        )
                    }
                }
            }

            // Camera Shot chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Camera Shot (कैमरा शॉट)", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CameraShot.entries.forEach { shot ->
                        FilterChip(
                            selected = scene.cameraShot == shot,
                            onClick = { onUpdateScene(scene.id) { it.copy(cameraShot = shot) } },
                            label = { Text(shot.englishLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = MidnightSlate
                            )
                        )
                    }
                }
            }

            // Environment chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Environment (वातावरण)", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EnvironmentType.entries.forEach { env ->
                        FilterChip(
                            selected = scene.environment == env,
                            onClick = { onUpdateScene(scene.id) { it.copy(environment = env) } },
                            label = { Text(env.hindiLabel, fontFamily = DevanagariFontFamily) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronAmber.copy(alpha = 0.9f),
                                selectedLabelColor = MidnightSlate
                            )
                        )
                    }
                }
            }

            // Duration slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scene Duration", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                    Text(
                        String.format(Locale.US, "%.1f sec", scene.durationSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = SaffronAmber
                    )
                }
                Slider(
                    value = scene.durationSec,
                    onValueChange = { dur -> onUpdateScene(scene.id) { it.copy(durationSec = dur) } },
                    valueRange = 2f..12f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = SaffronAmber,
                        activeTrackColor = SaffronAmber
                    )
                )
            }

            // Custom Scene Backdrop & Per-Scene Voiceover Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sceneImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scene_bg_upload_button"),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (scene.customSceneImageUri != null) " Change BG" else " Custom BG",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (scene.customSceneImageUri != null) {
                    IconButton(
                        onClick = { onSetCustomSceneImage(scene.id, null) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, StudioBorder, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Reset custom scene background", tint = CrimsonAccent)
                    }
                }

                OutlinedButton(
                    onClick = { sceneAudioPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scene_audio_upload_button"),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (scene.customVoiceoverUri != null) " Custom Audio" else " Scene Audio",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (scene.customVoiceoverUri != null) {
                    IconButton(
                        onClick = { onSetCustomSceneVoiceover(scene.id, null) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, StudioBorder, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear scene custom audio", tint = CrimsonAccent)
                    }
                }
            }
        }
    }
}
