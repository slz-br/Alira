package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Acessibilidade" do diagrama de classes. Campos idênticos:
 * idAcessibilidade, nomeAcessibilidade, descricao, tipoAcessibilidade.
 * Relacionamento "Usuario configura Acessibilidade".
 *
 * Cada linha desta tabela é um recurso de acessibilidade (mapeando 1 para
 * 1 com os 3 itens que já existem na AcessibilitySettingsScreen: "Fonte
 * de alto contraste", "Sinais de vibração", "Texto para Voz").
 *
 * Única adição em relação ao diagrama: o campo "ativo" (Boolean). O
 * diagrama define os métodos ativar()/desativar(), mas não tem nenhum
 * campo pra guardar esse estado — sem ele não haveria onde persistir se o
 * recurso está ligado ou desligado. É a única coluna nova em toda a
 * migração; todas as outras entidades usam só os campos que já estavam
 * desenhados.
 */
@Entity(
    tableName = "acessibilidade",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("usuario_id")],
)
data class AcessibilidadeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_acessibilidade")
    val idAcessibilidade: Long = 0L,

    @ColumnInfo(name = "nome_acessibilidade")
    val nomeAcessibilidade: String,

    @ColumnInfo(name = "descricao")
    val descricao: String? = null,

    @ColumnInfo(name = "tipo_acessibilidade")
    val tipoAcessibilidade: String? = null,

    @ColumnInfo(name = "ativo", defaultValue = "0")
    val ativo: Boolean = false,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,
)
