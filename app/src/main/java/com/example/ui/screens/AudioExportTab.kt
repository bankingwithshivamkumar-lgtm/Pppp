package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.TtsStatus
import com.example.model.AudioSettings
import com.example.model.BgmMood
import com.example.model.ExportResolution
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
fun AudioExportTab(
    audioSettings: AudioSettings,
    ttsStatus: TtsStatus,
    availableVoices: List<com.example.audio.TtsVoiceOption>,
    defaultVoiceAssignments: Map<Speaker, String>,
    exportResolution: ExportResolution,
    totalDurationSec: Float,
    sceneCount: Int,
    onUpdateAudioSettings: ((AudioSettings) -> AudioSettings) -> Unit,
    onTestVoiceover: (Speaker) -> Unit,
    onSetCustomBgm: (Uri?) -> Unit,
    onSetFullVideoVoiceover: (Uri?) -> Unit,
    onSelectResolution: (ExportResolution) -> Unit,
    onStartExport: () -> Unit
) {
    val bgmPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSetCustomBgm(uri)
    }

    val fullVoicePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSetFullVideoVoiceover(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Audio Mixer Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = SaffronAmber)
                    Text(
                        text = "Audio Mixer (ध्वनि मिश्रण)",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = DevanagariFontFamily),
                        color = RoyalGold
                    )
                }

                // Voiceover Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Voiceover Volume", style = MaterialTheme.typography.labelMedium, color = IvoryText)
                    Text(
                        "${(audioSettings.voiceoverVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = SaffronAmber
                    )
                }
                Slider(
                    value = audioSettings.voiceoverVolume,
                    onValueChange = { v -> onUpdateAudioSettings { it.copy(voiceoverVolume = v) } },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("voiceover_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SaffronAmber,
                        activeTrackColor = SaffronAmber
                    )
                )

                // BGM Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Background Music Volume", style = MaterialTheme.typography.labelMedium, color = IvoryText)
                    Text(
                        "${(audioSettings.bgmVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalGold
                    )
                }
                Slider(
                    value = audioSettings.bgmVolume,
                    onValueChange = { v -> onUpdateAudioSettings { it.copy(bgmVolume = v) } },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("bgm_volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = RoyalGold,
                        activeTrackColor = RoyalGold
                    )
                )
            }
        }

        // Background Music Synthesizer & Upload
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = SaffronAmber)
                    Text(
                        text = "Devotional BGM Synthesizer",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BgmMood.entries.forEach { mood ->
                        FilterChip(
                            selected = audioSettings.bgmMood == mood && audioSettings.customBgmUri == null,
                            onClick = {
                                onSetCustomBgm(null)
                                onUpdateAudioSettings { it.copy(bgmMood = mood) }
                            },
                            label = {
                                Text(
                                    "${mood.hindiLabel} (${mood.englishLabel})",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { bgmPicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_custom_bgm_button"),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (audioSettings.customBgmUri != null) {
                                " Custom BGM Loaded (Tap to Change)"
                            } else {
                                " Upload Custom .mp3 / .wav BGM"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (audioSettings.customBgmUri != null) {
                        IconButton(onClick = { onSetCustomBgm(null) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove custom BGM", tint = CrimsonAccent)
                        }
                    }
                }
            }
        }

        // Hindi Voiceover (Distinct Character Voices + Acoustic FX + Full Video Voiceover Upload)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = SaffronAmber)
                        Text(
                            text = "Character Voices (प्रत्येक पात्र की आवाज़)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DevanagariFontFamily
                            ),
                            color = RoyalGold
                        )
                    }
                    Text(
                        text = ttsStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ttsStatus.isReady) EmeraldAccent else CrimsonAccent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distinct Acoustic Voice FX",
                            style = MaterialTheme.typography.labelMedium,
                            color = IvoryText
                        )
                        Text(
                            text = "Unique warmth, presence & resonance per character",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlateText
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = audioSettings.enableAcousticFx,
                        onCheckedChange = { on ->
                            onUpdateAudioSettings { it.copy(enableAcousticFx = on) }
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MidnightSlate,
                            checkedTrackColor = SaffronAmber
                        )
                    )
                }

                Speaker.entries.forEach { speaker ->
                    val pitch = audioSettings.pitchFor(speaker)
                    val rate = audioSettings.rateFor(speaker)
                    val selectedVoice = audioSettings.voiceNameFor(speaker)
                    val autoDefaultVoice = defaultVoiceAssignments[speaker]
                    val activeVoiceLabel = availableVoices
                        .firstOrNull { it.name == (selectedVoice ?: autoDefaultVoice) }
                        ?.displayLabel
                        ?: "System Hindi Default"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${speaker.hindiName} (${speaker.englishName})",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = DevanagariFontFamily
                                        ),
                                        color = IvoryText
                                    )
                                    Text(
                                        text = "${speaker.voiceProfileLabel} • $activeVoiceLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SaffronAmber
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onTestVoiceover(speaker) },
                                    border = BorderStroke(1.dp, SaffronAmber.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        Icons.Rounded.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = SaffronAmber
                                    )
                                    Text(" Test", color = SaffronAmber)
                                }
                            }

                            if (availableVoices.size > 1) {
                                Text(
                                    text = "Select TTS Voice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSlateText
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedVoice == null,
                                        onClick = {
                                            onUpdateAudioSettings {
                                                when (speaker) {
                                                    Speaker.RAM -> it.copy(ramVoiceName = null)
                                                    Speaker.LAKSHMAN -> it.copy(lakshmanVoiceName = null)
                                                    Speaker.HANUMAN -> it.copy(hanumanVoiceName = null)
                                                }
                                            }
                                        },
                                        label = { Text("Auto Default", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoyalGold,
                                            selectedLabelColor = MidnightSlate
                                        )
                                    )
                                    availableVoices.take(4).forEach { voiceOpt ->
                                        FilterChip(
                                            selected = selectedVoice == voiceOpt.name,
                                            onClick = {
                                                onUpdateAudioSettings {
                                                    when (speaker) {
                                                        Speaker.RAM -> it.copy(ramVoiceName = voiceOpt.name)
                                                        Speaker.LAKSHMAN -> it.copy(lakshmanVoiceName = voiceOpt.name)
                                                        Speaker.HANUMAN -> it.copy(hanumanVoiceName = voiceOpt.name)
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    voiceOpt.displayLabel,
                                                    style = MaterialTheme.typography.labelSmall
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pitch", style = MaterialTheme.typography.labelSmall, color = MutedSlateText)
                                Text(
                                    String.format(Locale.US, "%.2f", pitch),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaffronAmber
                                )
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { p ->
                                    onUpdateAudioSettings {
                                        when (speaker) {
                                            Speaker.RAM -> it.copy(ramPitch = p)
                                            Speaker.LAKSHMAN -> it.copy(lakshmanPitch = p)
                                            Speaker.HANUMAN -> it.copy(hanumanPitch = p)
                                        }
                                    }
                                },
                                valueRange = 0.55f..1.45f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SaffronAmber,
                                    activeTrackColor = SaffronAmber
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speed", style = MaterialTheme.typography.labelSmall, color = MutedSlateText)
                                Text(
                                    String.format(Locale.US, "%.2fx", rate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalGold
                                )
                            }
                            Slider(
                                value = rate,
                                onValueChange = { r ->
                                    onUpdateAudioSettings {
                                        when (speaker) {
                                            Speaker.RAM -> it.copy(ramRate = r)
                                            Speaker.LAKSHMAN -> it.copy(lakshmanRate = r)
                                            Speaker.HANUMAN -> it.copy(hanumanRate = r)
                                        }
                                    }
                                },
                                valueRange = 0.65f..1.35f,
                                colors = SliderDefaults.colors(
                                    thumbColor = RoyalGold,
                                    activeTrackColor = RoyalGold
                                )
                            )
                        }
                    }
                }

                // Whole-video voiceover upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { fullVoicePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_full_voiceover_button"),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (audioSettings.fullVideoVoiceoverUri != null) {
                                " Full-Video Voiceover Loaded"
                            } else {
                                " Upload Full-Video Voiceover (.mp3/.wav)"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (audioSettings.fullVideoVoiceoverUri != null) {
                        IconButton(onClick = { onSetFullVideoVoiceover(null) }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear full-video voiceover",
                                tint = CrimsonAccent
                            )
                        }
                    }
                }
            }
        }

        // Video Export Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.MovieCreation, contentDescription = null, tint = SaffronAmber)
                    Column {
                        Text(
                            text = "Export 9:16 Reel (.mp4)",
                            style = MaterialTheme.typography.titleLarge,
                            color = RoyalGold
                        )
                        Text(
                            text = "$sceneCount scenes • ${String.format(Locale.US, "%.1fs", totalDurationSec)} • H.264 + AAC Audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateText
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportResolution.entries.forEach { res ->
                        FilterChip(
                            selected = exportResolution == res,
                            onClick = { onSelectResolution(res) },
                            label = { Text(res.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronAmber,
                                selectedLabelColor = MidnightSlate
                            )
                        )
                    }
                }

                Button(
                    onClick = onStartExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_video_main_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronAmber,
                        contentColor = MidnightSlate
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.MovieCreation, contentDescription = null)
                    Text(
                        text = "  Export & Save Video (.mp4)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
