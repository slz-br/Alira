package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Classe "Usuario" do diagrama de classes (Imagem 6).
 * Campos e tipos seguem exatamente o diagrama:
 * idUsuario, nome, dataNascimento, telefone, email, deficiencia,
 * nivelAutonomia, senha.
 *
 * Duas adaptações de implementação (não estruturais — os campos continuam
 * os mesmos do diagrama):
 * 1) "dataNascimento : Date" é gravado como epoch millis (Long), prática
 *    padrão do Room/Android em vez de guardar um objeto Date.
 * 2) "senha" continua existindo como String nesta tabela, mas o app grava
 *    aqui o HASH da senha (ver Hashing.kt), nunca o texto puro — isso é
 *    exigido pelo RNF08/RNF10 do próprio TCC (criptografia e LGPD).
 *
 * Os métodos autenticar(), atualizarPerfil() e solicitarAjuda() do
 * diagrama não viram funções desta classe (Room exige entidades como
 * dados puros) — eles foram implementados em AliraRepository.kt, que faz
 * o papel de "camada de comportamento" do Usuario.
 */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_usuario")
    val idUsuario: Long = 0L,

    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "data_nascimento")
    val dataNascimento: Long? = null,

    @ColumnInfo(name = "telefone")
    val telefone: String? = null,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "deficiencia")
    val deficiencia: String? = null,

    @ColumnInfo(name = "nivel_autonomia")
    val nivelAutonomia: String? = null,

    @ColumnInfo(name = "senha")
    val senha: String, // hash da senha, nunca texto puro
)
