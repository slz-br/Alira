package me.slz_br.alira.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.entities.AlertaEntity

@Dao
interface AlertaDao {

    @Insert
    suspend fun inserir(alerta: AlertaEntity): Long

    @Query("UPDATE alerta SET status = :status WHERE id_alerta = :idAlerta")
    suspend fun atualizarStatus(idAlerta: Long, status: String)

    @Query("UPDATE alerta SET cuidador_id = :cuidadorId, status = :status WHERE id_alerta = :idAlerta")
    suspend fun definirCuidadorNotificado(idAlerta: Long, cuidadorId: Long, status: String)

    // Não há usuario_id em Alerta (o diagrama liga Alerta a Usuario só
    // indiretamente, via Monitoramento). Como cada relógio hoje guarda os
    // dados de um único usuário local, todos os alertas do banco já são
    // dele — por isso as consultas abaixo não filtram por usuário.
    @Query("SELECT * FROM alerta ORDER BY data_hora DESC")
    fun observarHistorico(): Flow<List<AlertaEntity>>

    @Query("SELECT * FROM alerta WHERE status = 'PENDENTE' ORDER BY data_hora DESC")
    fun observarPendentes(): Flow<List<AlertaEntity>>

    @Query("SELECT * FROM alerta WHERE cuidador_id = :cuidadorId ORDER BY data_hora DESC")
    fun observarParaCuidador(cuidadorId: Long): Flow<List<AlertaEntity>>
}
