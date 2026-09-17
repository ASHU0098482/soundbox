package com.ashupaybox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ashupaybox.presentation.navigation.PayBoxNavigation
import com.ashupaybox.presentation.navigation.Screen
import com.ashupaybox.presentation.theme.AshuPayBoxTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PayBoxApp
        app.notificationManager.createNotificationChannels()

        setContent {
            val config by app.preferences.configFlow.collectAsState(initial = null)

            AshuPayBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    config?.let { currentConfig ->
                        val startDestination = if (currentConfig.isOnboardingCompleted) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }

                        PayBoxNavigation(
                            app = app,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as? PayBoxApp)?.notificationManager?.createNotificationChannels()
    }
}
