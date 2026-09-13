package me.slz_br.alira.data.db

import java.security.MessageDigest

/**
 * Mesma função do :wear (SHA-256), duplicada aqui de propósito: cada
 * módulo é compilado num .apk separado, então não existe como "importar"
 * uma classe de um módulo dentro do outro sem criar um terceiro módulo
 * Gradle só para código compartilhado — desnecessário pelo tamanho atual
 * do projeto.
 */
object Hashing {
    fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
