package com.ashupaybox.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Transactions : Screen("transactions", "Transactions")
    object Analytics : Screen("analytics", "Analytics")
    object Settings : Screen("settings", "Settings")

    object TestPayment : Screen("test_payment", "Payment Simulator")
    object SoundBoxDiagnostic : Screen("soundbox_diagnostic", "SoundBox Diagnostic")
    object ReliabilityCheck : Screen("reliability_check", "Reliability & Permissions")
    object AppUpdate : Screen("app_update", "App Update")
    object Onboarding : Screen("onboarding", "Setup Wizard")
}
