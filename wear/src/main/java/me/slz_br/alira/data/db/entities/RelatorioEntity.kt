package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Relatorio" do diagrama de classes. Campos idênticos: idRelatorio,
 * tipo, dataGeracao. Relacionamento "Usuario visualiza Relatorio" e
 * "Relatorio utiliza dados (do) Monitoramento".
 *
 * Único campo extra: caminhoArquivo (String?, nulo por padrão). O método
 * gerarPDF() do diagrama precisa gravar o PDF em algum lugar do
 * armazenamento do relógio — sem este campo não haveria como o app achar
 * o arquivo depois de gerado. exportarDados() usa o mesmo campo (aponta
 * pro .csv exportado).
 */
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
