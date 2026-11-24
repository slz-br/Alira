package com.example.telainicial.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mesma classe "Usuario" do diagrama, agora no celular. Os dados nascem na
 * tela de Perfil (PerfilActivity) e são copiados para o relógio pela Data
 * Layer API (ver data/sync/MobileDataLayerService.kt). São dois arquivos
 * .db separados, em dois aparelhos separados.
 *
 * O celular guarda UM usuário só: o dono do aparelho.
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

    // Este app não tem tela de login, então a senha fica vazia. A coluna
    // continua existindo porque faz parte do diagrama de classes e do banco
    // que já estava instalado (versão 1).
    @ColumnInfo(name = "senha")
    val senha: String = "",
)
