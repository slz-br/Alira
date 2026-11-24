package me.slz_br.alira.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.entities.CuidadorEntity

@Dao
interface CuidadorDao {

    @Insert
    suspend fun inserir(cuidador: CuidadorEntity): Long

    @Update
    suspend fun atualizar(cuidador: CuidadorEntity)

    @Delete
    suspend fun remover(cuidador: CuidadorEntity)

    @Query("SELECT * FROM cuidador WHERE usuario_id = :usuarioId ORDER BY nome")
    fun observarCuidadores(usuarioId: Long): Flow<List<CuidadorEntity>>

    // Versão "foto de agora" (sem Flow): usada quando chega o cuidador do
    // celular, para saber se já existe um para atualizar.
    @Query("SELECT * FROM cuidador WHERE usuario_id = :usuarioId ORDER BY id_cuidador")
    suspend fun obterCuidadores(usuarioId: Long): List<CuidadorEntity>

    @Query("SELECT * FROM cuidador WHERE id_cuidador = :idCuidador")
    suspend fun obterPorId(idCuidador: Long): CuidadorEntity?
}
