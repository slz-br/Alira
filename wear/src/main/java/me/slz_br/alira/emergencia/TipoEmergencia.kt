package me.slz_br.alira.emergencia

/**
 * Os tipos de emergência que o relógio sabe tratar.
 *
 * Todas (queda, crise de ansiedade e batimento fora do normal) terminam do mesmo jeito —
 * perguntar "Você está bem?", esperar e, se precisar, avisar o contato —
 * então a diferença entre elas fica toda aqui, nos textos e em quantas
 * vezes perguntar. O fluxo em si é um só (CentralDeEmergencia).
 *
 * @param codigo     gravado em alerta.tipo_alerta e enviado ao celular.
 * @param tentativas quantas vezes perguntar antes de avisar o contato
 *                   (queda: 1 vez; ansiedade: 2 vezes, com 1 minuto entre elas).
 */
enum class TipoEmergencia(
    val codigo: String,
    val titulo: String,
    val pergunta: String,
    val textoFalado: String,
    val tentativas: Int,
) {
    QUEDA(
        codigo = "QUEDA",
        titulo = "Possível queda",
        pergunta = "Você está bem?",
        textoFalado = "Detectei uma possível queda. Você está bem? " +
            "Toque na parte de cima da tela se estiver bem, ou na parte de baixo para pedir ajuda.",
        tentativas = 1,
    ),
    ANSIEDADE(
        codigo = "ANSIEDADE",
        titulo = "Coração acelerado",
        pergunta = "Está tudo bem?",
        textoFalado = "Seus batimentos estão acelerados. Está tudo bem? " +
            "Toque na parte de cima da tela se estiver bem, ou na parte de baixo para pedir ajuda.",
        tentativas = 2,
    ),
    FREQUENCIA_ANORMAL(
        codigo = "FREQUENCIA_ANORMAL",
        titulo = "Batimento fora do normal",
        pergunta = "Você está bem?",
        textoFalado = "Seus batimentos estão fora do normal. Você está bem? " +
            "Toque na parte de cima da tela se estiver bem, ou na parte de baixo para pedir ajuda.",
        tentativas = 1,
    ),
}
