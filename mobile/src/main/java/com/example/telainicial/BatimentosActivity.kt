package com.example.telainicial

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import com.example.telainicial.data.repository.AliraRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BatimentosActivity : BaseActivity() {

    override val tituloTela = "Tela de batimentos cardíacos"

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var chart: BarChart

    private data class Dados(val labels: List<String>, val valores: List<Float>)

    // Cores das barras
    private val palette = listOf(
        0xFF4F7BFF.toInt(), // azul
        0xFFB38CFF.toInt(), // roxo
        0xFFF5B96B.toInt(), // laranja
        0xFFFFE066.toInt()  // amarelo
    )

    // Os 3 filtros da tela. Cada um diz quais barras o gráfico tem.
    private enum class Filtro { ANUAL, MENSAL, SEMANAL }

    private var filtroAtual = Filtro.ANUAL

    private val repository by lazy { AliraRepository.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_batimentos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chart = findViewById(R.id.bar_chart)
        setupChart()
        setupFilters()
        setupDrawerMenu()
        observarLeituras()
    }

    // ---------------------------------------------------------------- DADOS DO RELÓGIO
    /**
     * Fica "escutando" a tabela de leituras (Flow). Toda vez que chegam
     * leituras novas do relógio, refaz o gráfico do filtro escolhido e a
     * lista do histórico. repeatOnLifecycle(STARTED): só escuta enquanto a
     * tela está visível (economiza bateria).
     */
    private fun observarLeituras() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observarUltimosBatimentos(limite = 20).collect { ultimas ->
                    mostrarHistorico(ultimas)
                    atualizarGrafico(calcular(filtroAtual))
                }
            }
        }
    }

    /**
     * Monta as barras do filtro: para cada período (ano, semana do mês ou
     * dia da semana), pede ao banco a MÉDIA dos batimentos daquele período.
     * Período sem leitura vira 0 (barra vazia).
     */
    private suspend fun calcular(filtro: Filtro): Dados {
        val periodos = when (filtro) {
            Filtro.ANUAL -> periodosAnuais()
            Filtro.MENSAL -> periodosDoMes()
            Filtro.SEMANAL -> periodosDaSemana()
        }
        val valores = periodos.map { (_, inicio, fim) ->
            repository.mediaBatimentos(inicio, fim)?.toFloat() ?: 0f
        }
        return Dados(periodos.map { it.first }, valores)
    }

    /** Hoje à meia-noite (início do dia). */
    private fun hojeMeiaNoite(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** Anual: os últimos 4 anos (ex.: 2023, 2024, 2025, 2026). */
    private fun periodosAnuais(): List<Triple<String, Long, Long>> {
        val anoAtual = Calendar.getInstance().get(Calendar.YEAR)
        return (anoAtual - 3..anoAtual).map { ano ->
            val inicio = hojeMeiaNoite().apply { set(ano, Calendar.JANUARY, 1) }
            val fim = (inicio.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
            Triple(ano.toString(), inicio.timeInMillis, fim.timeInMillis)
        }
    }

    /**
     * Mensal: as semanas do mês atual. Sem 1 = dias 1 a 7, Sem 2 = 8 a 14,
     * Sem 3 = 15 a 21, Sem 4 = 22 a 28 e, se o mês tiver mais de 28 dias,
     * Sem 5 = do dia 29 até o fim do mês.
     */
    private fun periodosDoMes(): List<Triple<String, Long, Long>> {
        val primeiroDia = hojeMeiaNoite().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val fimDoMes = (primeiroDia.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
        val diasNoMes = primeiroDia.getActualMaximum(Calendar.DAY_OF_MONTH)
        val semanas = if (diasNoMes > 28) 5 else 4
        return (0 until semanas).map { s ->
            val inicio = (primeiroDia.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, s * 7) }
            val fim = if (s == semanas - 1) fimDoMes
            else (inicio.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }.timeInMillis
            Triple("Sem ${s + 1}", inicio.timeInMillis, fim)
        }
    }

    /** Semanal: de segunda a domingo da semana atual. */
    private fun periodosDaSemana(): List<Triple<String, Long, Long>> {
        val nomes = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        val hoje = hojeMeiaNoite()
        // DAY_OF_WEEK: domingo = 1, segunda = 2, ..., sábado = 7.
        // Quantos dias voltar para chegar na segunda-feira:
        val diasDesdeSegunda = (hoje.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val segunda = (hoje.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -diasDesdeSegunda) }
        return nomes.mapIndexed { i, nome ->
            val inicio = (segunda.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val fim = (inicio.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            Triple(nome, inicio.timeInMillis, fim.timeInMillis)
        }
    }

    // ---------------------------------------------------------------- HISTÓRICO
    /** Um cartão por leitura (mais nova em cima). */
    private fun mostrarHistorico(leituras: List<MonitoramentoEntity>) {
        val lista = findViewById<LinearLayout>(R.id.historico_lista)
        lista.removeAllViews()

        if (leituras.isEmpty()) {
            lista.addView(TextView(this).apply {
                text = "Nenhuma leitura recebida ainda. No relógio, abra Relatórios e toque em " +
                    "\"Gerar relatório\" para enviar os dados."
                setTextColor(Color.parseColor("#B3FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            })
        } else {
            val formato = SimpleDateFormat("dd/MM/yyyy '·' HH:mm", Locale("pt", "BR"))
            val padding = (16 * resources.displayMetrics.density).toInt()
            val espaco = (8 * resources.displayMetrics.density).toInt()
            leituras.forEach { leitura ->
                val quando = formato.format(Date(leitura.dataHora))
                val bpm = leitura.frequenciaCardiaca?.toInt() ?: 0
                lista.addView(TextView(this).apply {
                    text = "$quando     $bpm bpm"
                    contentDescription = "$quando, $bpm batimentos por minuto"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    setTypeface(typeface, Typeface.BOLD)
                    setBackgroundResource(R.drawable.card_historico)
                    setPadding(padding, padding, padding, padding)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = espaco }
                })
            }
        }
        // Os cartões nasceram agora: aplica a fonte e o tamanho escolhidos em Acessibilidade.
        AcessibilidadeManager.aplicarEm(lista)
    }

    // ---------------------------------------------------------------- GRÁFICO
    private fun setupChart() {
        val radiusPx = 10f * resources.displayMetrics.density

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setNoDataText("Sem leituras do relógio neste período")
            setNoDataTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)
            setFitBars(true)
            extraBottomOffset = 8f

            // sem zoom/arrastar, para não brigar com a rolagem da tela
            setScaleEnabled(false)
            setPinchZoom(false)
            isDragEnabled = false
            isDoubleTapToZoomEnabled = false

            // eixo Y (esquerda): 0, 20, 40, 60, 80...
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 20f
                isGranularityEnabled = true
                textColor = Color.parseColor("#B3FFFFFF")
                textSize = 11f
                gridColor = Color.parseColor("#33FFFFFF")
                setDrawAxisLine(false)
            }

            // eixo X: nomes das barras
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                textSize = 11f
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                setDrawAxisLine(false)
            }

            marker = ValueMarker(this@BatimentosActivity)
            renderer = RoundedBarChartRenderer(this, animator, viewPortHandler, radiusPx)
        }
    }

    private fun atualizarGrafico(dados: Dados) {
        // Nenhuma leitura no período inteiro: mostra o aviso em vez de barras vazias.
        if (dados.valores.all { it == 0f }) {
            chart.clear()
            chart.contentDescription = "Gráfico de batimentos: sem leituras do relógio neste período."
            return
        }

        val entries = dados.valores.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }

        val dataSet = BarDataSet(entries, "Batimentos").apply {
            colors = List(entries.size) { palette[it % palette.size] }
            setDrawValues(false)
            highLightColor = Color.WHITE
            highLightAlpha = 70
        }

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dados.labels)
        chart.xAxis.labelCount = dados.labels.size
        chart.data = BarData(dataSet).apply { barWidth = 0.7f }
        chart.highlightValues(null)

        // Acessibilidade: leitores de tela leem os valores
        chart.contentDescription = "Gráfico de barras, média de batimentos por minuto. " +
                dados.labels.zip(dados.valores).joinToString(". ") { (rotulo, valor) ->
                    if (valor == 0f) "$rotulo: sem leitura" else "$rotulo: ${valor.toInt()}"
                }

        chart.animateY(600)
    }

    // ---------------------------------------------------------------- FILTROS
    private fun setupFilters() {
        val filtros = listOf(
            findViewById<TextView>(R.id.filter_anual) to Filtro.ANUAL,
            findViewById<TextView>(R.id.filter_mensal) to Filtro.MENSAL,
            findViewById<TextView>(R.id.filter_semanal) to Filtro.SEMANAL
        )

        // Deixa o filtro escolhido aceso e os outros apagados.
        fun destacar(escolhido: Filtro) {
            filtros.forEach { (view, filtro) -> view.alpha = if (filtro == escolhido) 1f else 0.5f }
        }

        filtros.forEach { (view, filtro) ->
            view.setOnClickListener {
                filtroAtual = filtro
                destacar(filtro)
                // As médias vêm do banco, que não pode ser lido na thread da
                // tela: por isso o cálculo roda numa coroutine.
                lifecycleScope.launch {
                    atualizarGrafico(calcular(filtro))
                    LeitorDeTela.anunciar(this@BatimentosActivity, "Filtro ${view.text} selecionado. ${chart.contentDescription}")
                }
            }
        }
        // Começa em "Anual". O gráfico é desenhado quando as leituras
        // chegam do banco (observarLeituras).
        destacar(filtroAtual)
    }

    // ---------------------------------------------------------------- MENU
    private fun setupDrawerMenu() {
        drawerLayout = findViewById(R.id.main)

        findViewById<ImageView>(R.id.btn_menu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Troque os Toasts pelas suas telas
        // ("Relatórios" já abre esta tela: ver Navegacao.configurarMenu)
        listOf(
            R.id.menu_institucional to "Institucional"
        ).forEach { (id, nome) ->
            findViewById<View>(id).setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.START)
                Toast.makeText(this, nome, Toast.LENGTH_SHORT).show()
            }
        }

        // Perfil, Acessibilidade, Relatórios e logo ALIRA (-> tela inicial)
        Navegacao.configurarMenu(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ---------------------------------------------------------------- ACESSIBILIDADE NO GRÁFICO
    /** O gráfico desenha o próprio texto (eixos), então não é alcançado pelo percorrer de views. */
    override fun aoAplicarAcessibilidade() {
        if (!::chart.isInitialized) return

        val escala = AcessibilidadeManager.escalaTexto(this)
        val fonte = AcessibilidadeManager.typeface(this, AcessibilidadeManager.fonte(this), Typeface.NORMAL)

        chart.xAxis.textSize = 11f * escala      // em dp (o MPAndroidChart converte)
        chart.xAxis.typeface = fonte
        chart.axisLeft.textSize = 11f * escala
        chart.axisLeft.typeface = fonte

        // mais espaço para os rótulos e um gráfico um pouco mais alto quando o texto cresce
        chart.extraBottomOffset = 8f * escala
        chart.layoutParams = chart.layoutParams.apply {
            height = (300f * (1f + (escala - 1f) * 0.5f) * resources.displayMetrics.density).toInt()
        }

        if (chart.data != null) chart.notifyDataSetChanged()
        chart.invalidate()
    }
}
