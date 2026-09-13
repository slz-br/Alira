package me.slz_br.alira.data.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import me.slz_br.alira.data.repository.AliraRepository

/**
 * Mesmo separador definido em data/sync/MobileDataLayerService.kt no
 * módulo :mobile. Duplicado aqui pelo mesmo motivo de Hashing.kt: os dois
 * módulos viram .apk separados, não compartilham classes automaticamente.
 */
private const val SEP = "\u0001"

private const val CAMINHO_ALERTA = "/alira/alerta"

/**
 * Recebe, em segundo plano, as mensagens enviadas pelo celular pareado —
 * funciona mesmo com o app fechado, porque é um Service registrado no
 * sistema (ver declaração em AndroidManifest.xml), não uma tela.
 *
 * onMessageReceived já roda fora da main thread (é assim que o próprio
 * WearableListenerService funciona), então dá para usar runBlocking aqui
 * sem travar a interface do relógio.
 */
class WearDataLayerListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/alira/usuario_cadastrado" -> tratarUsuarioCadastrado(event.data)
        }
    }

    private fun tratarUsuarioCadastrado(payload: ByteArray) {
        val campos = String(payload, Charsets.UTF_8).split(SEP)
        if (campos.size < 7) {
            Log.w("ALIRA_SYNC", "Mensagem de cadastro incompleta, ignorando: $campos")
            return
        }
        // Nao usa destructuring (val (a, b, ...) = lista) porque Kotlin so
        // gera component1()..component5() para List de fabrica -- com 7
        // campos precisa acessar por indice mesmo.
        val nome = campos[0]
        val dataNascimentoTexto = campos[1]
        val telefone = campos[2]
        val email = campos[3]
        val deficiencia = campos[4]
        val nivelAutonomia = campos[5]
        val senhaHash = campos[6]

        runBlocking {
            val repository = AliraRepository.getInstance(applicationContext)
            repository.sincronizarUsuarioDoCelular(
                nome = nome,
                dataNascimento = dataNascimentoTexto.toLongOrNull(),
                telefone = telefone.ifBlank { null },
                email = email,
                deficiencia = deficiencia.ifBlank { null },
                nivelAutonomia = nivelAutonomia.ifBlank { null },
                senhaHash = senhaHash,
            )
        }
    }
}

/**
 * Envia um alerta recém-gerado para o celular pareado (melhor esforço,
 * mesmo raciocínio de enviarUsuarioParaRelogio() no :mobile: sem fila de
 * retentativa, só entrega se houver um nó conectado agora).
 */
fun enviarAlertaParaCelular(
    contexto: android.content.Context,
    tipoAlerta: String,
    descricao: String?,
    nivelUrgencia: String,
    dataHora: Long,
) {
    val texto = listOf(tipoAlerta, descricao ?: "", nivelUrgencia, dataHora.toString()).joinToString(SEP)
    val payload = texto.toByteArray(Charsets.UTF_8)

    val nodeClient = Wearable.getNodeClient(contexto)
    val messageClient = Wearable.getMessageClient(contexto)

    nodeClient.connectedNodes
        .addOnSuccessListener { nos ->
            nos.forEach { no ->
                messageClient.sendMessage(no.id, CAMINHO_ALERTA, payload)
                    .addOnFailureListener { erro ->
                        Log.w("ALIRA_SYNC", "Falha ao enviar alerta para ${no.displayName}", erro)
                    }
            }
        }
        .addOnFailureListener { erro ->
            Log.w("ALIRA_SYNC", "Não foi possível listar o celular pareado", erro)
        }
}
