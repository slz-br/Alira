package me.slz_br.alira.monitoramento

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import me.slz_br.alira.R
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.emergencia.TipoEmergencia
import me.slz_br.alira.presentation.MainActivity
import me.slz_br.alira.presentation.permissaoFrequenciaCardiaca

private const val TAG = "ALIRA_MONITORAMENTO"

/**
 * O "vigia" que fica ligado o tempo todo enquanto o botão "Monitoramento
 * contínuo" (Configurações) estiver ligado — mesmo com o app fechado e a
 * tela apagada.
 *
 * É um "foreground service" (serviço em primeiro plano): o Android só
 * deixa um app ler sensores sem parar, em segundo plano, se ele mostrar
 * uma notificação fixa avisando ("ALIRA monitorando"). Assim a pessoa
 * sempre sabe que o relógio está vigiando.
 *
 * O que ele faz:
 *  - Lê o ACELERÔMETRO ~50 vezes por segundo e entrega para o
 *    DetectorDeQueda e para o DetectorDeMovimento (parado ou não).
 *  - Lê o sensor de BATIMENTOS e entrega para o MonitorCardiaco (grava no
 *    banco e vigia batimento fora do normal).
 *  - A cada 5 s, entrega o batimento mais recente ao DetectorDeAnsiedade
 *    (junto com "a pessoa está parada?").
 *  - A cada 10 min, recalcula a média de batimentos da pessoa (últimos 7
 *    dias do banco), usada pelo detector de ansiedade.
 */
class MonitoramentoContinuoService : Service(), SensorEventListener {

    companion object {
        private const val CANAL = "alira_monitoramento"
        private const val ID_NOTIFICACAO = 4343
        private const val INTERVALO_VERIFICACAO_MS = 5_000L
        private const val BATIMENTO_VELHO_MS = 30_000L
        private const val INTERVALO_MEDIA_MS = 10 * 60_000L
        private const val INTERVALO_PAINEL_MS = 100L
        private const val PONTOS_NO_GRAFICO = 40
        private const val MOSTRAR_QUEDA_MS = 4_000L

        /** Liga o serviço (só se a permissão de batimentos já foi dada). */
        fun ligar(context: Context) {
            if (!temPermissaoDeBatimentos(context)) {
                Log.w(TAG, "Sem permissão de batimentos; monitoramento contínuo não iniciado")
                return
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, MonitoramentoContinuoService::class.java),
                )
            } catch (erro: Exception) {
                // Ex.: o Android não deixa iniciar com o app em segundo plano.
                Log.w(TAG, "Não foi possível iniciar o monitoramento contínuo", erro)
            }
        }

        fun desligar(context: Context) {
            context.stopService(Intent(context, MonitoramentoContinuoService::class.java))
        }

        fun temPermissaoDeBatimentos(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, permissaoFrequenciaCardiaca) ==
                PackageManager.PERMISSION_GRANTED
    }

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var sensorManager: SensorManager
    private var travaDeEnergia: PowerManager.WakeLock? = null

    private val detectorDeQueda = DetectorDeQueda {
        quedaDetectadaEm = SystemClock.elapsedRealtime()
        GatilhoDeEmergencia.disparar(this, TipoEmergencia.QUEDA)
    }
    private val detectorDeMovimento = DetectorDeMovimento()
    private val detectorDeAnsiedade = DetectorDeAnsiedade {
        GatilhoDeEmergencia.disparar(this, TipoEmergencia.ANSIEDADE)
    }

    private var ultimoBpm = 0f
    private var momentoDoUltimoBpm = 0L

    // Para o painel ao vivo: o maior pico de força desde a última
    // atualização da tela, o histórico do gráfico e quando foi a última queda.
    private var picoDaJanela = 0f
    private var ultimaPublicacao = 0L
    private val historicoForca = ArrayDeque<Float>()
    private var quedaDetectadaEm = 0L
    private var demonstracao = false

    // Não é um serviço "conectável" (bound): só liga e desliga.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!temPermissaoDeBatimentos(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            // O Android exige mostrar a notificação fixa nos primeiros
            // segundos depois de iniciar um foreground service.
            ServiceCompat.startForeground(
                this,
                ID_NOTIFICACAO,
                criarNotificacao(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } catch (erro: Exception) {
            Log.w(TAG, "O Android recusou o monitoramento em primeiro plano", erro)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!::sensorManager.isInitialized) comecarAVigiar()
        // START_STICKY: se o Android precisar matar o serviço por falta de
        // memória, ele o recria sozinho depois.
        return START_STICKY
    }

    private fun comecarAVigiar() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Trava de energia "parcial": mantém o processador acordado com a
        // tela apagada, senão o acelerômetro para de mandar amostras e uma
        // queda com a tela desligada passaria despercebida. Gasta mais
        // bateria — por isso o monitoramento contínuo é opcional.
        val energia = getSystemService(Context.POWER_SERVICE) as PowerManager
        travaDeEnergia = energia.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alira:monitoramento").apply {
            setReferenceCounted(false)
            acquire()
        }

        val acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.w(TAG, "Este relógio não tem acelerômetro: queda e ansiedade não serão detectadas")
        }
        val batimentos = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (batimentos != null) {
            sensorManager.registerListener(this, batimentos, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.w(TAG, "Este relógio não tem sensor de batimentos")
        }

        // A cada 5 s: batimento mais recente -> detector de ansiedade e MonitorCardiaco.
        // (O sensor de batimentos só avisa quando o valor MUDA; se o
        // batimento ficar parado em 120, sem esta repetição a contagem dos
        // 2 minutos não andaria.)
        escopo.launch {
            while (isActive) {
                delay(INTERVALO_VERIFICACAO_MS)
                val agora = SystemClock.elapsedRealtime()
                if (ultimoBpm > 0f && agora - momentoDoUltimoBpm <= BATIMENTO_VELHO_MS) {
                    MonitorCardiaco.novaLeitura(this@MonitoramentoContinuoService, ultimoBpm)
                    detectorDeAnsiedade.novaLeitura(ultimoBpm, detectorDeMovimento.estaParado(agora), agora)
                }
            }
        }

        // Modo demonstração: sempre que a chave muda, troca os limites do
        // detector de queda (NORMAL <-> DEMONSTRACAO), sem reiniciar o serviço.
        escopo.launch {
            PreferenciasMonitoramento.modoDemonstracao(this@MonitoramentoContinuoService).collect { ligado ->
                demonstracao = ligado
                detectorDeQueda.limites = if (ligado) LimitesQueda.DEMONSTRACAO else LimitesQueda.NORMAL
            }
        }

        // A cada 10 min: média de batimentos dos últimos 7 dias (banco).
        escopo.launch {
            val repository = AliraRepository.getInstance(this@MonitoramentoContinuoService)
            while (isActive) {
                val fim = System.currentTimeMillis()
                val inicio = fim - TimeUnit.DAYS.toMillis(7)
                val usuarioId = repository.garantirUsuarioLocal()
                detectorDeAnsiedade.mediaDaPessoa = repository.mediaFrequenciaCardiaca(usuarioId, inicio, fim)
                Log.d(TAG, "Limite de ansiedade agora: ${detectorDeAnsiedade.limiteAtual()} bpm")
                delay(INTERVALO_MEDIA_MS)
            }
        }
        Log.i(TAG, "Monitoramento contínuo ligado")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Todos os detectores usam o mesmo "relógio": milissegundos
                // desde que o aparelho ligou (não volta atrás se a pessoa
                // mudar a hora do relógio).
                val tempoMs = SystemClock.elapsedRealtime()
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                detectorDeQueda.novaAmostra(x, y, z, tempoMs)
                detectorDeMovimento.novaAmostra(x, y, z, tempoMs)
                atualizarPainel(x, y, z, tempoMs)
            }

            Sensor.TYPE_HEART_RATE -> {
                // Sem contato com a pele ou leitura não confiável: ignora.
                if (event.accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT ||
                    event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                ) return
                val bpm = event.values[0]
                if (bpm <= 0f) return
                ultimoBpm = bpm
                momentoDoUltimoBpm = SystemClock.elapsedRealtime()
                MonitorCardiaco.novaLeitura(this, bpm)
            }
        }
    }

    /**
     * Manda uma "foto" do monitoramento para o painel ao vivo, no máximo
     * 10 vezes por segundo (mais que isso só gastaria bateria redesenhando
     * a tela). Entre uma foto e outra guarda o MAIOR pico de força, para o
     * impacto de uma queda aparecer no gráfico mesmo se durar 1 amostra só.
     */
    private fun atualizarPainel(x: Float, y: Float, z: Float, tempoMs: Long) {
        val forcaG = sqrt(x * x + y * y + z * z) / DetectorDeQueda.G
        if (forcaG > picoDaJanela) picoDaJanela = forcaG
        if (tempoMs - ultimaPublicacao < INTERVALO_PAINEL_MS) return
        ultimaPublicacao = tempoMs

        historicoForca.addLast(picoDaJanela)
        while (historicoForca.size > PONTOS_NO_GRAFICO) historicoForca.removeFirst()
        val pico = picoDaJanela
        picoDaJanela = 0f

        val situacao = when {
            quedaDetectadaEm > 0L && tempoMs - quedaDetectadaEm < MOSTRAR_QUEDA_MS -> SituacaoQueda.QUEDA_DETECTADA
            detectorDeQueda.fase == DetectorDeQueda.Fase.ASSENTANDO -> SituacaoQueda.IMPACTO
            detectorDeQueda.fase == DetectorDeQueda.Fase.CONFERINDO_IMOBILIDADE -> SituacaoQueda.CONFERINDO
            else -> SituacaoQueda.VIGIANDO
        }
        MonitoramentoAoVivo.publicar(
            LeituraAoVivo(
                ativo = true,
                forcaG = pico,
                historicoForca = historicoForca.toList(),
                situacao = situacao,
                progresso = if (situacao == SituacaoQueda.QUEDA_DETECTADA) 1f else detectorDeQueda.progresso(tempoMs),
                parado = detectorDeMovimento.estaParado(tempoMs),
                bpm = if (tempoMs - momentoDoUltimoBpm <= BATIMENTO_VELHO_MS) ultimoBpm else 0f,
                limiteImpactoG = detectorDeQueda.limites.impactoG,
                demonstracao = demonstracao,
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        travaDeEnergia?.let { if (it.isHeld) it.release() }
        escopo.cancel()
        MonitoramentoAoVivo.limpar()
        Log.i(TAG, "Monitoramento contínuo desligado")
        super.onDestroy()
    }

    private fun criarNotificacao(): Notification {
        val canal = NotificationChannel(CANAL, "Monitoramento contínuo", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Aviso fixo enquanto o ALIRA vigia quedas e crises de ansiedade."
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)

        val abrirApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.heart_icon)
            .setContentTitle("ALIRA monitorando")
            .setContentText("Vigiando quedas, batimentos e crises de ansiedade")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(abrirApp)
            .build()
    }
}
