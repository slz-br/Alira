package com.example.telainicial.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoramentoDao {

    // IGNORE: se a leitura já existe (mesmo idNoRelogio + dataHora), pula
    // ela em vez de dar erro. Devolve -1 para as que foram puladas.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirTodas(leituras: List<MonitoramentoEntity>): List<Long>

    // Média dos batimentos entre duas datas (null se não houver leitura).
    // É o valor de cada barra do gráfico.
    @Query(
        "SELECT AVG(frequencia_cardiaca) FROM monitoramento " +
            "WHERE frequencia_cardiaca IS NOT NULL AND data_hora >= :inicio AND data_hora < :fim"
    )
    suspend fun mediaBatimentos(inicio: Long, fim: Long): Double?

    // Últimas leituras de batimento, da mais nova para a mais antiga (lista
    // "Histórico"). Flow: a tela é avisada sozinha quando chegam leituras novas.
    @Query(
        "SELECT * FROM monitoramento WHERE frequencia_cardiaca IS NOT NULL " +
            "ORDER BY data_hora DESC LIMIT :limite"
    )
    fun observarUltimosBatimentos(limite: Int): Flow<List<MonitoramentoEntity>>
}
