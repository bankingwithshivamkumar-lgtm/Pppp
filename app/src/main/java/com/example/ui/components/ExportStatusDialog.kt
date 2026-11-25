package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ExportState
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.StudioSurfaceElevated

@Composable
fun ExportStatusDialog(
    exportState: ExportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (exportState) {
        ExportState.Idle -> Unit

        is ExportState.Rendering -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = StudioSurfaceElevated,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = null,
                        tint = SaffronAmber,
                        modifier = Modifier.size(34.dp)
                    )
                },
                title = {
                    Text(
                        text = "Rendering MP4 Video",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = exportState.statusLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateText
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = SaffronAmber
                        )
                        Text(
                            text = "${(exportState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronAmber
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("cancel_export_button")
                    ) {
                        Text("Cancel", color = CrimsonAccent)
                    }
                }
            )
        }

        is ExportState.Success -> {
            val uri = Uri.parse(exportState.uriString)
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = StudioSurfaceElevated,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Reel Ready! (वीडियो तैयार)",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Saved to Movies/SamvadStudio/${exportState.displayName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(viewIntent, "Watch Video")
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("watch_exported_video_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SaffronAmber,
                                    contentColor = MidnightSlate
                                )
                            ) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                                Text(" Watch", modifier = Modifier.padding(start = 4.dp))
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Share Reel")
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_exported_video_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Text(" Share", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = SaffronAmber)
                    }
                }
            )
        }

        is ExportState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = StudioSurfaceElevated,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = CrimsonAccent,
                        modifier = Modifier.size(34.dp)
                    )
                },
                title = {
                    Text("Export Failed", style = MaterialTheme.typography.titleLarge)
                },
                text = {
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = SaffronAmber)
                    }
                }
            )
        }
    }
}
