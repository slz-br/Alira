package me.slz_br.alira.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.slz_br.alira.presentation.screens.*
import me.slz_br.alira.presentation.screens.monitoramento.MonitoramentoScreen
import me.slz_br.alira.presentation.screens.settings.AcessibilitySettingsScreen
import me.slz_br.alira.presentation.screens.settings.CuidadorScreen
import me.slz_br.alira.presentation.screens.settings.SettingsScreen

/**
 * Nó pai de todas as telas. Controla o fluxo entre as telas.
 */
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
                            onMonitoramentoClick = { backStack.add(Route.Monitoramento) },
                            onSettingsClick = { backStack.add(Route.Settings) },
                            onInfoScreenClick = { backStack.add(Route.Info) },
                            onVitalsClick = { backStack.add(Route.Vitals) },
                            onGenerateReportClick = { backStack.add(Route.Report) }
                        )
                    }
                }
                is Route.Monitoramento -> {
                    NavEntry(key) {
                        MonitoramentoScreen()
                    }
                }
                is Route.Vitals -> {
                    NavEntry(key) {
                        VitalsScreen()
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
                            onAcessibilityClick = { backStack.add(Route.AccessibilitySettings) },
                            onMonitoramentoClick = { backStack.add(Route.Monitoramento) },
                            onCuidadorClick = { backStack.add(Route.Cuidador) },
                        )
                    }
                }
                is Route.AccessibilitySettings -> {
                    NavEntry(key) {
                        AcessibilitySettingsScreen()
                    }
                }
                is Route.Cuidador -> {
                    NavEntry(key) {
                        CuidadorScreen()
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