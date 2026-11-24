package com.example.telainicial.data.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking
import com.example.telainicial.data.db.entities.CuidadorEntity
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import com.example.telainicial.data.db.entities.UsuarioEntity
import com.example.telainicial.data.repository.AliraRepository

/**
 * Separador de campos: o caractere de controle Unicode U+0001, escolhido
 * porque nunca aparece digitado num nome, e-mail ou telefone reais — por
 * isso e seguro usa-lo para depois recompor os campos com split() do
 * outro lado da mensagem.
 */
private const val SEP = "\u0001"

private const val CAMINHO_USUARIO_CADASTRADO = "/alira/usuario_cadastrado"

// Leituras (batimento, passos, calorias, distância) que o relógio manda
// quando a pessoa toca em "Gerar relatório". O mesmo texto existe em
// WearDataLayerListenerService.kt no :wear.
private const val CAMINHO_LEITURAS = "/alira/leituras"

// Caminhos da emergência (queda / crise de ansiedade). Os mesmos textos
// existem em WearDataLayerListenerService.kt no :wear — os dois lados
// precisam usar exatamente o mesmo caminho para a mensagem chegar.
private const val CAMINHO_EMERGENCIA = "/alira/emergencia"
private const val CAMINHO_EMERGENCIA_RESULTADO = "/alira/emergencia_resultado"
private const val MOTIVO_PEDIU_AJUDA = "PEDIU_AJUDA"

/**
 * Transforma o usuário e o cuidador num texto simples, com os campos
 * separados por SEP, nesta ordem (o relógio lê na mesma ordem):
 *
 *   0 nome · 1 nascimento · 2 telefone · 3 e-mail · 4 deficiência ·
 *   5 nível de autonomia · 6 senha · 7 nome do cuidador ·
 *   8 telefone do cuidador · 9 e-mail do cuidador · 10 parentesco
 *
 * Campo vazio = "" (o relógio entende como "não informado").
 *
 * Por que não JSON: para poucos campos de texto, um separador resolve o
 * mesmo problema sem precisar de biblioteca nem plugin novo no Gradle.
 */
private fun codificarUsuario(usuario: UsuarioEntity, cuidador: CuidadorEntity?): ByteArray {
    val texto = listOf(
        usuario.nome,
        usuario.dataNascimento?.toString() ?: "",
        usuario.telefone ?: "",
        usuario.email,
        usuario.deficiencia ?: "",
        usuario.nivelAutonomia ?: "",
        usuario.senha,
        cuidador?.nome ?: "",
        cuidador?.telefone ?: "",
        cuidador?.email ?: "",
        cuidador?.parentesco ?: "",
    ).joinToString(SEP)
    return texto.toByteArray(Charsets.UTF_8)
}

/**
 * Envia o usuário e o cuidador para todos os relógios conectados agora.
 * É "melhor esforço": se o relógio estiver desligado ou longe, a mensagem
 * não é entregue (MessageClient não guarda fila). Não tem problema: os
 * dados já estão salvos no celular e são reenviados toda vez que a pessoa
 * sai da tela de Perfil.
 */
fun enviarUsuarioParaRelogio(context: Context, usuario: UsuarioEntity, cuidador: CuidadorEntity?) {
    val payload = codificarUsuario(usuario, cuidador)
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
 * Lê o pacote de leituras mandado pelo relógio. Uma leitura por linha, com
 * os campos separados por ";" nesta ordem:
 *
 *   número no relógio ; data/hora ; bpm ; passos ; calorias ; distância
 *
 * Campo vazio = sem valor (ex.: uma linha só de batimento não tem passos).
 * Linha estragada é pulada, para uma linha ruim não perder o pacote todo.
 */
internal fun decodificarLeituras(texto: String): List<MonitoramentoEntity> =
    texto.lineSequence().mapNotNull { linha ->
        val c = linha.split(';')
        if (c.size < 6) return@mapNotNull null
        val id = c[0].toLongOrNull() ?: return@mapNotNull null
        val dataHora = c[1].toLongOrNull() ?: return@mapNotNull null
        MonitoramentoEntity(
            idNoRelogio = id,
            dataHora = dataHora,
            frequenciaCardiaca = c[2].toDoubleOrNull(),
            passos = c[3].toLongOrNull(),
            calorias = c[4].toDoubleOrNull(),
            distancia = c[5].toDoubleOrNull(),
        )
    }.toList()

/**
 * Recebe mensagens vindas do relógio, mesmo com o app do celular fechado
 * (é um Service registrado no AndroidManifest.xml):
 *
 *  - /alira/alerta     -> aviso de alerta gerado (RF03); por enquanto só é
 *                         registrado no log.
 *  - /alira/emergencia -> emergência confirmada: manda SMS ao cuidador.
 *  - /alira/leituras   -> leituras para os gráficos: grava no banco.
 */
class MobileDataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/alira/alerta" -> {
                val texto = String(event.data, Charsets.UTF_8)
                val campos = texto.split(SEP)
                Log.i("ALIRA_SYNC", "Alerta recebido do relogio: $campos")
            }
            CAMINHO_EMERGENCIA -> tratarEmergencia(event)
            CAMINHO_LEITURAS -> tratarLeituras(event)
        }
    }

    /**
     * Leituras novas do relógio: grava no banco. A tela de Batimentos está
     * "escutando" a tabela (Flow), então o gráfico se atualiza sozinho se
     * estiver aberta.
     */
    private fun tratarLeituras(event: MessageEvent) {
        val leituras = decodificarLeituras(String(event.data, Charsets.UTF_8))
        val novas = runBlocking {
            AliraRepository.getInstance(applicationContext).salvarLeituras(leituras)
        }
        Log.i("ALIRA_SYNC", "Leituras recebidas do relógio: ${leituras.size} (novas: $novas)")
    }

    /**
     * Emergência confirmada pelo relógio (queda ou crise de ansiedade em que
     * a pessoa pediu ajuda ou não respondeu). Diferente do "/alira/alerta"
     * acima, que só é registrado: aqui o celular AGE — manda um SMS para
     * cada contato de emergência e responde ao relógio dizendo se deu certo.
     *
     * Por que SMS: funciona sem internet e o contato não precisa ter o app
     * instalado. O relógio não consegue falar direto com o celular do
     * contato — ele só conversa com o celular da própria pessoa (este app).
     *
     * onMessageReceived já roda fora da main thread (mesmo raciocínio do
     * WearDataLayerListenerService no :wear), por isso o runBlocking aqui
     * não trava a tela do celular.
     */
    private fun tratarEmergencia(event: MessageEvent) {
        val campos = String(event.data, Charsets.UTF_8).split(SEP)
        if (campos.size < 6) {
            Log.w("ALIRA_SYNC", "Mensagem de emergência incompleta, ignorando: $campos")
            return
        }
        val tipo = campos[0]
        val motivo = campos[1]
        val email = campos[2]
        val nome = campos[3].ifBlank { "a pessoa monitorada" }
        val dataHora = campos[4].toLongOrNull() ?: System.currentTimeMillis()
        val teste = campos[5] == "1"

        val resultado = runBlocking {
            val contatos = AliraRepository.getInstance(applicationContext).contatosDeEmergencia(email)
            enviarSmsDeEmergencia(contatos, montarMensagem(tipo, motivo, nome, dataHora, teste))
        }

        Log.i("ALIRA_SYNC", "Emergência $tipo ($motivo): sucesso=${resultado.first} -> ${resultado.second}")
        responderAoRelogio(event.sourceNodeId, resultado.first, resultado.second)
    }

    private fun montarMensagem(tipo: String, motivo: String, nome: String, dataHora: Long, teste: Boolean): String {
        val hora = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(dataHora))
        val evento = when (tipo) {
            "QUEDA" -> "uma possível queda"
            "ANSIEDADE" -> "uma possível crise de ansiedade (batimentos acelerados)"
            else -> "uma possível emergência"
        }
        val situacao = if (motivo == MOTIVO_PEDIU_AJUDA) {
            "A pessoa pediu ajuda pelo relógio."
        } else {
            "A pessoa não respondeu ao relógio."
        }
        val prefixo = if (teste) "[TESTE] " else ""
        return "${prefixo}ALIRA: o relógio de $nome detectou $evento às $hora. $situacao " +
            "Tente entrar em contato agora."
    }

    /**
     * Manda o SMS para todos os contatos. Devolve (deuCerto, detalhe):
     * o detalhe é o que aparece na tela do relógio — os nomes dos contatos
     * avisados, ou o motivo da falha.
     *
     * "Deu certo" aqui quer dizer que o celular entregou o SMS para a
     * operadora sem erro; não é confirmação de que chegou no aparelho do
     * contato (isso exigiria acompanhar o recibo de entrega).
     */
    private fun enviarSmsDeEmergencia(contatos: List<CuidadorEntity>, mensagem: String): Pair<Boolean, String> {
        if (contatos.isEmpty()) {
            return false to "Nenhum contato de emergência cadastrado no celular."
        }
        val permissao = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        if (permissao != PackageManager.PERMISSION_GRANTED) {
            return false to "O celular não tem permissão para enviar SMS."
        }

        val sms: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        if (sms == null) {
            return false to "Este celular não consegue enviar SMS."
        }

        val avisados = mutableListOf<String>()
        contatos.forEach { contato ->
            try {
                val partes = sms.divideMessage(mensagem)
                sms.sendMultipartTextMessage(contato.telefone, null, partes, null, null)
                val parentesco = contato.parentesco?.let { " ($it)" } ?: ""
                avisados.add(contato.nome + parentesco)
            } catch (erro: Exception) {
                Log.w("ALIRA_SYNC", "Falha ao enviar SMS para ${contato.nome}", erro)
            }
        }

        return if (avisados.isNotEmpty()) {
            true to avisados.joinToString(", ")
        } else {
            false to "Erro ao enviar o SMS."
        }
    }

    private fun responderAoRelogio(idDoRelogio: String, sucesso: Boolean, detalhe: String) {
        val texto = listOf(if (sucesso) "OK" else "FALHA", detalhe).joinToString(SEP)
        Wearable.getMessageClient(this)
            .sendMessage(idDoRelogio, CAMINHO_EMERGENCIA_RESULTADO, texto.toByteArray(Charsets.UTF_8))
            .addOnFailureListener { erro ->
                Log.w("ALIRA_SYNC", "Não foi possível responder ao relógio", erro)
            }
    }
}
