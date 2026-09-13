package me.slz_br.alira.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Monitoramento" do diagrama de classes. Campos idênticos:
 * idMonitoramento, frequenciaCardiaca, saturacaoOxigenio,
 * temperaturaCorporal, dataHora. Relacionamento "Usuario possui
 * Monitoramento".
 *
 * Único ajuste: no diagrama os 3 campos de biometria são "double" sem
 * indicar nulidade; aqui eles são Double? (aceitam nulo). Isso é
 * necessário porque hoje só o sensor de frequência cardíaca está
 * implementado (HearRateScreen.kt) — saturação de oxigênio e temperatura
 * corporal ainda não têm sensor lendo. Gravar 0.0 nesses dois campos
 * seria pior: pareceria uma leitura real de valor zero. Assim que os
 * sensores forem adicionados, os campos passam a ser preenchidos.
 */
@Entity(
    tableName = "monitoramento",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("usuario_id"), Index("data_hora")],
)
data class MonitoramentoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_monitoramento")
    val idMonitoramento: Long = 0L,

    @ColumnInfo(name = "frequencia_cardiaca")
    val frequenciaCardiaca: Double? = null,

    @ColumnInfo(name = "saturacao_oxigenio")
    val saturacaoOxigenio: Double? = null,

    @ColumnInfo(name = "temperatura_corporal")
    val temperaturaCorporal: Double? = null,

    @ColumnInfo(name = "data_hora")
    val dataHora: Long,

    @ColumnInfo(name = "usuario_id")
    val usuarioId: Long,
)
