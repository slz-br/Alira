package me.slz_br.alira.emergencia

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Os "sinais" que chamam a atenção da pessoa durante uma emergência:
 * vibração, som de alarme e voz. Quais deles são usados depende do
 * PerfilDeAlerta (ou seja, da deficiência da pessoa).
 *
 * Precisa ser criada na thread principal (a CentralDeEmergencia já roda
 * nela), por causa do TextToSpeech.
 */
class SinaisDeAlerta(context: Context, private val perfil: PerfilDeAlerta) {

    private val appContext = context.applicationContext

    private val vibrador: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private var alarme: Ringtone? = null

    // Voz: só é ligada para quem tem deficiência visual. O TextToSpeech
    // demora um pouco para ficar pronto; se pedirmos para falar antes
    // disso, o texto fica guardado em "falaPendente" e é dito assim que ele
    // terminar de carregar.
    private var voz: TextToSpeech? = null
    private var vozPronta = false
    private var falaPendente: String? = null

    init {
        if (perfil.usaVoz) {
            voz = TextToSpeech(appContext) { status ->
                vozPronta = status == TextToSpeech.SUCCESS
                if (vozPronta) {
                    voz?.setLanguage(Locale("pt", "BR"))
                    falaPendente?.let { falar(it) }
                    falaPendente = null
                } else {
                    Log.w(TAG, "Leitura em voz alta indisponível neste relógio")
                }
            }
        }
    }

    /** Liga os sinais (chamado quando a pergunta "Você está bem?" aparece). */
    fun iniciar(textoFalado: String) {
        vibrarSemParar()
        if (perfil.usaSom) tocarAlarme()
        if (perfil.usaVoz) falar(textoFalado, interromper = true)
    }

    /** Desliga vibração e alarme (a voz termina a frase que estiver falando). */
    fun parar() {
        vibrador.cancel()
        alarme?.stop()
        alarme = null
    }

    /** Libera tudo, inclusive a voz. Chamado só no fim da emergência. */
    fun encerrar() {
        parar()
        voz?.stop()
        voz?.shutdown()
        voz = null
    }

    /**
     * Fala o texto. Por padrão ele entra na FILA (espera a frase atual
     * terminar); com interromper = true, corta o que estiver sendo dito.
     */
    fun falar(texto: String, interromper: Boolean = false) {
        if (!perfil.usaVoz) return
        val tts = voz ?: return
        if (!vozPronta) {
            falaPendente = texto
            return
        }
        val modo = if (interromper) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(texto, modo, null, "alira_emergencia")
    }

    // Padrão: vibra 0,7 s, pausa 0,3 s, e repete (o "0" no final manda
    // recomeçar do início) até alguém chamar parar().
    //
    // O "USAGE_ALARM" diz ao Android que é uma vibração de alarme: sem
    // isso, o sistema pode ignorar vibrações de apps que estão em segundo
    // plano (o caso real de uma queda detectada com o app fechado).
    //
    // Com "Sinais de vibração" ligado (perfil.vibracaoForte), cada vibração
    // usa a força máxima do motor (255) em vez da força padrão.
    private fun vibrarSemParar() {
        val padrao = longArrayOf(0, 700, 300)
        val tipoAlarme = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        val efeito = if (perfil.vibracaoForte) {
            VibrationEffect.createWaveform(padrao, intArrayOf(0, 255, 0), 0)
        } else {
            VibrationEffect.createWaveform(padrao, 0)
        }
        vibrador.vibrate(efeito, tipoAlarme)
    }

    private fun tocarAlarme() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val toque = RingtoneManager.getRingtone(appContext, uri) ?: return
            toque.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            toque.isLooping = true
            toque.play()
            alarme = toque
        } catch (erro: Exception) {
            // Alguns relógios não têm alto-falante ou som de alarme
            // configurado; nesse caso fica só a vibração.
            Log.w(TAG, "Não foi possível tocar o alarme", erro)
        }
    }

    private companion object {
        const val TAG = "ALIRA_EMERGENCIA"
    }
}
