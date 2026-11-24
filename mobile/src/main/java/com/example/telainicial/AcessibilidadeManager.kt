package com.example.telainicial

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * Fonte única da verdade das preferências de acessibilidade.
 *
 * Percorre TODAS as views de uma tela (conteúdo + menu lateral) e aplica:
 *  - família da fonte (Quicksand / OpenDyslexic / Arial)
 *  - tamanho do texto (0.8x até 1.6x)
 *  - tamanho dos botões (views com android:tag="botao")
 *
 * Os valores originais do XML ficam guardados nas próprias views (tags), então
 * aplicar várias vezes NUNCA acumula escala.
 */
object AcessibilidadeManager {

    const val PREFS = "alira_acessibilidade"

    const val FONTE_QUICKSAND = "quicksand"
    const val FONTE_OPENDYSLEXIC = "opendyslexic"
    const val FONTE_ARIAL = "arial"

    /** Views com android:tag="botao" (pode combinar: "botao compacto") crescem/encolhem com o controle "Tamanho dos botões". */
    const val TAG_BOTAO = "botao"

    /** Views com android:tag="compacto" (ex.: filtros lado a lado) crescem no máximo até este limite. */
    const val TAG_COMPACTO = "compacto"
    private const val ESCALA_MAX_COMPACTO = 1.25f

    private const val PADRAO_TAM_FONTE = 25     // = 1.0x
    private const val PADRAO_TAM_BOTOES = 20    // = tamanho original

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ===================== PREFERÊNCIAS =====================
    fun fonte(ctx: Context): String = prefs(ctx).getString("fonte", FONTE_QUICKSAND) ?: FONTE_QUICKSAND
    fun setFonte(ctx: Context, valor: String) = prefs(ctx).edit().putString("fonte", valor).apply()

    fun progressoFonte(ctx: Context): Int = prefs(ctx).getInt("tam_fonte", PADRAO_TAM_FONTE)
    fun setProgressoFonte(ctx: Context, valor: Int) = prefs(ctx).edit().putInt("tam_fonte", valor).apply()

    fun progressoBotoes(ctx: Context): Int = prefs(ctx).getInt("tam_botoes", PADRAO_TAM_BOTOES)
    fun setProgressoBotoes(ctx: Context, valor: Int) = prefs(ctx).edit().putInt("tam_botoes", valor).apply()

    fun leitorLigado(ctx: Context): Boolean = prefs(ctx).getBoolean("leitor", false)
    fun setLeitorLigado(ctx: Context, valor: Boolean) = prefs(ctx).edit().putBoolean("leitor", valor).apply()

    fun tema(ctx: Context): String = prefs(ctx).getString("tema", "escuro") ?: "escuro"
    fun setTema(ctx: Context, valor: String) = prefs(ctx).edit().putString("tema", valor).apply()

    // ===================== ESCALAS =====================
    /** 0.8x até 1.6x (progresso 25 = 1.0x). */
    fun escalaTexto(ctx: Context): Float = 0.8f + (progressoFonte(ctx) / 100f) * 0.8f

    /** Progresso 20 = tamanho original; 100 = bem maior. Limitado para não quebrar as telas. */
    fun escalaBotao(ctx: Context): Float = ((48 + progressoBotoes(ctx)) / 68f).coerceIn(0.7f, 1.6f)

    // ===================== FONTES =====================
    private val cacheFamilias = HashMap<String, Typeface>()

    /** Família completa (regular + negrito) da fonte escolhida. */
    fun familia(ctx: Context, chave: String = fonte(ctx)): Typeface =
        cacheFamilias.getOrPut(chave) { carregarFamilia(ctx, chave) }

    private fun carregarFamilia(ctx: Context, chave: String): Typeface {
        val res = when (chave) {
            FONTE_OPENDYSLEXIC -> R.font.opendyslexic
            FONTE_ARIAL -> R.font.arimo   // Arimo = equivalente livre e métrico do Arial
            else -> R.font.quicksand
        }
        return try {
            ResourcesCompat.getFont(ctx, res) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    /** Typeface da fonte escolhida mantendo o estilo (normal/negrito) que a view tinha no XML. */
    fun typeface(ctx: Context, chave: String, estilo: Int): Typeface =
        Typeface.create(familia(ctx, chave), estilo)

    // ===================== APLICAÇÃO =====================
    /** Aplica as preferências em toda a tela (conteúdo e menu lateral). */
    fun aplicar(activity: Activity) {
        val raiz = activity.findViewById<View>(android.R.id.content) ?: return
        aplicarEm(raiz)
    }

    /** Use também em views criadas depois (itens de lista, diálogos, marcadores de gráfico...). */
    fun aplicarEm(raiz: View) {
        val ctx = raiz.context
        val escalaTexto = escalaTexto(ctx)
        val escalaBotao = escalaBotao(ctx)
        val chave = fonte(ctx)
        percorrer(raiz, ctx, chave, escalaTexto, escalaBotao)
    }

    private fun percorrer(v: View, ctx: Context, chave: String, escTexto: Float, escBotao: Float) {
        if (v is TextView) aplicarTexto(v, ctx, chave, escTexto)
        if (temTag(v, TAG_BOTAO)) aplicarBotao(v, escBotao)

        if (v is ViewGroup) {
            for (i in 0 until v.childCount) percorrer(v.getChildAt(i), ctx, chave, escTexto, escBotao)
        }
    }

    private fun aplicarTexto(tv: TextView, ctx: Context, chave: String, escala: Float) {
        // guarda o tamanho e o estilo originais na primeira vez
        if (tv.getTag(R.id.a11y_tag_tam_base) == null) tv.setTag(R.id.a11y_tag_tam_base, tv.textSize)
        if (tv.getTag(R.id.a11y_tag_estilo_base) == null) {
            tv.setTag(R.id.a11y_tag_estilo_base, tv.typeface?.style ?: Typeface.NORMAL)
        }
        val base = tv.getTag(R.id.a11y_tag_tam_base) as Float
        val estilo = tv.getTag(R.id.a11y_tag_estilo_base) as Int

        val escalaFinal = if (temTag(tv, TAG_COMPACTO)) minOf(escala, ESCALA_MAX_COMPACTO) else escala
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * escalaFinal)
        tv.typeface = typeface(ctx, chave, estilo)
    }

    /** A tag pode ter várias palavras: android:tag="botao compacto". */
    private fun temTag(v: View, palavra: String): Boolean =
        (v.tag as? String)?.split(' ')?.contains(palavra) == true

    private fun aplicarBotao(v: View, escala: Float) {
        if (v.getTag(R.id.a11y_tag_altura_base) == null) v.setTag(R.id.a11y_tag_altura_base, v.minimumHeight)
        val base = v.getTag(R.id.a11y_tag_altura_base) as Int
        if (base > 0) v.minimumHeight = (base * escala).toInt()
    }
}
