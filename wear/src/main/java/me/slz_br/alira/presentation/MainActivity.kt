package me.slz_br.alira.presentation

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.slz_br.alira.data.repository.HealthServiceRepository
import me.slz_br.alira.presentation.screens.*
import me.slz_br.alira.presentation.screens.HeartRate.HeartRateScreen
import me.slz_br.alira.presentation.screens.settings.AcessibilitySettingsScreen
import me.slz_br.alira.presentation.screens.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    private val pedidoDePermissaoSegundoPlano =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (concedida) {
                HealthServiceRepository(applicationContext).ligarMonitoramentoEmSegundoPlano()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepararMonitoramentoEmSegundoPlano()
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = AliraRoutes.Home.route
            ) {
                composable(AliraRoutes.Home.route) {
                    WearApp(
                        onHeartRateClick = { navController.navigate(AliraRoutes.HeartRate.route) },
                        onVitalsClick = { navController.navigate(AliraRoutes.Vitals.route) },
                        onGenerateReportClick = { navController.navigate(AliraRoutes.HealthReport.route) },
                        onSettingsClick = { navController.navigate(AliraRoutes.Settings.route) },
                        onInfoScreenClick = { navController.navigate(AliraRoutes.Info.route) }
                    )
                }
                composable(AliraRoutes.HeartRate.route) {
                    HeartRateScreen()
                }
                composable(AliraRoutes.Vitals.route) {
                    VitalsScreen()
                }
                composable(AliraRoutes.HealthReport.route) {
                    GenerateReportScreen()
                }
                composable(AliraRoutes.Settings.route) {
                    SettingsScreen(
                        onAcessibilityClick = { navController.navigate(AliraRoutes.Accessibility.route) }
                    )
                }
                composable(AliraRoutes.Accessibility.route) {
                    AcessibilitySettingsScreen()
                }
                composable(AliraRoutes.Info.route) {
                    InfoScreen()
                }

            }
        }
    }

    private fun prepararMonitoramentoEmSegundoPlano() {
        if (!temPermissao(permissaoFrequenciaCardiaca)) return

        if (temPermissao(permissaoSegundoPlano)) {
            HealthServiceRepository(applicationContext).ligarMonitoramentoEmSegundoPlano()
        } else {
            pedidoDePermissaoSegundoPlano.launch(permissaoSegundoPlano)
        }
    }

    private fun temPermissao(permissao: String): Boolean =
        ActivityCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED
}