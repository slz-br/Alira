package com.example.telainicial

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class AcessibilidadeActivity : BaseActivity() {

    override val tituloTela = "Tela de acessibilidade"

    private val COR_SELECIONADO = 0xFF4A7CF0.toInt()
    private val COR_NORMAL = 0xFFE0E0E0.toInt()

    private val COR_TRACK_OFF = 0xFFC8D0DA.toInt()
    private val COR_TRACK_ON = 0xFF4A7CF0.toInt()

    private lateinit var drawer: DrawerLayout
    private lateinit var switchTrack: FrameLayout
    private lateinit var switchThumb: TextView
    private lateinit var previewBotao: View

    private lateinit var fontes: List<TextView>
    private lateinit var temas: List<TextView>

    // chave da fonte -> nome falado
    private val nomesFontes = mapOf(
        AcessibilidadeManager.FONTE_QUICKSAND to "Quicksand",
        AcessibilidadeManager.FONTE_OPENDYSLEXIC to "OpenDyslexic",
        AcessibilidadeManager.FONTE_ARIAL to "Arial"
    )

    private var leitorLigado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acessibilidade)

        drawer = findViewById(R.id.main)
        switchTrack = findViewById(R.id.switch_track)
        switchThumb = findViewById(R.id.switch_thumb)
        previewBotao = findViewById(R.id.preview_botao)

        // ===================== MENU =====================
        findViewById<View>(R.id.btn_menu).setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
        Navegacao.configurarMenu(this) // Perfil, Acessibilidade e logo -> tela inicial

        configurarSwitch()
        configurarFontes()
        configurarSliderFonte()
        configurarSliderBotoes()
        configurarTemas()
    }

    /** Cada opção de fonte aparece escrita na PRÓPRIA fonte, para a pessoa ver antes de escolher. */
    override fun aoAplicarAcessibilidade() {
        if (!::fontes.isInitialized) return
        val chaves = chavesDasFontes()
        fontes.forEach { tv ->
            tv.typeface = AcessibilidadeManager.typeface(this, chaves.getValue(tv), Typeface.BOLD)
        }
    }

    // ===================== LEITOR DE TELA (OFF/ON) =====================
    private fun configurarSwitch() {
        leitorLigado = AcessibilidadeManager.leitorLigado(this)
        // espera medir o layout para saber o quanto a bolinha deve andar
        switchTrack.post { atualizarSwitch(animar = false) }

        switchTrack.setOnClickListener {
            leitorLigado = !leitorLigado
            AcessibilidadeManager.setLeitorLigado(this, leitorLigado)
            atualizarSwitch(animar = true)

            LeitorDeTela.falarSempre(
                this,
                if (leitorLigado) {
                    "Leitor de tela ativado. Toque uma vez para ouvir um item e toque de novo para ativá-lo."
                } else {
                    "Leitor de tela desativado."
                }
            )
        }
    }

    private fun atualizarSwitch(animar: Boolean) {
        val distancia = (switchTrack.width - switchThumb.width).toFloat()
        val destino = if (leitorLigado) distancia else 0f

        switchThumb.text = if (leitorLigado) "ON" else "OFF"
        switchTrack.contentDescription = "Leitor de tela, " + if (leitorLigado) "ligado" else "desligado"
        (switchTrack.background.mutate() as GradientDrawable)
            .setColor(if (leitorLigado) COR_TRACK_ON else COR_TRACK_OFF)

        if (animar) {
            switchThumb.animate().translationX(destino).setDuration(200).start()
        } else {
            switchThumb.translationX = destino
        }
    }

    // ===================== MUDAR FONTE =====================
    private fun chavesDasFontes(): Map<TextView, String> = mapOf(
        findViewById<TextView>(R.id.font_quicksand) to AcessibilidadeManager.FONTE_QUICKSAND,
        findViewById<TextView>(R.id.font_opendyslexic) to AcessibilidadeManager.FONTE_OPENDYSLEXIC,
        findViewById<TextView>(R.id.font_arial) to AcessibilidadeManager.FONTE_ARIAL
    )

    private fun configurarFontes() {
        val chaves = chavesDasFontes()
        fontes = chaves.keys.toList()

        val atual = AcessibilidadeManager.fonte(this)
        selecionar(fontes, chaves.entries.firstOrNull { it.value == atual }?.key ?: fontes.first())
        reaplicarAcessibilidade()

        fontes.forEach { tv ->
            tv.setOnClickListener {
                val chave = chaves.getValue(tv)
                AcessibilidadeManager.setFonte(this, chave)
                selecionar(fontes, tv)
                reaplicarAcessibilidade() // troca a fonte nesta tela, no menu lateral e em todas as outras ao voltar
                LeitorDeTela.anunciar(this, "Fonte ${nomesFontes[chave]} selecionada")
            }
        }
    }

    // ===================== TAMANHO DA FONTE =====================
    private fun configurarSliderFonte() {
        val seek = findViewById<SeekBar>(R.id.seek_fonte)
        seek.progress = AcessibilidadeManager.progressoFonte(this)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                AcessibilidadeManager.setProgressoFonte(this@AcessibilidadeActivity, progress)
                reaplicarAcessibilidade()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                val escala = AcessibilidadeManager.escalaTexto(this@AcessibilidadeActivity)
                LeitorDeTela.anunciar(this@AcessibilidadeActivity, "Tamanho da fonte ${(escala * 100).toInt()} por cento")
            }
        })
    }

    // ===================== TAMANHO DOS BOTÕES =====================
    private fun configurarSliderBotoes() {
        val seek = findViewById<SeekBar>(R.id.seek_botoes)
        seek.progress = AcessibilidadeManager.progressoBotoes(this)
        aplicarTamanhoBotaoPreview(seek.progress)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                AcessibilidadeManager.setProgressoBotoes(this@AcessibilidadeActivity, progress)
                aplicarTamanhoBotaoPreview(progress)
                reaplicarAcessibilidade() // os botões do app inteiro seguem este controle
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                LeitorDeTela.anunciar(this@AcessibilidadeActivity, "Tamanho dos botões ajustado")
            }
        })
    }

    private fun aplicarTamanhoBotaoPreview(progress: Int) {
        // altura vai de 48dp (progress 0) até 148dp (progress 100)
        val lp = previewBotao.layoutParams
        lp.height = dpToPx(48 + progress)
        previewBotao.layoutParams = lp
    }

    // ===================== TEMA =====================
    private fun configurarTemas() {
        val escuro = findViewById<TextView>(R.id.tema_escuro)
        val contraste = findViewById<TextView>(R.id.tema_contraste)
        val claro = findViewById<TextView>(R.id.tema_claro)
        temas = listOf(escuro, contraste, claro)

        val chaves = mapOf(escuro to "escuro", contraste to "contraste", claro to "claro")
        val salvo = AcessibilidadeManager.tema(this)
        selecionar(temas, chaves.entries.firstOrNull { it.value == salvo }?.key ?: escuro)

        temas.forEach { tv ->
            tv.setOnClickListener {
                AcessibilidadeManager.setTema(this, chaves.getValue(tv))
                selecionar(temas, tv)
                // TODO: aplicar o tema escolhido no app (cores ainda estão fixas nos XMLs)
            }
        }
    }

    // ===================== AUXILIARES =====================
    /** Deixa só o item escolhido em azul. */
    private fun selecionar(grupo: List<TextView>, escolhido: TextView) {
        grupo.forEach { it.setTextColor(if (it == escolhido) COR_SELECIONADO else COR_NORMAL) }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
