package me.slz_br.alira.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import me.slz_br.alira.emergencia.CentralDeEmergencia
import me.slz_br.alira.emergencia.EstadoEmergencia
import me.slz_br.alira.emergencia.NotificacaoDeEmergencia
import me.slz_br.alira.emergencia.TipoEmergencia
import me.slz_br.alira.presentation.theme.AliraTheme

/**
 * Tela "Você está bem?" que aparece numa emergência (queda ou crise).
 *
 * Ela não decide nada: só mostra o estado atual da CentralDeEmergencia e
 * repassa o toque da pessoa para ela. Por isso funciona igual tanto para a
 * queda quanto para a ansiedade.
 *
 * Desenho pensado para acessibilidade:
 *  - A tela é dividida em duas metades enormes: em cima "ESTOU BEM"
 *    (verde), embaixo "PRECISO DE AJUDA" (vermelho). Não precisa acertar
 *    um botão pequeno — bom para quem tem tremor, deficiência motora ou
 *    visual (a voz explica: "parte de cima" / "parte de baixo").
 *  - Cores com texto branco em fundo escuro: alto contraste.
 *  - Para surdos, a faixa do meio pisca (no máximo 1 vez por segundo,
 *    abaixo do limite de 3 piscadas/s considerado seguro para quem tem
 *    epilepsia fotossensível).
 */
class AlertaEmergenciaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aparece mesmo com o relógio bloqueado, acende a tela e não deixa
        // ela apagar enquanto a pergunta estiver aberta.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AliraTheme {
                TelaAlertaEmergencia(
                    onFechar = {
                        CentralDeEmergencia.fechar()
                        finish()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        CentralDeEmergencia.telaAbriu(this)
    }

    override fun onStop() {
        CentralDeEmergencia.telaSaiu(this)
        super.onStop()
    }
}

private val VerdeEstouBem = Color(0xFF1B5E20)
private val VermelhoAjuda = Color(0xFFB71C1C)
private val CinzaFaixa = Color(0xFF212121)
private val VermelhoPiscando = Color(0xFFD32F2F)

@Composable
private fun TelaAlertaEmergencia(onFechar: () -> Unit) {
    val estado by CentralDeEmergencia.estado.collectAsState()
    val context = LocalContext.current

    when (val atual = estado) {
        is EstadoEmergencia.Inativo -> {
            // Tela aberta depois que a emergência já acabou (ex.: tocou numa
            // notificação antiga): não há nada para mostrar.
            // Aproveita para tirar alguma notificação que tenha sobrado (ex.:
            // o app foi fechado à força no meio de uma emergência).
            LaunchedEffect(Unit) {
                NotificacaoDeEmergencia.remover(context)
                onFechar()
            }
        }

        is EstadoEmergencia.Perguntando -> TelaPergunta(
            tipo = atual.tipo,
            textoContagem = "Avisando seu contato em ${atual.segundosRestantes} s",
            piscar = atual.perfil.telaPiscando,
        )

        is EstadoEmergencia.EsperandoParaPerguntarDeNovo -> TelaPergunta(
            tipo = atual.tipo,
            textoContagem = "Pergunto de novo em ${atual.segundosRestantes} s",
            piscar = false,
        )

        is EstadoEmergencia.AvisandoContato -> TelaMensagem(
            titulo = "Avisando seu contato...",
            detalhe = null,
            onOk = null,
        )

        is EstadoEmergencia.ContatoAvisado -> TelaMensagem(
            titulo = "Seu contato foi avisado",
            detalhe = atual.contatos,
            onOk = onFechar,
        )

        is EstadoEmergencia.FalhaAoAvisar -> TelaMensagem(
            titulo = "Não consegui avisar seu contato",
            detalhe = "${atual.motivo} Se precisar, peça ajuda a alguém por perto.",
            onOk = onFechar,
        )

        is EstadoEmergencia.PessoaEstaBem -> {
            TelaMensagem(
                titulo = "Que bom que você está bem!",
                detalhe = "O ocorrido foi registrado.",
                onOk = onFechar,
            )
            // Fecha sozinha depois de alguns segundos.
            LaunchedEffect(atual) {
                delay(4_000)
                onFechar()
            }
        }
    }
}

@Composable
private fun TelaPergunta(tipo: TipoEmergencia, textoContagem: String, piscar: Boolean) {
    var acesa by remember { mutableStateOf(false) }
    if (piscar) {
        LaunchedEffect(Unit) {
            while (true) {
                acesa = !acesa
                delay(500)
            }
        }
    }
    val corDaFaixa = if (piscar && acesa) VermelhoPiscando else CinzaFaixa

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Metade de cima: ESTOU BEM
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(VerdeEstouBem)
                .clickable(onClickLabel = "Responder que estou bem") {
                    CentralDeEmergencia.responder(estouBem = true)
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tipo.pergunta,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "ESTOU BEM",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Faixa do meio: o que aconteceu + contagem regressiva
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(corDaFaixa)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = tipo.titulo,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = textoContagem,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        // Metade de baixo: PRECISO DE AJUDA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(VermelhoAjuda)
                .clickable(onClickLabel = "Pedir ajuda") {
                    CentralDeEmergencia.responder(estouBem = false)
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "PRECISO DE AJUDA",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun TelaMensagem(titulo: String, detalhe: String?, onOk: (() -> Unit)?) {
    // O Box centraliza a mensagem quando ela é curta. Quando é comprida
    // demais para a tela redonda, a Column de dentro passa a rolar — assim
    // o botão OK nunca fica escondido.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (detalhe != null) {
                Text(
                    text = detalhe,
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (onOk != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .sizeIn(minWidth = 96.dp, minHeight = 48.dp)
                        .background(Color(0xFF424242), RoundedCornerShape(50))
                        .clickable(onClickLabel = "Fechar") { onOk() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "OK",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
