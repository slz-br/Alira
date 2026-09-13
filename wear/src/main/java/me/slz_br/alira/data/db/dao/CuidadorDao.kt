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

    @Query("SELECT * FROM cuidador WHERE id_cuidador = :idCuidador")
    suspend fun obterPorId(idCuidador: Long): CuidadorEntity?
}
