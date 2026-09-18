package com.ashupaybox.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashupaybox.core.util.AnnouncementFormat
import com.ashupaybox.core.util.VoiceLanguage
import com.ashupaybox.data.repository.BackendSyncStatus
import com.ashupaybox.presentation.theme.AmberGold
import com.ashupaybox.presentation.theme.EmeraldGreen
import com.ashupaybox.presentation.theme.ErrorRed

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToReliability: () -> Unit,
    onNavigateToAppUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val backendStatus by viewModel.backendSyncStatus.collectAsState()
    val backendLastError by viewModel.backendLastError.collectAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER
        item {
            Column {
                Text(
                    text = "Settings & Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure merchant voice, alerts, and system reliability",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. QUICK ACTION CARDS (Diagnostic, Reliability & Update)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDiagnostic() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.HealthAndSafety, contentDescription = null, tint = EmeraldGreen)
                        Column {
                            Text(text = "SoundBox Health Diagnostic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Run 7-point hardware & speech test", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = EmeraldGreen)
                            Column {
                                Text(text = "Business Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = viewModel.backendUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        BackendStatusPill(backendStatus, viewModel.isBackendPaired)
                    }

                    Text(
                        text = when {
                            viewModel.isBackendPaired -> "This SoundBox is paired with your website backend."
                            backendStatus == BackendSyncStatus.CONNECTING -> "Connecting to backend..."
                            else -> "Enter pairing code from website admin to connect this device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!viewModel.isBackendPaired) {
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { value -> pairingCode = value.filter { it.isDigit() }.take(6) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Pairing Code") },
                            singleLine = true
                        )
                    }

                    if (backendLastError.isNotBlank()) {
                        Text(
                            text = backendLastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.isBackendPaired) {
                                    viewModel.syncSoundBox { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.connectSoundBox(pairingCode) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        if (success) pairingCode = ""
                                    }
                                }
                            },
                            enabled = backendStatus != BackendSyncStatus.CONNECTING && (viewModel.isBackendPaired || pairingCode.length == 6),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (viewModel.isBackendPaired) "Sync Now" else "Connect")
                        }

                        if (viewModel.isBackendPaired) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectSoundBox() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReliability() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = AmberGold)
                        Column {
                            Text(text = "Reliability & Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "DND bypass, battery optimization & OEM autostart", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAppUpdate() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = EmeraldGreen)
                        Column {
                            Text(text = "App Update (GitHub Releases)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Check latest releases, changelog & install updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 3. SOUNDBOX ENGINE SETTINGS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "Sound & Voice Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    SettingSwitchRow(
                        title = "SoundBox Enabled",
                        subtitle = "Enable automatic payment sound announcements",
                        checked = config.isSoundBoxEnabled,
                        onCheckedChange = { viewModel.setSoundBoxEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Payment Voice",
                        subtitle = "Speak out amount in Indian rupees",
                        checked = config.isVoiceEnabled,
                        onCheckedChange = { viewModel.setVoiceEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Notifications",
                        subtitle = "Issue high-importance lockscreen payment alert",
                        checked = config.isNotificationEnabled,
                        onCheckedChange = { viewModel.setNotificationEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Vibration",
                        subtitle = "Haptic feedback when payment arrives",
                        checked = config.isVibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Announce Payment Method",
                        subtitle = "Announce 'via UPI', 'via Card', etc.",
                        checked = config.announcePaymentMethod,
                        onCheckedChange = { viewModel.setAnnouncePaymentMethod(it) }
                    )

                    SettingSwitchRow(
                        title = "Announce Missed Payments",
                        subtitle = "Speak payments that arrived while phone was offline",
                        checked = config.announceMissedPayments,
                        onCheckedChange = { viewModel.setAnnounceMissedPayments(it) }
                    )

                    SettingSwitchRow(
                        title = "Keep Active in Background",
                        subtitle = "Start foreground service for enhanced background reliability",
                        checked = config.keepServiceAlive,
                        onCheckedChange = { viewModel.setKeepServiceAlive(it) }
                    )

                    // LANGUAGE SELECTION
                    Text(text = "Voice Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceLanguage.entries.forEach { lang ->
                            val selected = config.language == lang
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // ANNOUNCEMENT TEMPLATE
                    Text(text = "Announcement Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnnouncementFormat.entries.forEach { format ->
                            val selected = config.format == format
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setAnnouncementFormat(format) },
                                label = { Text(format.displayName) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // VOLUME SLIDER
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Volume", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${(config.volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = config.volume,
                            onValueChange = { viewModel.setVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen)
                        )
                    }

                    // SPEECH RATE SLIDER
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Speech Speed", style = MaterialTheme.typography.bodyMedium)
                            Text(text = String.format(java.util.Locale.US, "%.1fx", config.speechRate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = config.speechRate,
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen)
                        )
                    }

                    // TEST ANNOUNCEMENT BUTTON
                    Button(
                        onClick = { viewModel.testAnnouncement() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Test Sound (\"Payment received. ₹300.\")", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. DEVELOPER & DEMO MODE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "Developer & Demo Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    SettingSwitchRow(
                        title = "Enable Test Mode",
                        subtitle = "Allows simulator transactions",
                        checked = config.isTestModeEnabled,
                        onCheckedChange = { viewModel.setTestModeEnabled(it) }
                    )

                    SettingSwitchRow(
                        title = "Include Test Transactions in Revenue",
                        subtitle = "Show simulated transactions on main dashboard",
                        checked = config.includeTestInRevenue,
                        onCheckedChange = { viewModel.setIncludeTestInRevenue(it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.seedDemoTransactions {
                                    Toast.makeText(context, "Demo transactions seeded!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seed Demo")
                        }

                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Test")
                        }
                    }
                }
            }
        }

        // 5. ABOUT ASHU PAYBOX
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = EmeraldGreen)
                        Text(text = "About Ashu PayBox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(text = "Digital Merchant Payment SoundBox", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "App Version: 1.1.0 (Phase 3)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Installation ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Installation Device ID (Privacy-Safe)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = viewModel.deviceId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = {
                            val clip = ClipData.newPlainText("Device ID", viewModel.deviceId)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldGreen)
                        }
                    }
                }
            }
        }
    }

    // CLEAR TEST DATA CONFIRMATION DIALOG
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Test Data?") },
            text = { Text("This will permanently delete simulated test transactions. Real production records will NOT be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearTestData { deletedCount ->
                            Toast.makeText(context, "Deleted $deletedCount test transactions", Toast.LENGTH_SHORT).show()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text("Clear", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BackendStatusPill(status: BackendSyncStatus, paired: Boolean) {
    val (text, color) = when {
        paired && status != BackendSyncStatus.FAILED -> "PAIRED" to EmeraldGreen
        status == BackendSyncStatus.CONNECTING -> "SYNCING" to AmberGold
        status == BackendSyncStatus.FAILED -> "FAILED" to ErrorRed
        status == BackendSyncStatus.PAIRING_REQUIRED -> "PAIRING" to AmberGold
        else -> "OFFLINE" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldGreen,
                checkedTrackColor = EmeraldGreen.copy(alpha = 0.4f)
            )
        )
    }
}
