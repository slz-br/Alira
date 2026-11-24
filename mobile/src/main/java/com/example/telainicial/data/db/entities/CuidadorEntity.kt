package com.example.telainicial.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mesma classe "Cuidador" do módulo :wear — cadastrada aqui no celular (teclado de verdade). */
@Entity(
    tableName = "cuidador",
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
data class CuidadorEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_cuidador")
    val idCuidador: Long = 0L,

    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "telefone")
    val telefone: String,

    @ColumnInfo(name = "parentesco")
    val parentesco: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,
)
