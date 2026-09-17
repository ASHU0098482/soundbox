package com.ashupaybox.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.core.util.VoiceLanguage
import com.ashupaybox.presentation.theme.AmberGold
import com.ashupaybox.presentation.theme.EmeraldGreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PayBoxApp
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 6

    var selectedLanguage by remember { mutableStateOf(VoiceLanguage.ENGLISH) }
    var testSoundPlayed by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Proceed regardless of grant
        currentStep = 3
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // STEP INDICATOR (Dots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                (1..totalSteps).forEach { step ->
                    val isCompleted = step < currentStep
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isCurrent) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) EmeraldGreen else if (isCompleted) EmeraldGreen.copy(alpha = 0.5f) else Color.DarkGray
                            )
                    )
                }
            }

            // DYNAMIC CONTENT
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 24.dp),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> OnboardingStepCard(
                        icon = Icons.Default.VolumeUp,
                        title = "Welcome to Ashu PayBox",
                        description = "Transform your Android smartphone into a high-reliability merchant payment SoundBox with instant voice announcements.",
                        content = {}
                    )

                    2 -> OnboardingStepCard(
                        icon = Icons.Default.Notifications,
                        title = "Enable Notifications",
                        description = "Required to display instantaneous payment alerts with full lockscreen visibility.",
                        content = {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        currentStep = 3
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black)
                            ) {
                                Text("Allow Notifications", fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    3 -> OnboardingStepCard(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Configure Voice Alerts",
                        description = "Select your preferred language for payment amount announcements.",
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VoiceLanguage.entries.forEach { lang ->
                                    val selected = selectedLanguage == lang
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            selectedLanguage = lang
                                            scope.launch { app.preferences.setLanguage(lang) }
                                        },
                                        label = { Text(lang.displayName) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldGreen,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    )

                    4 -> OnboardingStepCard(
                        icon = Icons.Default.DoNotDisturbOn,
                        title = "Priority & DND Access",
                        description = "Allow PayBox to announce received payments even when your device is set to Do Not Disturb.",
                        content = {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open DND Settings")
                            }
                        }
                    )

                    5 -> OnboardingStepCard(
                        icon = Icons.Default.BatteryAlert,
                        title = "Battery Reliability",
                        description = "Ensure Android does not kill or delay payment sound events during background operation.",
                        content = {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Review Battery Optimization")
                            }
                        }
                    )

                    6 -> OnboardingStepCard(
                        icon = if (testSoundPlayed) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        title = if (testSoundPlayed) "Your SoundBox is Ready!" else "Test SoundBox Audio",
                        description = if (testSoundPlayed)
                            "Congratulations! Your device is fully configured to announce merchant payments."
                        else
                            "Tap the button below to simulate receiving ₹300 and verify the voice sound engine.",
                        content = {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val testEvent = PaymentEvent(
                                            eventId = "onboard_test_${UUID.randomUUID().toString().substring(0, 6)}",
                                            paymentId = "pay_onboard_300",
                                            orderId = "order_onboard",
                                            amountPaise = 30000L, // ₹300
                                            status = PaymentStatus.CAPTURED,
                                            paymentMethod = PaymentMethod.UPI,
                                            payerName = "Merchant Test Payer",
                                            source = PaymentSource.LOCAL_TEST,
                                            isTest = true
                                        )
                                        app.eventProcessor.processEvent(testEvent)
                                        testSoundPlayed = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play ₹300 Test Payment", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // NAVIGATION BUTTONS (Back, Skip, Next / Finish)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                if (currentStep < totalSteps) {
                    Button(
                        onClick = { currentStep++ },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                app.preferences.setOnboardingCompleted(true)
                                onFinished()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
