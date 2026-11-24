package com.example.telainicial.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mesma classe "Relatorio" — o celular guarda a lista pra exibir/compartilhar (RF13). */
@Entity(
    tableName = "relatorio",
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
data class RelatorioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_relatorio")
    val idRelatorio: Long = 0L,

    @ColumnInfo(name = "tipo")
    val tipo: String,

    @ColumnInfo(name = "data_geracao")
    val dataGeracao: Long,

    @ColumnInfo(name = "caminho_arquivo")
    val caminhoArquivo: String? = null,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,
)
