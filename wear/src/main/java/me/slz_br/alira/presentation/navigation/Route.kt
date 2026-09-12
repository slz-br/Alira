package me.slz_br.alira.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {

    @Serializable
    data object Home: Route, NavKey

    @Serializable
    data object Settings: Route, NavKey

    @Serializable
    data object AccessibilitySettings: Route, NavKey

    @Serializable
    data object Info: Route, NavKey

    @Serializable
    data object Vitals: Route, NavKey

    @Serializable
    data object Report: Route, NavKey
}