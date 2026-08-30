package me.slz_br.alira.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.slz_br.alira.presentation.screens.*
import me.slz_br.alira.presentation.screens.HeartRate.HeartRateScreen
import me.slz_br.alira.presentation.screens.settings.AccessibilitySettingsScreen
import me.slz_br.alira.presentation.screens.settings.SettingsScreen

const val PERMISSION = android.Manifest.permission.BODY_SENSORS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = AliraRoutes.Home.route
            ) {
                composable(AliraRoutes.Home.route) {
                    WearApp(
                        onHeartRateClick = { navController.navigate(AliraRoutes.HeartRate.route) },
                        onGenerateReportClick = { navController.navigate(AliraRoutes.HealthReport.route) },
                        onSettingsClick = { navController.navigate(AliraRoutes.Settings.route) },
                        onInfoScreenClick = { navController.navigate(AliraRoutes.Info.route) }
                    )
                }
                composable(AliraRoutes.HeartRate.route) {
                    HeartRateScreen()
                }
                composable(AliraRoutes.HealthReport.route) {
                    GenerateReportScreen()
                }
                composable(AliraRoutes.Settings.route) {
                    SettingsScreen(
                        onAccessibilityClick = { navController.navigate(AliraRoutes.Accessibility.route) }
                    )
                }
                composable(AliraRoutes.Accessibility.route) {
                    AccessibilitySettingsScreen()
                }
                composable(AliraRoutes.Info.route) {
                    InfoScreen()
                }

            }
        }
    }
}