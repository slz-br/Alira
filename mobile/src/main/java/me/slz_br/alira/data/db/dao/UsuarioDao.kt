package me.slz_br.alira.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.entities.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(usuario: UsuarioEntity): Long

    @Update
    suspend fun atualizar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario WHERE email = :email LIMIT 1")
    suspend fun obterPorEmail(email: String): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE id_usuario = :id LIMIT 1")
    suspend fun obterPorId(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE id_usuario = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<UsuarioEntity?>
}
