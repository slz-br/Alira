package me.slz_br.alira.presentation.screens.monitoramento

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.monitoramento.MonitoramentoContinuoService
import me.slz_br.alira.monitoramento.PreferenciasMonitoramento
import me.slz_br.alira.presentation.permissaoFrequenciaCardiaca

/**
 * O "controle remoto" do monitoramento contínuo, usado pelas telas.
 *
 * @param ligado        a chave "Monitoramento contínuo" está ligada?
 * @param demonstracao  a chave "Modo demonstração" está ligada?
 * @param aviso         mensagem para mostrar se faltou permissão (ou null).
 * @param alternar      ligar (true) ou desligar (false) o monitoramento.
 * @param alternarDemonstracao ligar/desligar o modo demonstração.
 */
class ControleMonitoramento(
    val ligado: Boolean,
    val demonstracao: Boolean,
    val aviso: String?,
    val alternar: (Boolean) -> Unit,
    val alternarDemonstracao: (Boolean) -> Unit,
)

/**
 * Cria o controle do monitoramento para uma tela. Cuida de tudo que ligar
 * o monitoramento exige: pedir as permissões (batimentos é obrigatória;
 * atividade física e notificações ajudam), salvar a escolha, ligar ou
 * desligar o MonitoramentoContinuoService e dar o retorno acessível
 * (vibrar / falar "Monitoramento contínuo ligado").
 *
 * O estado vem dos StateFlows de PreferenciasMonitoramento, então todas as
 * telas que usam o controle mostram sempre o mesmo valor.
 */
@Composable
fun rememberControleMonitoramento(): ControleMonitoramento {
    val context = LocalContext.current
    val ligado by remember { PreferenciasMonitoramento.ligado(context) }.collectAsState()
    val demonstracao by remember { PreferenciasMonitoramento.modoDemonstracao(context) }.collectAsState()
    var aviso by remember { mutableStateOf<String?>(null) }

    fun ligarDeVerdade() {
        PreferenciasMonitoramento.definir(context, true)
        MonitoramentoContinuoService.ligar(context)
        aviso = null
        FeedbackAcessivel.falar(context, "Monitoramento contínuo ligado")
    }

    val pedirPermissoes = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados[permissaoFrequenciaCardiaca] == true ||
            MonitoramentoContinuoService.temPermissaoDeBatimentos(context)
        ) {
            ligarDeVerdade()
        } else {
            val texto = "Sem a permissão de batimentos não dá para monitorar."
            aviso = texto
            FeedbackAcessivel.falar(context, texto)
        }
    }

    return ControleMonitoramento(
        ligado = ligado,
        demonstracao = demonstracao,
        aviso = aviso,
        alternar = { ligar ->
            FeedbackAcessivel.vibrarToque(context)
            if (ligar) {
                val necessarias = buildList {
                    add(permissaoFrequenciaCardiaca)
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                val faltando = necessarias.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (faltando.isEmpty()) ligarDeVerdade()
                else pedirPermissoes.launch(faltando.toTypedArray())
            } else {
                PreferenciasMonitoramento.definir(context, false)
                MonitoramentoContinuoService.desligar(context)
                FeedbackAcessivel.falar(context, "Monitoramento contínuo desligado")
            }
        },
        alternarDemonstracao = { ligar ->
            FeedbackAcessivel.vibrarToque(context)
            PreferenciasMonitoramento.definirModoDemonstracao(context, ligar)
            FeedbackAcessivel.falar(context, if (ligar) "Modo demonstração ligado" else "Modo demonstração desligado")
        },
    )
}
