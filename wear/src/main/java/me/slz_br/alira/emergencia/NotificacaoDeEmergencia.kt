package me.slz_br.alira.emergencia

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.slz_br.alira.R
import me.slz_br.alira.presentation.AlertaEmergenciaActivity

/**
 * Notificação da emergência.
 *
 * Por que precisa de notificação, se já existe a tela de alerta: quando a
 * queda/crise é detectada com o app FECHADO, o Android não deixa um app
 * abrir uma tela sozinho do nada. O caminho permitido é publicar uma
 * notificação de alta prioridade com "full screen intent" — é o mesmo
 * mecanismo que o despertador e as chamadas usam para acender a tela.
 * Se mesmo assim a tela não abrir, a notificação fica visível e, ao
 * tocar nela, a tela de alerta abre.
 */
object NotificacaoDeEmergencia {

    private const val CANAL = "alira_emergencia"
    private const val ID_NOTIFICACAO = 4242

    fun criarCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Alertas de emergência",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pergunta \"Você está bem?\" quando o relógio detecta uma queda ou crise."
            // Som e vibração NÃO vêm do canal: quem decide é a SinaisDeAlerta,
            // de acordo com a deficiência da pessoa (ex.: sem som para surdos).
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    /** Notificação que abre a tela "Você está bem?". */
    fun mostrarPergunta(context: Context, tipo: TipoEmergencia) {
        val abrirTela = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlertaEmergenciaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.heart_icon)
            .setContentTitle(tipo.titulo)
            .setContentText("${tipo.pergunta} Toque para responder.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(abrirTela)
            .setFullScreenIntent(abrirTela, true)
            .build()
        publicar(context, notificacao)
    }

    /** Notificação final, com o resultado (contato avisado ou falha). */
    fun mostrarResultado(context: Context, titulo: String, texto: String) {
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.heart_icon)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        publicar(context, notificacao)
    }

    fun remover(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_NOTIFICACAO)
    }

    // A permissão é conferida logo abaixo; o @SuppressLint só avisa o
    // Android Studio disso (ele não percebe a checagem sozinho).
    @SuppressLint("MissingPermission")
    private fun publicar(context: Context, notificacao: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("ALIRA_EMERGENCIA", "Sem permissão de notificação — só a tela/vibração vão avisar")
            return
        }
        criarCanal(context)
        NotificationManagerCompat.from(context).notify(ID_NOTIFICACAO, notificacao)
    }
}
