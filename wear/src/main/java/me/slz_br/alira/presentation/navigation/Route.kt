package me.slz_br.alira.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Define a rota para todas as telas do sistema
 */
@Serializable
sealed interface Route: NavKey {

    @Serializable
    data object Home: Route, NavKey

    @Serializable
    data object Settings: Route, NavKey

    @Serializable
    data object AccessibilitySettings: Route, NavKey
    // Configurações > Cuidador (dados que vêm do Perfil do celular)
    @Serializable
    data object Cuidador: Route, NavKey

    @Serializable
    data object Info: Route, NavKey

    @Serializable
    data object Vitals: Route, NavKey

    @Serializable
    data object Monitoramento: Route, NavKey

    @Serializable
    data object Report: Route, NavKey
}