package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mesma classe "Usuario" do diagrama, agora no celular — este é o banco
 * ONDE O CADASTRO NASCE (RF08 é tipo "Mobile" na sua tabela de requisitos).
 * O relógio recebe uma cópia sincronizada via Data Layer API (ver
 * data/sync/MobileDataLayerService.kt), não lê esta tabela diretamente:
 * são dois arquivos .db separados, em dois aparelhos separados.
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
    val senha: String, // hash SHA-256, nunca texto puro (RNF08/RNF10)
)
