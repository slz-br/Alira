package me.slz_br.alira.presentation.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.services.client.data.DataTypeAvailability
import kotlinx.coroutines.launch
import me.slz_br.alira.data.repository.HealthServiceRepository
import me.slz_br.alira.data.repository.LeituraCardiaca
import me.slz_br.alira.data.repository.LeituraDiaria
import me.slz_br.alira.monitoramento.MonitorCardiaco

enum class StatusMetrica {
    AGUARDANDO,
    OK,
    ADQUIRINDO,
    INDISPONIVEL,
    NAO_SUPORTADO,
    SEM_PERMISSAO,
    ERRO,
}

data class EstadoSinaisVitais(
    val bpm: Double? = null,
    val statusBpm: StatusMetrica = StatusMetrica.AGUARDANDO,
    val calorias: Double? = null,
    val passos: Long? = null,
    val distanciaMetros: Double? = null,
    val statusDiarias: StatusMetrica = StatusMetrica.AGUARDANDO,
    val detalhe: String? = null,
    // Últimos batimentos recebidos (até 40), para o gráfico da tela.
    val historicoBpm: List<Float> = emptyList(),
)

class VitalScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = HealthServiceRepository(application)

    var estado by mutableStateOf(EstadoSinaisVitais())
        private set

    init {
        observarFrequenciaCardiaca()
        observarMetricasDiarias()
    }

    private fun observarFrequenciaCardiaca() {
        viewModelScope.launch {
            repositorio.fluxoFrequenciaCardiaca().collect { leitura ->
                estado = when (leitura) {
                    is LeituraCardiaca.Bpm -> {
                        // Também passa pelo MonitorCardiaco: grava no banco e
                        // vigia batimento fora do normal enquanto a tela está aberta.
                        MonitorCardiaco.novaLeitura(getApplication<Application>(), leitura.valor.toFloat())
                        estado.copy(
                            bpm = leitura.valor,
                            statusBpm = StatusMetrica.OK,
                            historicoBpm = (estado.historicoBpm + leitura.valor.toFloat()).takeLast(40),
                        )
                    }

                    is LeituraCardiaca.Disponibilidade -> estado.copy(
                        statusBpm = when (leitura.estado) {
                            DataTypeAvailability.AVAILABLE ->
                                if (estado.bpm != null) StatusMetrica.OK else StatusMetrica.ADQUIRINDO
                            DataTypeAvailability.ACQUIRING -> StatusMetrica.ADQUIRINDO
                            else -> StatusMetrica.INDISPONIVEL
                        }
                    )

                    is LeituraCardiaca.Falha ->
                        estado.copy(statusBpm = StatusMetrica.ERRO, detalhe = leitura.motivo)

                    LeituraCardiaca.NaoSuportado ->
                        estado.copy(statusBpm = StatusMetrica.NAO_SUPORTADO)
                }
            }
        }
    }

    private fun observarMetricasDiarias() {
        viewModelScope.launch {
            repositorio.fluxoMetricasDiarias().collect { leitura ->
                estado = when (leitura) {
                    is LeituraDiaria.Dados -> estado.copy(
                        calorias = leitura.calorias ?: estado.calorias,
                        passos = leitura.passos ?: estado.passos,
                        distanciaMetros = leitura.distanciaMetros ?: estado.distanciaMetros,
                        statusDiarias = StatusMetrica.OK,
                    )

                    is LeituraDiaria.Falha ->
                        estado.copy(statusDiarias = StatusMetrica.ERRO, detalhe = leitura.motivo)

                    LeituraDiaria.SemPermissao ->
                        estado.copy(statusDiarias = StatusMetrica.SEM_PERMISSAO)
                }
            }
        }
    }
}
