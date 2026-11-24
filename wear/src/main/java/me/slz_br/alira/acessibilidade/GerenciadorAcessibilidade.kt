package me.slz_br.alira.acessibilidade

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.slz_br.alira.data.repository.AliraRepository

/** As 3 chaves da tela de Acessibilidade, já lidas do banco. */
data class ConfigAcessibilidade(
    val altoContraste: Boolean = false,
    val vibracao: Boolean = false,
    val voz: Boolean = false,
)

/**
 * Guarda, na memória, a configuração de acessibilidade do usuário ativo e
 * avisa todo mundo quando ela muda.
 *
 * O banco (tabela acessibilidade) continua sendo o lugar onde a escolha
 * fica salva. Este objeto só fica "escutando" o banco e copia o valor para
 * um StateFlow, que qualquer parte do app consegue ler na hora, sem
 * esperar uma consulta: o tema (AliraTheme), o retorno de toque
 * (FeedbackAcessivel), a vibração dos alertas e a emergência.
 */
object GerenciadorAcessibilidade {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _config = MutableStateFlow(ConfigAcessibilidade())
    val config: StateFlow<ConfigAcessibilidade> = _config.asStateFlow()

    @Volatile
    private var iniciado = false

    /**
     * Começa a escutar o banco. Pode ser chamado várias vezes (por cada
     * tela, pelos serviços): só a primeira chamada faz alguma coisa.
     */
    fun iniciar(context: Context) {
        if (iniciado) return
        synchronized(this) {
            if (iniciado) return
            iniciado = true
        }
        val repository = AliraRepository.getInstance(context)
        escopo.launch {
            repository.garantirUsuarioLocal()
            repository.observarConfigAcessibilidade().collect { novo -> _config.value = novo }
        }
    }
}
