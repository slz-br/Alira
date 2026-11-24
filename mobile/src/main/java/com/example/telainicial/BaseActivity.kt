package com.example.telainicial

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

/**
 * Todas as telas herdam daqui. Assim, fonte, tamanho de texto, tamanho de botões
 * e leitor de tela valem automaticamente no app inteiro.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Frase falada quando a tela abre (só se o leitor de tela estiver ligado). */
    open val tituloTela: String? = null

    /** Chamado sempre que as preferências são (re)aplicadas na tela. */
    open fun aoAplicarAcessibilidade() {}

    // Assim que o layout é inflado já aplica, para a tela nunca "piscar" no tamanho antigo.
    override fun onContentChanged() {
        super.onContentChanged()
        AcessibilidadeManager.aplicar(this)
    }

    override fun onResume() {
        super.onResume()
        reaplicarAcessibilidade() // pega mudanças feitas na tela de Acessibilidade ao voltar
        Navegacao.atualizarAvatar(this) // foto do menu = foto do perfil, sempre em dia
        tituloTela?.let { LeitorDeTela.anunciar(this, it) }
    }

    override fun onPause() {
        super.onPause()
        LeitorDeTela.parar()
    }

    /** Reaplica fonte/tamanhos em toda a tela e avisa a tela para ajustar o que for dela. */
    fun reaplicarAcessibilidade() {
        AcessibilidadeManager.aplicar(this)
        aoAplicarAcessibilidade()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean =
        LeitorDeTela.processar(this, ev) { super@BaseActivity.dispatchTouchEvent(it) }
}
