package me.slz_br.alira.monitoramento

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda duas escolhas do aparelho:
 *  - se o "Monitoramento contínuo" está ligado;
 *  - se o "Modo demonstração" está ligado (detector de queda mais sensível,
 *    para dar certo caindo num colchão na apresentação).
 *
 * Usa SharedPreferences: um arquivinho de "chave = valor" que o Android
 * oferece para guardar configurações simples. Não vai para o banco Room
 * porque não é um dado do diagrama de classes, é só uma preferência do
 * aparelho (como "som ligado/desligado").
 *
 * Além do arquivo, cada valor fica num StateFlow, para as telas (Painel de
 * monitoramento, tela inicial) se atualizarem sozinhas quando ele muda.
 */
object PreferenciasMonitoramento {

    private const val ARQUIVO = "alira_preferencias"
    private const val CHAVE_CONTINUO = "monitoramento_continuo"
    private const val CHAVE_DEMONSTRACAO = "modo_demonstracao"

    private val _ligado = MutableStateFlow(false)
    private val _demonstracao = MutableStateFlow(false)

    @Volatile
    private var carregado = false

    /** Lê o arquivo uma vez só e copia os valores para os StateFlows. */
    private fun carregar(context: Context) {
        if (carregado) return
        synchronized(this) {
            if (carregado) return
            val arquivo = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            _ligado.value = arquivo.getBoolean(CHAVE_CONTINUO, false)
            _demonstracao.value = arquivo.getBoolean(CHAVE_DEMONSTRACAO, false)
            carregado = true
        }
    }

    /** "Monitoramento contínuo" ligado? (como fluxo, para as telas observarem) */
    fun ligado(context: Context): StateFlow<Boolean> {
        carregar(context)
        return _ligado.asStateFlow()
    }

    /** "Modo demonstração" ligado? (como fluxo, para as telas observarem) */
    fun modoDemonstracao(context: Context): StateFlow<Boolean> {
        carregar(context)
        return _demonstracao.asStateFlow()
    }

    fun estaLigado(context: Context): Boolean {
        carregar(context)
        return _ligado.value
    }

    fun demonstracaoLigada(context: Context): Boolean {
        carregar(context)
        return _demonstracao.value
    }

    fun definir(context: Context, ligado: Boolean) {
        carregar(context)
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE_CONTINUO, ligado)
            .apply()
        _ligado.value = ligado
    }

    fun definirModoDemonstracao(context: Context, ligado: Boolean) {
        carregar(context)
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE_DEMONSTRACAO, ligado)
            .apply()
        _demonstracao.value = ligado
    }
}
