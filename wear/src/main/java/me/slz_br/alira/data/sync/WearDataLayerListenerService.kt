package me.slz_br.alira.data.sync

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.emergencia.CentralDeEmergencia

/**
 * Mesmo separador definido em data/sync/MobileDataLayerService.kt no
 * módulo :mobile. Duplicado aqui pelo mesmo motivo de Hashing.kt: os dois
 * módulos viram .apk separados, não compartilham classes automaticamente.
 */
private const val SEP = "\u0001"

private const val CAMINHO_ALERTA = "/alira/alerta"

// Caminhos da emergência (queda / crise de ansiedade). Os mesmos textos
// existem em MobileDataLayerService.kt no :mobile.
private const val CAMINHO_EMERGENCIA = "/alira/emergencia"
private const val CAMINHO_EMERGENCIA_RESULTADO = "/alira/emergencia_resultado"

// Leituras para os gráficos do celular (o mesmo texto existe em
// MobileDataLayerService.kt no :mobile).
private const val CAMINHO_LEITURAS = "/alira/leituras"

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
            CAMINHO_EMERGENCIA_RESULTADO -> tratarResultadoEmergencia(event.data)
        }
    }

    /**
     * Resposta do celular depois de tentar mandar o SMS de emergência:
     * "OK" + nomes dos contatos avisados, ou "FALHA" + o motivo. Repassa
     * para a CentralDeEmergencia, que mostra o resultado na tela.
     */
    private fun tratarResultadoEmergencia(payload: ByteArray) {
        val campos = String(payload, Charsets.UTF_8).split(SEP)
        val sucesso = campos.getOrNull(0) == "OK"
        val detalhe = campos.getOrNull(1).orEmpty()
        CentralDeEmergencia.receberResultadoDoCelular(sucesso, detalhe)
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
        // Campos 7 a 10: o cuidador (o celular novo sempre manda; um celular
        // antigo não manda, por isso getOrNull + "" quando faltar).
        val cuidadorNome = campos.getOrNull(7).orEmpty()
        val cuidadorTelefone = campos.getOrNull(8).orEmpty()
        val cuidadorEmail = campos.getOrNull(9).orEmpty()
        val cuidadorParentesco = campos.getOrNull(10).orEmpty()

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
                cuidadorNome = cuidadorNome,
                cuidadorTelefone = cuidadorTelefone,
                cuidadorEmail = cuidadorEmail.ifBlank { null },
                cuidadorParentesco = cuidadorParentesco.ifBlank { null },
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

/**
 * Manda uma emergência confirmada para o celular pareado, que é quem envia
 * o SMS ao contato de emergência (ver MobileDataLayerService.kt).
 *
 * Diferente de enviarAlertaParaCelular() acima, esta função ESPERA a
 * resposta do sistema (Tasks.await) para saber se a mensagem saiu do
 * relógio. Devolve true se chegou a pelo menos um celular conectado;
 * false se não há celular conectado ou se o envio falhou — aí a tela do
 * relógio avisa a pessoa que não foi possível chamar o contato.
 *
 * Tasks.await trava a linha de execução enquanto espera, por isso esta
 * função só pode ser chamada fora da thread principal (a
 * CentralDeEmergencia chama dentro de withContext(Dispatchers.IO)).
 */
fun enviarEmergenciaParaCelular(contexto: android.content.Context, campos: List<String>): Boolean {
    val payload = campos.joinToString(SEP).toByteArray(Charsets.UTF_8)
    return try {
        val nos = Tasks.await(Wearable.getNodeClient(contexto).connectedNodes, 10, TimeUnit.SECONDS)
        var algumEntregue = false
        for (no in nos) {
            try {
                Tasks.await(
                    Wearable.getMessageClient(contexto).sendMessage(no.id, CAMINHO_EMERGENCIA, payload),
                    10,
                    TimeUnit.SECONDS,
                )
                algumEntregue = true
            } catch (erro: Exception) {
                Log.w("ALIRA_SYNC", "Falha ao enviar emergência para ${no.displayName}", erro)
            }
        }
        algumEntregue
    } catch (erro: Exception) {
        Log.w("ALIRA_SYNC", "Não foi possível falar com o celular pareado", erro)
        false
    }
}

/**
 * Manda um lote de leituras ao celular (ver AliraRepository.enviarDados()).
 * Mesmo jeito de enviarEmergenciaParaCelular(): ESPERA a resposta do
 * sistema para saber se saiu. Devolve true se chegou a pelo menos um
 * celular conectado.
 *
 * Trava a linha de execução enquanto espera: só pode ser chamada fora da
 * thread principal (enviarDados usa Dispatchers.IO).
 */
fun enviarLeiturasParaCelular(contexto: android.content.Context, texto: String): Boolean {
    val payload = texto.toByteArray(Charsets.UTF_8)
    return try {
        val nos = Tasks.await(Wearable.getNodeClient(contexto).connectedNodes, 10, TimeUnit.SECONDS)
        var algumEntregue = false
        for (no in nos) {
            try {
                Tasks.await(
                    Wearable.getMessageClient(contexto).sendMessage(no.id, CAMINHO_LEITURAS, payload),
                    10,
                    TimeUnit.SECONDS,
                )
                algumEntregue = true
            } catch (erro: Exception) {
                Log.w("ALIRA_SYNC", "Falha ao enviar leituras para ${no.displayName}", erro)
            }
        }
        algumEntregue
    } catch (erro: Exception) {
        Log.w("ALIRA_SYNC", "Não foi possível falar com o celular pareado", erro)
        false
    }
}
