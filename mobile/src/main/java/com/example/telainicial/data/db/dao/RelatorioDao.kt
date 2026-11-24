package com.example.telainicial.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.telainicial.data.db.entities.RelatorioEntity

@Dao
interface RelatorioDao {

    @Insert
    suspend fun inserir(relatorio: RelatorioEntity): Long

    @Query("SELECT * FROM relatorio WHERE usuario_id = :usuarioId ORDER BY data_geracao DESC")
    fun observarHistorico(usuarioId: Long): Flow<List<RelatorioEntity>>
}
