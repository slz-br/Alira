package me.slz_br.alira.acessibilidade

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * O "retorno" que o app dá quando a pessoa toca em algo:
 *  - Sinais de vibração ligado -> um toque curto de vibração (como um clique).
 *  - Texto para voz ligado     -> o relógio fala o nome do botão ou o valor.
 *
 * Se as duas opções estiverem desligadas, nada acontece (o app fica como
 * era antes). A voz é criada uma vez só e fica pronta para o app todo.
 */
object FeedbackAcessivel {

    private const val TAG = "ALIRA_ACESSIBILIDADE"

    private var voz: TextToSpeech? = null
    private var vozPronta = false
    private var falaPendente: String? = null

    /** Chamado a cada toque: vibra e/ou fala [rotulo], conforme a configuração. */
    fun aoTocar(context: Context, rotulo: String) {
        vibrarToque(context)
        falar(context, rotulo)
    }

    /**
     * Fala [texto] em voz alta se "Texto para voz" estiver ligado.
     * [mesmoDesligado] = true serve para o momento em que a pessoa acabou
     * de ligar a voz (o banco ainda não terminou de salvar, mas já dá para
     * confirmar falando).
     */
    fun falar(context: Context, texto: String, mesmoDesligado: Boolean = false) {
        if (!mesmoDesligado && !GerenciadorAcessibilidade.config.value.voz) return
        val tts = voz ?: criarVoz(context)
        if (!vozPronta) {
            // A voz ainda está carregando: guarda o texto e fala quando ficar pronta.
            falaPendente = texto
            return
        }
        // QUEUE_FLUSH: corta a frase anterior. Quem toca rápido em vários
        // botões ouve só o último, e não uma fila atrasada.
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "alira_toque")
    }

    /** Vibração curtinha de "clique", só se "Sinais de vibração" estiver ligado. */
    fun vibrarToque(context: Context, mesmoDesligado: Boolean = false) {
        if (!mesmoDesligado && !GerenciadorAcessibilidade.config.value.vibracao) return
        vibrador(context).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    private fun criarVoz(context: Context): TextToSpeech {
        val nova = TextToSpeech(context.applicationContext) { status ->
            vozPronta = status == TextToSpeech.SUCCESS
            if (vozPronta) {
                voz?.setLanguage(Locale("pt", "BR"))
                falaPendente?.let { voz?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "alira_toque") }
            } else {
                Log.w(TAG, "Leitura em voz alta indisponível neste relógio")
            }
            falaPendente = null
        }
        voz = nova
        return nova
    }

    private fun vibrador(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}

/**
 * Atalho para as telas: devolve um onClick que primeiro dá o retorno
 * acessível (vibra / fala [rotulo]) e depois faz a [acao] do botão.
 *
 * Uso: Button(onClick = acaoAcessivel("Sinais vitais") { abrirTela() }) { ... }
 */
@Composable
fun acaoAcessivel(rotulo: String, acao: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        FeedbackAcessivel.aoTocar(context, rotulo)
        acao()
    }
}
