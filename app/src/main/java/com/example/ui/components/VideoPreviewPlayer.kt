package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.SceneItem
import com.example.model.VisualSettings
import com.example.renderer.MythologicalSceneRenderer
import com.example.ui.theme.DevanagariFontFamily
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import java.util.Locale

@Composable
fun VideoPreviewPlayer(
    renderer: MythologicalSceneRenderer,
    scene: SceneItem,
    sceneProgress: Float,
    currentTimeSec: Float,
    totalDurationSec: Float,
    visualSettings: VisualSettings,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxCanvasWidth: Dp = 240.dp
) {
    // Subtle idle ambient clock for embers/fireflies even when paused
    var ambientClockSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            var lastNs = 0L
            while (true) {
                withFrameNanos { ns ->
                    if (lastNs != 0L) {
                        ambientClockSec += (ns - lastNs) / 1_000_000_000f
                    }
                    lastNs = ns
                }
            }
        }
    }

    val effectiveGlobalTime = if (isPlaying) currentTimeSec else currentTimeSec + ambientClockSec

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 9:16 Vertical Canvas Preview
            Box(
                modifier = Modifier
                    .widthIn(max = maxCanvasWidth)
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, RoyalGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video_preview_canvas")
                ) {
                    val w = size.width.toInt()
                    val h = size.height.toInt()
                    if (w > 0 && h > 0) {
                        drawIntoCanvas { canvas ->
                            renderer.drawFrame(
                                canvas = canvas.nativeCanvas,
                                width = w,
                                height = h,
                                scene = scene,
                                sceneProgress = sceneProgress,
                                globalTimeSec = effectiveGlobalTime,
                                totalProgress = (currentTimeSec / totalDurationSec.coerceAtLeast(1f)),
                                visualSettings = visualSettings,
                                isPlaying = isPlaying
                            )
                        }
                    }
                }
            }

            // Seek Slider & Timecode
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Slider(
                    value = currentTimeSec.coerceIn(0f, totalDurationSec),
                    onValueChange = onSeek,
                    valueRange = 0f..totalDurationSec.coerceAtLeast(1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_seek_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SaffronAmber,
                        activeTrackColor = SaffronAmber,
                        inactiveTrackColor = StudioSurfaceElevated
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatSeconds(currentTimeSec)} / ${formatSeconds(totalDurationSec)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlateText
                    )
                    Text(
                        text = "${scene.speaker.hindiName} • ${scene.cameraShot.englishLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = DevanagariFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = RoyalGold
                    )
                }
            }

            // Playback Transport Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceElevated)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = "Restart preview",
                        tint = Color.White
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("play_pause_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SaffronAmber,
                        contentColor = MidnightSlate
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

private fun formatSeconds(sec: Float): String {
    val total = sec.toInt().coerceAtLeast(0)
    val mins = total / 60
    val rem = total % 60
    return String.format(Locale.US, "%02d:%02d", mins, rem)
}
