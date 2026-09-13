package me.slz_br.alira.util

/**
 * Validações de campo usadas em CadastroActivity/LoginActivity (Tabela 1
 * do capítulo de qualidade: "Cadastro de usuário -> E-mail -> não pode
 * ficar vazio e deve ter formato válido").
 *
 * Por que isso vive numa function object separada, em vez de ficar direto
 * dentro da Activity (como estava antes, usando android.util.Patterns):
 * android.util.Patterns só existe dentro do Android de verdade (é um stub
 * vazio fora dele), então qualquer teste dessa regra precisava rodar num
 * emulador (teste instrumentado, lento). Uma Regex do Kotlin puro roda em
 * qualquer JVM — isso é o que deixa em mobile/src/test/.../ValidacoesTest.kt
 * ser um teste unitário comum, que roda em segundos, sem precisar de
 * emulador. É a mesma ideia de "testabilidade" que o capítulo 8 cobra:
 * regra de negócio isolada da tela = mais fácil de testar.
 */
object Validacoes {

    // Regex simples (não tenta cobrir 100% dos casos exóticos do RFC 5322,
    // só barra os erros de digitação mais comuns: sem "@", sem domínio,
    // espaço no meio, "@" duplicado etc.) — é exatamente o que a Tabela 1
    // pede como validação de e-mail.
    private val REGEX_EMAIL = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailValido(email: String): Boolean = REGEX_EMAIL.matches(email.trim())
}
