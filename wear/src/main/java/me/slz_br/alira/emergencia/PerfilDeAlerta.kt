package me.slz_br.alira.emergencia

import me.slz_br.alira.acessibilidade.ConfigAcessibilidade

/**
 * "Como avisar esta pessoa" — calculado a partir da deficiência marcada
 * no cadastro do celular (coluna "deficiencia" do Usuario, ex.:
 * "AUDITIVA,VISUAL"; ver util/Deficiencias.kt no :mobile).
 *
 * Regras:
 *  - Todo mundo recebe vibração forte.
 *  - Deficiência AUDITIVA: sem som; a faixa do meio da tela pisca.
 *  - Deficiência VISUAL: o relógio FALA a pergunta e o resultado (a voz
 *    substitui o alarme, para os dois não tocarem por cima um do outro).
 *  - Sem deficiência auditiva nem visual: toca o som de alarme.
 *  - Auditiva + visual ao mesmo tempo: só vibração (o único sinal que a
 *    pessoa consegue perceber).
 *
 * As chaves da tela de Acessibilidade também entram na conta (ver o
 * segundo de() lá embaixo).
 *
 * Também entende o texto livre dos cadastros antigos (antes das caixinhas
 * de marcar existirem): "surdo", "cega", etc.
 */
data class PerfilDeAlerta(
    val auditiva: Boolean,
    val visual: Boolean,
    /** "Sinais de vibração" ligado na Acessibilidade: vibração na força máxima. */
    val vibracaoForte: Boolean = false,
) {
    /** Toca o som de alarme? */
    val usaSom: Boolean get() = !auditiva && !visual

    /** Fala a pergunta em voz alta? */
    val usaVoz: Boolean get() = visual && !auditiva

    /** Faz a faixa do meio da tela piscar? */
    val telaPiscando: Boolean get() = auditiva

    companion object {
        fun de(deficiencia: String?): PerfilDeAlerta {
            val texto = deficiencia.orEmpty().lowercase()
            return PerfilDeAlerta(
                auditiva = "auditiva" in texto || "surd" in texto,
                visual = "visual" in texto || "ceg" in texto,
            )
        }

        /**
         * Igual ao de() acima, mas também respeita a tela de Acessibilidade:
         *  - "Texto para voz" ligado -> trata como quem precisa ouvir (fala a
         *    pergunta), mesmo que a deficiência visual não esteja marcada;
         *  - "Sinais de vibração" ligado -> vibração mais forte.
         */
        fun de(deficiencia: String?, config: ConfigAcessibilidade): PerfilDeAlerta {
            val base = de(deficiencia)
            return base.copy(
                visual = base.visual || config.voz,
                vibracaoForte = config.vibracao,
            )
        }
    }
}
