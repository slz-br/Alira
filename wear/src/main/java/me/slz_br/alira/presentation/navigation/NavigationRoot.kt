package me.slz_br.alira.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.slz_br.alira.presentation.data.HealthServiceRepository
import me.slz_br.alira.presentation.screens.AccessibilitySettingsScreen
import me.slz_br.alira.presentation.screens.GenerateReportScreen
import me.slz_br.alira.presentation.screens.InfoScreen
import me.slz_br.alira.presentation.screens.SettingsScreen
import me.slz_br.alira.presentation.screens.VitalsScreen
import me.slz_br.alira.presentation.screens.WearApp
import me.slz_br.alira.presentation.viewmodels.VitalScreenViewModel

@Composable
fun NavigationRoot() {
    val backStack = rememberNavBackStack(Route.Home)

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        entryProvider = { key ->
            when(key) {
                is Route.Home -> {
                    NavEntry(key) {
                        WearApp(
                            onSettingsClick = { backStack.add(Route.Settings) },
                            onInfoScreenClick = { backStack.add(Route.Info) },
                            onVitalsClick = { backStack.add(Route.Vitals) },
                            onGenerateReportClick = { backStack.add(Route.Report) }
                        )
                    }
                }
                is Route.Vitals -> {
                    NavEntry(key) {
                        VitalsScreen(VitalScreenViewModel(HealthServiceRepository(LocalContext.current)))
                    }
                }
                is Route.Report -> {
                    NavEntry(key) {
                        GenerateReportScreen()
                    }
                }
                is Route.Settings -> {
                    NavEntry(key) {
                        SettingsScreen(
                            onAccessibilityClick = { backStack.add(Route.AccessibilitySettings) }
                        )
                    }
                }
                is Route.AccessibilitySettings -> {
                    NavEntry(key) {
                        AccessibilitySettingsScreen()
                    }
                }
                is Route.Info -> {
                    NavEntry(key) {
                        InfoScreen()
                    }
                }
                else -> error("Unknown route")
            }
        }
    )
}