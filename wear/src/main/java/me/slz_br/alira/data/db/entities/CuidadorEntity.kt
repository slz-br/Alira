package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Cuidado" do diagrama de classes (nome normalizado para
 * "Cuidador" aqui no código/tabela, para concordar em português com o
 * restante do TCC — inclusive o próprio DER do capítulo 5.5 já usa
 * "Cuidador/Familiar"; os campos são exatamente os do diagrama:
 * idCuidador, nome, telefone, parentesco, email). Relacionamento
 * "Usuario possui Cuidado".
 *
 * Os métodos receberAlerta() e acompanharUsuario() do diagrama são, por
 * natureza, ações do LADO do cuidador (rodariam no celular/app de quem
 * cuida, não neste smartwatch). Como o módulo mobile ainda não existe,
 * eles foram representados em AliraRepository.kt como consultas que um
 * futuro app do cuidador chamaria (observarAlertasParaCuidador,
 * observarUltimoMonitoramento), em vez de simuladas com dado falso.
 */
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
