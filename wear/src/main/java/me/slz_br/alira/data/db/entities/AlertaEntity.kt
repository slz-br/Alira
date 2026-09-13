package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Alerta" do diagrama de classes. Campos idênticos: idAlerta,
 * tipoAlerta, descricao, nivelUrgencia, dataHora, status (todos String,
 * como no diagrama — não viraram enum Kotlin, pra não fugir do tipo
 * desenhado). Relacionamentos: "Monitoramento gera Alerta" e
 * "Alerta notifica Cuidado".
 *
 * Ajuste: monitoramentoId e cuidadorId são FKs nullable. No diagrama a
 * seta "gera" sai do Monitoramento, o que sugere obrigatoriedade — mas
 * isso impediria o botão de SOS manual (solicitarAjuda(), método do
 * Usuario) de existir, já que ele cria um alerta sem nenhuma leitura de
 * biometria por trás. cuidadorId é nulo até o alerta ser de fato
 * despachado a um cuidador específico.
 */
@Entity(
    tableName = "alerta",
    foreignKeys = [
        ForeignKey(
            entity = MonitoramentoEntity::class,
            parentColumns = ["id_monitoramento"],
            childColumns = ["monitoramento_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CuidadorEntity::class,
            parentColumns = ["id_cuidador"],
            childColumns = ["cuidador_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("monitoramento_id"), Index("cuidador_id")],
)
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_alerta")
    val idAlerta: Long = 0L,

    @ColumnInfo(name = "tipo_alerta")
    val tipoAlerta: String,

    @ColumnInfo(name = "descricao")
    val descricao: String? = null,

    @ColumnInfo(name = "nivel_urgencia")
    val nivelUrgencia: String,

    @ColumnInfo(name = "data_hora")
    val dataHora: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "monitoramento_id")
    val monitoramentoId: Long? = null,

    @ColumnInfo(name = "cuidador_id")
    val cuidadorId: Long? = null,
)
