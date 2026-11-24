package me.slz_br.alira.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import me.slz_br.alira.acessibilidade.GerenciadorAcessibilidade
import me.slz_br.alira.data.repository.HealthServiceRepository
import me.slz_br.alira.emergencia.NotificacaoDeEmergencia
import me.slz_br.alira.monitoramento.MonitoramentoContinuoService
import me.slz_br.alira.monitoramento.PreferenciasMonitoramento
import me.slz_br.alira.presentation.navigation.NavigationRoot

class MainActivity : ComponentActivity() {

    private val pedidoDePermissaoSegundoPlano =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            if (concedida) {
                HealthServiceRepository(applicationContext).ligarMonitoramentoEmSegundoPlano()
            }
        }

    // Permissão de notificação: é por uma notificação que a pergunta
    // "Você está bem?" consegue aparecer quando o app está fechado (ver
    // emergencia/NotificacaoDeEmergencia.kt). Os pedidos de permissão são
    // feitos um depois do outro (notificação -> segundo plano), porque o
    // Android ignora um segundo pedido se ele for feito ao mesmo tempo.
    private val pedidoDePermissaoNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            prepararMonitoramentoEmSegundoPlano()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificacaoDeEmergencia.criarCanal(this)
        GerenciadorAcessibilidade.iniciar(this)
        // Se o "Monitoramento contínuo" estava ligado e o Android parou o
        // serviço por algum motivo, abrir o app liga de novo.
        if (PreferenciasMonitoramento.estaLigado(this)) {
            MonitoramentoContinuoService.ligar(this)
        }
        pedirPermissaoDeNotificacao()
        setContent {
            NavigationRoot()
        }
    }

    private fun pedirPermissaoDeNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !temPermissao(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            pedidoDePermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            prepararMonitoramentoEmSegundoPlano()
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
