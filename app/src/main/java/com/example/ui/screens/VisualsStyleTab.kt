package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CaptionFrameStyle
import com.example.model.Speaker
import com.example.model.StudioDefaults
import com.example.model.VisualSettings
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualsStyleTab(
    visualSettings: VisualSettings,
    onUpdateVisualSettings: ((VisualSettings) -> VisualSettings) -> Unit,
    onSetCustomCharacterImage: (Speaker, Uri?) -> Unit
) {
    var pendingSpeakerTarget by remember { mutableStateOf(Speaker.RAM) }
    val charPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSetCustomCharacterImage(pendingSpeakerTarget, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar Caption Styling
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
                    Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = SaffronAmber)
                    Text(
                        text = "Top Caption Bar (शीर्ष कैप्शन शैली)",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = DevanagariFontFamily),
                        color = RoyalGold
                    )
                }

                Text(
                    text = "Decorative Icon Frame",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlateText
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CaptionFrameStyle.entries.forEach { style ->
                        FilterChip(
                            selected = visualSettings.captionFrameStyle == style,
                            onClick = {
                                onUpdateVisualSettings { it.copy(captionFrameStyle = style) }
                            },
                            label = { Text(style.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronAmber,
                                selectedLabelColor = MidnightSlate
                            )
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Caption Size", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                        Text(
                            "${visualSettings.captionFontSizeSp.toInt()} sp",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronAmber
                        )
                    }
                    Slider(
                        value = visualSettings.captionFontSizeSp,
                        onValueChange = { sz ->
                            onUpdateVisualSettings { it.copy(captionFontSizeSp = sz) }
                        },
                        valueRange = 14f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = SaffronAmber,
                            activeTrackColor = SaffronAmber
                        )
                    )
                }

                Text("Caption Color", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudioDefaults.captionColors.forEach { (argb, label) ->
                        val selected = visualSettings.captionColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) SaffronAmber else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateVisualSettings { it.copy(captionColorArgb = argb) }
                                }
                                .testTag("caption_color_$label")
                        )
                    }
                }
            }
        }

        // Channel Watermark & Atmospheric Embers
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
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = RoyalGold)
                        Text(
                            text = "Channel Watermark",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryText
                        )
                    }
                    Switch(
                        checked = visualSettings.showWatermark,
                        onCheckedChange = { checked ->
                            onUpdateVisualSettings { it.copy(showWatermark = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MidnightSlate,
                            checkedTrackColor = SaffronAmber
                        )
                    )
                }

                if (visualSettings.showWatermark) {
                    OutlinedTextField(
                        value = visualSettings.watermarkText,
                        onValueChange = { txt ->
                            onUpdateVisualSettings { it.copy(watermarkText = txt) }
                        },
                        label = { Text("Watermark Handle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronAmber,
                            unfocusedBorderColor = StudioBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Watermark Opacity", style = MaterialTheme.typography.labelMedium, color = MutedSlateText)
                        Text(
                            "${(visualSettings.watermarkOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronAmber
                        )
                    }
                    Slider(
                        value = visualSettings.watermarkOpacity,
                        onValueChange = { op ->
                            onUpdateVisualSettings { it.copy(watermarkOpacity = op) }
                        },
                        valueRange = 0.15f..0.85f,
                        colors = SliderDefaults.colors(
                            thumbColor = SaffronAmber,
                            activeTrackColor = SaffronAmber
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = SaffronAmber)
                    Text(
                        text = "Embers & Fireflies Density",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlateText
                    )
                }
                Slider(
                    value = visualSettings.particleIntensity,
                    onValueChange = { dens ->
                        onUpdateVisualSettings { it.copy(particleIntensity = dens) }
                    },
                    valueRange = 0f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = SaffronAmber,
                        activeTrackColor = SaffronAmber
                    )
                )
            }
        }

        // 3D Character Portrait Customizer
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
                Text(
                    text = "3D Character Portraits (पात्र चित्र)",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = DevanagariFontFamily),
                    color = RoyalGold
                )
                Text(
                    text = "Bundled 3D devotional portraits are active by default. Tap Upload to replace any character with your own artwork.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlateText
                )

                Speaker.entries.forEach { speaker ->
                    val defaultRes = when (speaker) {
                        Speaker.RAM -> R.drawable.img_char_ram
                        Speaker.LAKSHMAN -> R.drawable.img_char_lakshman
                        Speaker.HANUMAN -> R.drawable.img_char_hanuman
                    }
                    val customUri = when (speaker) {
                        Speaker.RAM -> visualSettings.customRamImageUri
                        Speaker.LAKSHMAN -> visualSettings.customLakshmanImageUri
                        Speaker.HANUMAN -> visualSettings.customHanumanImageUri
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = defaultRes),
                                contentDescription = speaker.englishName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${speaker.hindiName} (${speaker.englishName})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DevanagariFontFamily
                                    ),
                                    color = IvoryText
                                )
                                Text(
                                    text = if (customUri != null) "Custom image selected" else "Built-in 3D Portrait",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (customUri != null) SaffronAmber else MutedSlateText
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingSpeakerTarget = speaker
                                    charPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, StudioBorder)
                            ) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(" Upload", style = MaterialTheme.typography.labelMedium)
                            }
                            if (customUri != null) {
                                IconButton(onClick = { onSetCustomCharacterImage(speaker, null) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Reset character portrait",
                                        tint = CrimsonAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
