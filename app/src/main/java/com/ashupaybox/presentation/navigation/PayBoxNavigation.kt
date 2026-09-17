package com.ashupaybox.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ashupaybox.PayBoxApp
import com.ashupaybox.presentation.analytics.AnalyticsScreen
import com.ashupaybox.presentation.analytics.AnalyticsViewModel
import com.ashupaybox.presentation.diagnostic.DiagnosticScreen
import com.ashupaybox.presentation.diagnostic.DiagnosticViewModel
import com.ashupaybox.presentation.home.HomeScreen
import com.ashupaybox.presentation.home.HomeViewModel
import com.ashupaybox.presentation.onboarding.OnboardingScreen
import com.ashupaybox.presentation.reliability.ReliabilityScreen
import com.ashupaybox.presentation.settings.SettingsScreen
import com.ashupaybox.presentation.settings.SettingsViewModel
import com.ashupaybox.presentation.testpayment.TestPaymentScreen
import com.ashupaybox.presentation.testpayment.TestPaymentViewModel
import com.ashupaybox.presentation.theme.EmeraldGreen
import com.ashupaybox.presentation.transactions.TransactionsScreen
import com.ashupaybox.presentation.transactions.TransactionsViewModel

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Transactions : BottomNavItem(Screen.Transactions.route, "History", Icons.AutoMirrored.Filled.ListAlt)
    object Analytics : BottomNavItem(Screen.Analytics.route, "Analytics", Icons.Default.Analytics)
    object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun PayBoxNavigation(
    app: PayBoxApp,
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Transactions,
        BottomNavItem.Analytics,
        BottomNavItem.Settings
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    color = if (selected) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = EmeraldGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar && currentRoute != Screen.TestPayment.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.TestPayment.route) },
                    containerColor = EmeraldGreen,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Simulate Payment")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSimulator = { navController.navigate(Screen.TestPayment.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) }
                )
            }

            composable(Screen.Transactions.route) {
                val txViewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.factory(app))
                TransactionsScreen(viewModel = txViewModel)
            }

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.factory(app))
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToDiagnostic = { navController.navigate(Screen.SoundBoxDiagnostic.route) },
                    onNavigateToReliability = { navController.navigate(Screen.ReliabilityCheck.route) },
                    onNavigateToAppUpdate = { navController.navigate(Screen.AppUpdate.route) }
                )
            }

            composable(Screen.AppUpdate.route) {
                com.ashupaybox.presentation.update.AppUpdateScreen(
                    updateManager = app.updateManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TestPayment.route) {
                val testPaymentViewModel: TestPaymentViewModel = viewModel(factory = TestPaymentViewModel.factory(app))
                TestPaymentScreen(viewModel = testPaymentViewModel)
            }

            composable(Screen.SoundBoxDiagnostic.route) {
                val diagnosticViewModel: DiagnosticViewModel = viewModel(factory = DiagnosticViewModel.factory(app))
                DiagnosticScreen(
                    viewModel = diagnosticViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ReliabilityCheck.route) {
                ReliabilityScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Global in-app update popup when update is detected
        val updateCheckState by app.updateManager.updateCheckState.collectAsState()
        val currentVersionName = app.updateManager.getInstalledVersionName()
        (updateCheckState as? com.ashupaybox.core.update.UpdateCheckResult.UpdateAvailable)?.let { available ->
            com.ashupaybox.presentation.update.UpdateDialog(
                updateInfo = available.updateInfo,
                currentVersionName = currentVersionName,
                updateManager = app.updateManager,
                onDismiss = { app.updateManager.dismissUpdateDialog() }
            )
        }
    }
}
