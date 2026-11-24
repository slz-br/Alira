package com.example.telainicial

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Leitor de tela do app (liga/desliga em Acessibilidade > Leitor de tela).
 *
 * Funciona como o TalkBack:
 *  - 1 toque em qualquer elemento  -> o app LÊ o elemento em voz alta (sem ativar)
 *  - toque de novo no MESMO elemento -> ativa (abre, seleciona, edita...)
 *  - rolar, arrastar sliders e abrir o menu com o dedo continuam normais
 *
 * Se o TalkBack do Android estiver ligado, este leitor fica quieto para não competir com ele.
 */
object LeitorDeTela {

    private const val JANELA_CONFIRMACAO_MS = 3000L

    // ----- voz -----
    private var tts: TextToSpeech? = null
    private var pronto = false
    private var pendente: String? = null

    // ----- estado do gesto -----
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var moveu = false
    private var multiToque = false
    private var ultimoAlvo: WeakReference<View>? = null
    private var ultimoToque = 0L

    // ===================== ESTADO =====================
    fun ativo(ctx: Context): Boolean = AcessibilidadeManager.leitorLigado(ctx) && !talkBackLigado(ctx)

    private fun talkBackLigado(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am != null && am.isEnabled && am.isTouchExplorationEnabled
    }

    // ===================== VOZ =====================
    private fun garantirVoz(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("pt", "BR"))
                pronto = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                if (pronto) pendente?.let { falarAgora(it) }
            }
            pendente = null
        }
    }

    private fun falarAgora(texto: String) {
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "alira_leitor_tela")
    }

    /** Fala somente se o leitor estiver ligado (use para avisos de tela, confirmações etc.). */
    fun anunciar(ctx: Context, texto: String) {
        if (ativo(ctx)) falarSempre(ctx, texto)
    }

    /** Fala mesmo com o leitor desligado (ex.: avisar "Leitor de tela desativado"). */
    fun falarSempre(ctx: Context, texto: String) {
        if (texto.isBlank()) return
        garantirVoz(ctx)
        if (!pronto) {
            pendente = texto
            return
        }
        falarAgora(texto)
    }

    fun parar() {
        pendente = null
        tts?.stop()
    }

    // ===================== GESTO =====================
    /**
     * Chame no dispatchTouchEvent da Activity.
     * [repassar] entrega o evento ao fluxo normal (super.dispatchTouchEvent).
     */
    fun processar(activity: Activity, ev: MotionEvent, repassar: (MotionEvent) -> Boolean): Boolean {
        if (!ativo(activity)) return repassar(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                downTime = ev.eventTime
                moveu = false
                multiToque = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> multiToque = true
            MotionEvent.ACTION_MOVE -> {
                val slop = ViewConfiguration.get(activity).scaledTouchSlop
                if (Math.hypot((ev.rawX - downX).toDouble(), (ev.rawY - downY).toDouble()) > slop) moveu = true
            }
            MotionEvent.ACTION_UP -> {
                val foiToque = !moveu && !multiToque &&
                        (ev.eventTime - downTime) < ViewConfiguration.getLongPressTimeout()
                if (foiToque) {
                    val alvo = encontrarAlvo(activity, ev.rawX, ev.rawY)
                    if (alvo != null) {
                        val agora = SystemClock.uptimeMillis()
                        val confirmou = alvo === ultimoAlvo?.get() && (agora - ultimoToque) < JANELA_CONFIRMACAO_MS

                        if (confirmou) {
                            // segundo toque no mesmo elemento: deixa o clique acontecer normalmente
                            ultimoAlvo = null
                        } else {
                            ultimoAlvo = WeakReference(alvo)
                            ultimoToque = agora
                            falarSempre(activity, descrever(alvo))

                            // transforma o "levantar o dedo" em CANCEL: nada é clicado neste 1º toque
                            val cancelar = MotionEvent.obtain(ev)
                            cancelar.action = MotionEvent.ACTION_CANCEL
                            repassar(cancelar)
                            cancelar.recycle()
                            return true
                        }
                    }
                }
            }
        }
        return repassar(ev)
    }

    // ===================== QUEM FOI TOCADO =====================
    private fun encontrarAlvo(activity: Activity, x: Float, y: Float): View? {
        val raiz = activity.findViewById<View>(android.R.id.content) ?: return null
        var inicio: View = raiz

        // com o menu aberto, só o menu é "tocável" (o resto está sob a cortina escura)
        val drawer = activity.findViewById<DrawerLayout>(R.id.main)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            inicio = activity.findViewById(R.id.drawer_menu) ?: raiz
        }

        val folha = folhaEm(inicio, x.toInt(), y.toInt()) ?: return null

        // sobe pela hierarquia: um botão/cartão clicável vence o texto que está dentro dele
        var v: View? = folha
        var textoSolto: View? = null
        while (v != null) {
            if (ehDescrevivel(v)) {
                if (v.isClickable) return v
                if (textoSolto == null) textoSolto = v
            }
            if (v === inicio) break
            v = v.parent as? View
        }
        return textoSolto
    }

    private fun folhaEm(v: View, x: Int, y: Int): View? {
        if (!v.isShown) return null
        val r = Rect()
        if (!v.getGlobalVisibleRect(r) || !r.contains(x, y)) return null
        if (v is ViewGroup) {
            for (i in v.childCount - 1 downTo 0) {
                folhaEm(v.getChildAt(i), x, y)?.let { return it }
            }
        }
        return v
    }

    private fun ehDescrevivel(v: View): Boolean {
        if (v.id == R.id.drawer_menu) return false // o painel do menu é clicável só para "engolir" toques
        if (!v.contentDescription.isNullOrBlank()) return true
        if (v is EditText) return true
        if (v is TextView && v.text.isNotBlank()) return true
        if (v.isClickable) return coletarFilhos(v).isNotBlank()
        return false
    }

    // ===================== O QUE FALAR =====================
    private fun descrever(v: View): String {
        val cd = v.contentDescription?.toString()?.trim().orEmpty()

        val corpo = when {
            v is EditText -> {
                val rotulo = v.hint?.toString().orEmpty()
                val valor = v.text?.toString().orEmpty()
                if (valor.isBlank()) "$rotulo, vazio" else "$rotulo, $valor"
            }
            cd.isNotEmpty() -> cd
            v is TextView -> v.text.toString()
            v is ViewGroup -> coletarFilhos(v)
            else -> ""
        }.trim()

        val extra = if (v is SeekBar && v.max > 0) ", ${v.progress * 100 / v.max} por cento" else ""

        return when {
            v is EditText -> "$corpo, campo de texto. Toque novamente para editar."
            v.isClickable -> "$corpo$extra, botão. Toque novamente para ativar."
            else -> "$corpo$extra"
        }
    }

    /** Junta os textos de dentro de um cartão; se não houver texto, usa as descrições dos ícones. */
    private fun coletarFilhos(grupo: View): String {
        if (grupo !is ViewGroup) return ""
        val textos = mutableListOf<String>()
        val descricoes = mutableListOf<String>()

        fun varrer(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView && v.text.isNotBlank()) textos.add(v.text.toString().trim())
            else if (v is ImageView && !v.contentDescription.isNullOrBlank()) descricoes.add(v.contentDescription.toString())
            if (v is ViewGroup) for (i in 0 until v.childCount) varrer(v.getChildAt(i))
        }
        for (i in 0 until grupo.childCount) varrer(grupo.getChildAt(i))

        return (if (textos.isNotEmpty()) textos else descricoes).joinToString(", ")
    }
}
