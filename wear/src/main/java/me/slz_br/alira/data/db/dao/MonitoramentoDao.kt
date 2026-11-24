package me.slz_br.alira.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.entities.MonitoramentoEntity

@Dao
interface MonitoramentoDao {

    @Insert
    suspend fun inserir(monitoramento: MonitoramentoEntity): Long

    @Query(
        "SELECT * FROM monitoramento WHERE usuario_id = :usuarioId " +
            "ORDER BY data_hora DESC LIMIT :limite"
    )
    fun observarHistorico(usuarioId: Long, limite: Int = 50): Flow<List<MonitoramentoEntity>>

    @Query(
        "SELECT * FROM monitoramento WHERE usuario_id = :usuarioId " +
            "ORDER BY data_hora DESC LIMIT 1"
    )
    fun observarUltimo(usuarioId: Long): Flow<MonitoramentoEntity?>

    @Query(
        "SELECT * FROM monitoramento WHERE usuario_id = :usuarioId " +
            "AND data_hora BETWEEN :inicio AND :fim ORDER BY data_hora"
    )
    suspend fun obterEntrePeriodo(usuarioId: Long, inicio: Long, fim: Long): List<MonitoramentoEntity>

    // Próximo "lote" de leituras ainda não enviadas ao celular: as que têm
    // número (id) maior que o último enviado, em ordem, no máximo :limite.
    @Query(
        "SELECT * FROM monitoramento WHERE usuario_id = :usuarioId " +
            "AND id_monitoramento > :ultimoId ORDER BY id_monitoramento LIMIT :limite"
    )
    suspend fun obterDepoisDe(usuarioId: Long, ultimoId: Long, limite: Int): List<MonitoramentoEntity>

    @Query(
        "SELECT AVG(frequencia_cardiaca) FROM monitoramento " +
            "WHERE usuario_id = :usuarioId AND data_hora BETWEEN :inicio AND :fim"
    )
    suspend fun mediaFrequenciaCardiaca(usuarioId: Long, inicio: Long, fim: Long): Double?
}
