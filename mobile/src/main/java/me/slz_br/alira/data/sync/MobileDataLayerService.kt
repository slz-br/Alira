package me.slz_br.alira.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import me.slz_br.alira.data.db.entities.UsuarioEntity

/**
 * Separador de campos: o caractere de controle Unicode U+0001, escolhido
 * porque nunca aparece digitado num nome, e-mail ou telefone reais — por
 * isso e seguro usa-lo para depois recompor os campos com split() do
 * outro lado da mensagem.
 */
private const val SEP = "\u0001"

private const val CAMINHO_USUARIO_CADASTRADO = "/alira/usuario_cadastrado"

/**
 * Serializa os campos do Usuario num texto simples separado por SEP.
 *
 * Por que nao JSON/kotlinx.serialization: essa biblioteca ja e dependencia
 * do projeto, mas o plugin de compilador que gera o serializer de uma
 * classe @Serializable (org.jetbrains.kotlin.plugin.serialization) NAO
 * esta configurado em nenhum dos dois modulos — adicionar isso so para
 * este envio pontual seria mais uma peca nova de Gradle para dar errado.
 * Um texto com separador resolve o mesmo problema com zero dependencia
 * nova; para um payload maior/mais complexo no futuro, ai sim vale migrar
 * para JSON de verdade.
 */
private fun codificarUsuario(usuario: UsuarioEntity): ByteArray {
    val texto = listOf(
        usuario.nome,
        usuario.dataNascimento?.toString() ?: "",
        usuario.telefone ?: "",
        usuario.email,
        usuario.deficiencia ?: "",
        usuario.nivelAutonomia ?: "",
        usuario.senha, // ja e o hash, nunca a senha em texto puro
    ).joinToString(SEP)
    return texto.toByteArray(Charsets.UTF_8)
}

/**
 * Envia o usuario recem-cadastrado/atualizado para todos os relogios
 * pareados no momento. E "melhor esforco": se nenhum no estiver
 * conectado agora (relogio desligado/fora de alcance), a mensagem
 * simplesmente nao e entregue — nao ha fila/retentativa automatica com
 * MessageClient puro (para isso existiria o DataClient, ver capitulo 7.2
 * do guia). Por enquanto isso e aceitavel: o cadastro ja foi salvo no
 * banco do proprio celular; a sincronizacao com o relogio so habilita o
 * "login" automatico nele quando os dois estiverem por perto.
 */
fun enviarUsuarioParaRelogio(context: Context, usuario: UsuarioEntity) {
    val payload = codificarUsuario(usuario)
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)

    nodeClient.connectedNodes
        .addOnSuccessListener { nos ->
            nos.forEach { no ->
                messageClient.sendMessage(no.id, CAMINHO_USUARIO_CADASTRADO, payload)
                    .addOnFailureListener { erro ->
                        Log.w("ALIRA_SYNC", "Falha ao enviar usuario para ${no.displayName}", erro)
                    }
            }
        }
        .addOnFailureListener { erro ->
            Log.w("ALIRA_SYNC", "Nao foi possivel listar relogios pareados", erro)
        }
}

/**
 * Recebe mensagens vindas do relogio. Hoje so trata o alerta (RF03): o
 * relogio manda um aviso quando gera um alerta (ver AliraRepository.kt do
 * :wear); aqui isso so e logado. Numa versao futura, este e o lugar onde
 * entraria uma notificacao real (NotificationCompat) — deixado de fora de
 * proposito para nao misturar a peca de banco de dados com o fluxo de
 * permissao de notificacao (POST_NOTIFICATIONS), que e um assunto a parte.
 */
class MobileDataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/alira/alerta" -> {
                val texto = String(event.data, Charsets.UTF_8)
                val campos = texto.split(SEP)
                Log.i("ALIRA_SYNC", "Alerta recebido do relogio: $campos")
            }
        }
    }
}
