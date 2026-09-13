package me.slz_br.alira.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.entities.AcessibilidadeEntity

@Dao
interface AcessibilidadeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(recursos: List<AcessibilidadeEntity>)

    @Query("SELECT * FROM acessibilidade WHERE usuario_id = :usuarioId ORDER BY id_acessibilidade")
    fun observarRecursos(usuarioId: Long): Flow<List<AcessibilidadeEntity>>

    @Query("SELECT COUNT(*) FROM acessibilidade WHERE usuario_id = :usuarioId")
    suspend fun contarPorUsuario(usuarioId: Long): Int

    @Query(
        "UPDATE acessibilidade SET ativo = :ativo " +
            "WHERE usuario_id = :usuarioId AND nome_acessibilidade = :nome"
    )
    suspend fun definirAtivo(usuarioId: Long, nome: String, ativo: Boolean)
}
