package me.slz_br.alira.data.db

import java.security.MessageDigest

/**
 * Hash simples de senha (SHA-256) para o campo UsuarioEntity.senha.
 *
 * Observação para o TCC: SHA-256 puro é melhor que texto plano, mas o
 * ideal para produção é um algoritmo com "salt" e custo ajustável, como
 * BCrypt ou Argon2 (bibliotecas externas). Como o cadastro/login (RF08)
 * ainda não está implementado em nenhuma tela, mantive aqui só o
 * suficiente da biblioteca padrão do Java para não introduzir uma
 * dependência nova por causa de um fluxo que ainda não existe.
 */
object Hashing {
    fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
