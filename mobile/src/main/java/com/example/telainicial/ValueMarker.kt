package com.example.telainicial

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

/** Balão que mostra o valor da barra tocada. */
class ValueMarker(context: Context) : MarkerView(context, R.layout.marker_value) {

    private val text: TextView = findViewById(R.id.marker_text)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        text.text = e?.y?.toInt()?.toString() ?: ""
        AcessibilidadeManager.aplicarEm(text) // balão também segue a fonte e o tamanho escolhidos
        super.refreshContent(e, highlight)
    }

    // Centraliza o balão acima da barra
    override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 8f)
}