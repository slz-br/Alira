package me.slz_br.alira.presentation

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import me.slz_br.alira.data.repository.HealthServiceRepository
import me.slz_br.alira.presentation.navigation.NavigationRoot

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
            NavigationRoot()
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