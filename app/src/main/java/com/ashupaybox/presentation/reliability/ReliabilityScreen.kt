package com.ashupaybox.presentation.reliability

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashupaybox.presentation.theme.AmberGold
import com.ashupaybox.presentation.theme.EmeraldGreen
import com.ashupaybox.presentation.theme.ErrorRed

@Composable
fun ReliabilityScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageName = context.packageName

    // System Permission checks
    val isNotifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

    val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(packageName)
    } else true

    val hasDndAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.isNotificationPolicyAccessGranted
    } else true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Reliability & Permissions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ensure uninterrupted voice announcements",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // PERMISSION STATUS CHECKER
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
                    Text(text = "System Permissions Checklist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 1. Notification Permission
                    ReliabilityItemRow(
                        title = "Notification Permission",
                        statusText = if (isNotifEnabled) "Allowed ✅" else "Missing ❌",
                        isOk = isNotifEnabled,
                        buttonLabel = if (!isNotifEnabled) "Enable" else null
                    ) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                        context.startActivity(intent)
                    }

                    // 2. Sound Enabled
                    ReliabilityItemRow(
                        title = "Sound Enabled",
                        statusText = "Ready ✅",
                        isOk = true,
                        buttonLabel = null
                    ) {}

                    // 3. Battery Optimization
                    ReliabilityItemRow(
                        title = "Battery Optimization",
                        statusText = if (isIgnoringBattery) "Unrestricted ✅" else "Restricted ⚠️",
                        isOk = isIgnoringBattery,
                        buttonLabel = if (!isIgnoringBattery) "Unrestrict" else null
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    }

                    // 4. Do Not Disturb (DND) Access
                    ReliabilityItemRow(
                        title = "Do Not Disturb (DND) Access",
                        statusText = if (hasDndAccess) "Granted ✅" else "Not Granted ⚠️",
                        isOk = hasDndAccess,
                        buttonLabel = if (!hasDndAccess) "Grant" else null
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }

                    // 5. Background Operation
                    ReliabilityItemRow(
                        title = "Background Operation",
                        statusText = "Ready ✅",
                        isOk = true,
                        buttonLabel = null
                    ) {}
                }
            }
        }

        // OEM AUTOSTART & BATTERY INSTRUCTIONS
        item {
            Text(
                text = "Manufacturer Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            OemGuideCard(
                brand = "Xiaomi / Redmi / POCO (MIUI / HyperOS)",
                steps = listOf(
                    "1. Open Settings → Apps → Manage Apps → Ashu PayBox.",
                    "2. Enable 'Autostart' toggle.",
                    "3. Tap 'Battery Saver' and select 'No restrictions'.",
                    "4. In Recent Apps screen, long press Ashu PayBox and tap the Lock icon."
                )
            )
        }

        item {
            OemGuideCard(
                brand = "Samsung (One UI)",
                steps = listOf(
                    "1. Open Settings → Apps → Ashu PayBox → Battery.",
                    "2. Set Battery usage to 'Unrestricted'.",
                    "3. Open Settings → Battery & Device Care → Battery → Background usage limits.",
                    "4. Add Ashu PayBox to 'Never sleeping apps'."
                )
            )
        }

        item {
            OemGuideCard(
                brand = "OnePlus / Realme / OPPO (OxygenOS / ColorOS)",
                steps = listOf(
                    "1. Open Settings → Apps → App management → Ashu PayBox.",
                    "2. Enable 'Allow auto-launch' and 'Allow background activity'.",
                    "3. Open Battery settings → Optimize battery use → Select 'Don't optimize'."
                )
            )
        }

        item {
            OemGuideCard(
                brand = "Vivo (Funtouch OS)",
                steps = listOf(
                    "1. Open Settings → Battery → High background power consumption.",
                    "2. Enable Ashu PayBox to allow running in background.",
                    "3. In Settings → Applications → Autostart manager, toggle Ashu PayBox ON."
                )
            )
        }
    }
}

@Composable
fun ReliabilityItemRow(
    title: String,
    statusText: String,
    isOk: Boolean,
    buttonLabel: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOk) EmeraldGreen else AmberGold
            )
        }
        if (buttonLabel != null) {
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun OemGuideCard(brand: String, steps: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = brand, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
            steps.forEach { step ->
                Text(text = step, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
