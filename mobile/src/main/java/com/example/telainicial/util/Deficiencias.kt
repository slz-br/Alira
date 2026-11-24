package com.example.telainicial.util

/**
 * Deficiências que a pessoa marca no Perfil.
 *
 * O que é GRAVADO é um código fixo, separado por vírgula (ex.:
 * "AUDITIVA,VISUAL"), porque a pessoa pode ter mais de uma. O relógio lê
 * esse código (emergencia/PerfilDeAlerta.kt no módulo :wear) para mudar o
 * jeito de avisar e para já ligar a acessibilidade certa:
 *
 *  - AUDITIVA    -> alertas sem som: vibração forte + tela piscando.
 *  - VISUAL      -> alertas falados, alto contraste e voz.
 *  - FISICA      -> por enquanto só fica registrada (uso futuro).
 *  - INTELECTUAL -> por enquanto só fica registrada (uso futuro).
 *
 * O que APARECE na tela é o nome bonito (ex.: "Auditiva, Visual").
 */
object Deficiencias {
    const val AUDITIVA = "AUDITIVA"
    const val VISUAL = "VISUAL"
    const val FISICA = "FISICA"
    const val INTELECTUAL = "INTELECTUAL"

    /** Ordem em que as opções aparecem na lista: código -> nome na tela. */
    val OPCOES = linkedMapOf(
        AUDITIVA to "Auditiva",
        VISUAL to "Visual",
        FISICA to "Física",
        INTELECTUAL to "Intelectual",
    )

    /** Junta os códigos marcados num texto só, ou null se nenhum foi marcado. */
    fun juntar(codigos: List<String>): String? =
        codigos.joinToString(",").ifBlank { null }

    /** "AUDITIVA,VISUAL" -> lista de códigos válidos. */
    fun separar(texto: String?): List<String> =
        texto.orEmpty().split(',').map { it.trim() }.filter { it in OPCOES }

    /** "AUDITIVA,VISUAL" -> "Auditiva, Visual" (o que aparece no campo). */
    fun nomes(texto: String?): String =
        separar(texto).joinToString(", ") { OPCOES.getValue(it) }
}
