package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CameraShot
import com.example.model.EnvironmentType
import com.example.model.SceneItem
import com.example.model.ScriptLength
import com.example.model.ScriptTone
import com.example.model.ScriptTopic
import com.example.model.ScriptUiState
import com.example.model.Speaker
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScriptTab(
    selectedTopic: ScriptTopic,
    customTopic: String,
    selectedTone: ScriptTone,
    selectedLength: ScriptLength,
    scriptUiState: ScriptUiState,
    draftScenes: List<SceneItem>,
    onSelectTopic: (ScriptTopic) -> Unit,
    onCustomTopicChange: (String) -> Unit,
    onSelectTone: (ScriptTone) -> Unit,
    onSelectLength: (ScriptLength) -> Unit,
    onGenerateScript: () -> Unit,
    onUpdateDraftScene: (String, (SceneItem) -> SceneItem) -> Unit,
    onAddDraftScene: () -> Unit,
    onRemoveDraftScene: (String) -> Unit,
    onApplyToStudio: () -> Unit
) {
    val scrollState = rememberScrollState()

    androidx.compose.runtime.LaunchedEffect(scriptUiState) {
        if (scriptUiState is ScriptUiState.Success) {
            scrollState.animateScrollTo(420)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Generator Configuration Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = SaffronAmber
                    )
                    Column {
                        Text(
                            text = "AI Script Generator (स्क्रिप्ट लिखें)",
                            style = MaterialTheme.typography.titleLarge,
                            color = RoyalGold
                        )
                        Text(
                            text = "Generate devotional Ram-Lakshman-Hanuman Hindi dialogues",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateText
                        )
                    }
                }

                // Topic Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Topic / Theme (विषय चुनें)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlateText
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScriptTopic.entries.forEach { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { onSelectTopic(topic) },
                                label = {
                                    Text(
                                        "${topic.hindiLabel} (${topic.englishLabel})",
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

                if (selectedTopic == ScriptTopic.CUSTOM) {
                    OutlinedTextField(
                        value = customTopic,
                        onValueChange = onCustomTopicChange,
                        label = { Text("Custom Topic (अपना विषय लिखें)") },
                        placeholder = { Text("e.g. क्रोध पर विजय या समय का महत्व") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DevanagariFontFamily),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronAmber,
                            unfocusedBorderColor = StudioBorder,
                            focusedLabelColor = SaffronAmber
                        )
                    )
                }

                // Tone Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tone (शैली)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlateText
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScriptTone.entries.forEach { tone ->
                            FilterChip(
                                selected = selectedTone == tone,
                                onClick = { onSelectTone(tone) },
                                label = {
                                    Text(
                                        "${tone.hindiLabel} • ${tone.englishLabel}",
                                        fontFamily = DevanagariFontFamily
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold,
                                    selectedLabelColor = MidnightSlate
                                )
                            )
                        }
                    }
                }

                // Target Length Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target Length (अवधि)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlateText
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ScriptLength.entries.forEach { len ->
                            FilterChip(
                                selected = selectedLength == len,
                                onClick = { onSelectLength(len) },
                                label = { Text(len.label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronAmber,
                                    selectedLabelColor = MidnightSlate
                                )
                            )
                        }
                    }
                }

                if (scriptUiState is ScriptUiState.Error) {
                    Surface(
                        color = CrimsonAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = CrimsonAccent
                                )
                                Text(
                                    text = scriptUiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IvoryText
                                )
                            }
                            OutlinedButton(
                                onClick = onGenerateScript,
                                border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.7f))
                            ) {
                                Text("Retry Now (पुनः प्रयास करें)", color = SaffronAmber)
                            }
                        }
                    }
                }

                Button(
                    onClick = onGenerateScript,
                    enabled = scriptUiState !is ScriptUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_script_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronAmber,
                        contentColor = MidnightSlate
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (scriptUiState is ScriptUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MidnightSlate
                        )
                        Text(
                            text = "  Writing Hindi Samvad...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text(
                            text = "  Generate Script (स्क्रिप्ट बनाएं)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DevanagariFontFamily
                            )
                        )
                    }
                }
            }
        }

        // Generated Editable Draft Scenes
        if (draftScenes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Generated Scenes (${draftScenes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold
                )
                OutlinedButton(
                    onClick = onAddDraftScene,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Add Line")
                }
            }

            Button(
                onClick = onApplyToStudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("apply_script_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAccent,
                    contentColor = MidnightSlate
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Text(
                    text = "  Apply Script to Video Studio",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            draftScenes.forEachIndexed { idx, scene ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scene ${idx + 1} • ${scene.speaker.hindiName}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DevanagariFontFamily
                                ),
                                color = SaffronAmber
                            )
                            if (draftScenes.size > 1) {
                                IconButton(
                                    onClick = { onRemoveDraftScene(scene.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove generated scene",
                                        tint = CrimsonAccent
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = scene.dialogue,
                            onValueChange = { txt ->
                                onUpdateDraftScene(scene.id) { it.copy(dialogue = txt) }
                            },
                            label = { Text("Edit Hindi Dialogue") },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = DevanagariFontFamily
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SaffronAmber,
                                unfocusedBorderColor = StudioBorder
                            )
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Speaker.entries.forEach { sp ->
                                FilterChip(
                                    selected = scene.speaker == sp,
                                    onClick = { onUpdateDraftScene(scene.id) { it.copy(speaker = sp) } },
                                    label = { Text(sp.hindiName, fontFamily = DevanagariFontFamily) }
                                )
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CameraShot.entries.forEach { shot ->
                                FilterChip(
                                    selected = scene.cameraShot == shot,
                                    onClick = { onUpdateDraftScene(scene.id) { it.copy(cameraShot = shot) } },
                                    label = { Text(shot.englishLabel) }
                                )
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EnvironmentType.entries.forEach { env ->
                                FilterChip(
                                    selected = scene.environment == env,
                                    onClick = { onUpdateDraftScene(scene.id) { it.copy(environment = env) } },
                                    label = { Text(env.hindiLabel, fontFamily = DevanagariFontFamily) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duration: ${String.format(Locale.US, "%.1fs", scene.durationSec)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedSlateText
                            )
                            Slider(
                                value = scene.durationSec,
                                onValueChange = { d ->
                                    onUpdateDraftScene(scene.id) { it.copy(durationSec = d) }
                                },
                                valueRange = 3f..9f,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = SaffronAmber,
                                    activeTrackColor = SaffronAmber
                                )
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurface,
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = RoyalGold.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Choose a Dharma theme, tone & duration above, then tap Generate Script.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlateText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
