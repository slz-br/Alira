package me.slz_br.alira.monitoramento

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** O que o painel mostra no anel do meio. */
enum class SituacaoQueda { VIGIANDO, IMPACTO, CONFERINDO, QUEDA_DETECTADA }

/**
 * Uma "foto" do monitoramento contínuo, atualizada umas 10 vezes por
 * segundo pelo MonitoramentoContinuoService.
 *
 * @param ativo           o serviço está rodando?
 * @param forcaG          força atual do movimento, em g (1 g = parado).
 * @param historicoForca  os últimos ~4 segundos de força (para o gráfico).
 * @param situacao        o que o detector de queda está fazendo agora.
 * @param progresso       de 0 a 1, quanto falta para confirmar uma queda.
 * @param parado          a pessoa está parada? (DetectorDeMovimento)
 * @param bpm             último batimento lido (0 = ainda sem leitura).
 * @param limiteImpactoG  limite de impacto em uso (2,5 g normal, 1,8 g demonstração).
 * @param demonstracao    o modo demonstração está ligado?
 */
data class LeituraAoVivo(
    val ativo: Boolean = false,
    val forcaG: Float = 1f,
    val historicoForca: List<Float> = emptyList(),
    val situacao: SituacaoQueda = SituacaoQueda.VIGIANDO,
    val progresso: Float = 0f,
    val parado: Boolean = false,
    val bpm: Float = 0f,
    val limiteImpactoG: Float = LimitesQueda.NORMAL.impactoG,
    val demonstracao: Boolean = false,
)

/**
 * O "placar ao vivo" do monitoramento: o serviço escreve, o Painel de
 * monitoramento (MonitoramentoScreen) lê. É um object (um só no app) com
 * um StateFlow, o mesmo padrão do GerenciadorAcessibilidade.
 */
object MonitoramentoAoVivo {

    private val _leitura = MutableStateFlow(LeituraAoVivo())
    val leitura: StateFlow<LeituraAoVivo> = _leitura.asStateFlow()

    fun publicar(nova: LeituraAoVivo) {
        _leitura.value = nova
    }

    /** Chamado quando o serviço para: o painel volta a mostrar "desligado". */
    fun limpar() {
        _leitura.value = LeituraAoVivo()
    }
}
