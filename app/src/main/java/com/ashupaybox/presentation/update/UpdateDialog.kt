package com.ashupaybox.presentation.update

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashupaybox.core.update.AppUpdateInfo
import com.ashupaybox.core.update.DownloadProgress
import com.ashupaybox.core.update.DownloadStatus
import com.ashupaybox.core.update.InstallResult
import com.ashupaybox.core.update.UpdateManager
import com.ashupaybox.presentation.theme.EmeraldGreen
import com.ashupaybox.presentation.theme.ErrorRed

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    currentVersionName: String,
    updateManager: UpdateManager,
    onDismiss: () -> Unit
) {
    val progress by updateManager.downloadProgress.collectAsState()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && progress.status != DownloadStatus.DOWNLOADING) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = EmeraldGreen
                )
                Text(
                    text = if (updateInfo.isForceUpdate) "Update Required" else "Update Available",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Version badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Current: v$currentVersionName",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "New: v${updateInfo.versionName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Changelog
                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateInfo.changelog.ifBlank { "Performance improvements and bug fixes." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // DOWNLOAD PROGRESS UI
                when (progress.status) {
                    DownloadStatus.DOWNLOADING -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Downloading Update...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(progress.progressFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress.progressFraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = EmeraldGreen
                            )
                            val downloadedMb = progress.bytesDownloaded / (1024f * 1024f)
                            val totalMb = progress.totalBytes / (1024f * 1024f)
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DownloadStatus.VERIFYING -> {
                        Text(
                            text = "Verifying package integrity & signing certificate...",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DownloadStatus.READY_TO_INSTALL -> {
                        Text(
                            text = "Verification passed! Ready to install.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DownloadStatus.FAILED -> {
                        Text(
                            text = progress.errorMessage ?: "Download or verification failed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    else -> {}
                }
            }
        },
        confirmButton = {
            when (progress.status) {
                DownloadStatus.IDLE, DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    Button(
                        onClick = { updateManager.startDownload(updateInfo) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold)
                    }
                }

                DownloadStatus.DOWNLOADING -> {
                    OutlinedButton(
                        onClick = { updateManager.cancelDownload() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                DownloadStatus.READY_TO_INSTALL -> {
                    Button(
                        onClick = {
                            progress.downloadedFile?.let { file ->
                                when (val result = updateManager.launchInstaller(file)) {
                                    is InstallResult.Launched -> onDismiss()
                                    is InstallResult.PermissionRequired -> {
                                        Toast.makeText(context, "Please allow installation from this source", Toast.LENGTH_LONG).show()
                                        context.startActivity(result.settingsIntent)
                                    }
                                    is InstallResult.Error -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Install Now", fontWeight = FontWeight.Bold)
                    }
                }

                DownloadStatus.VERIFYING -> {}
            }
        },
        dismissButton = {
            if (!updateInfo.isForceUpdate && progress.status != DownloadStatus.DOWNLOADING) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
