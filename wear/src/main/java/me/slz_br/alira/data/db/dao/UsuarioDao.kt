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

    // O app ainda não tem tela de cadastro/login (RF08); o smartwatch
    // opera com um único perfil local: o primeiro cadastrado no aparelho.
    @Query("SELECT * FROM usuario ORDER BY id_usuario LIMIT 1")
    fun observarUsuarioAtual(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuario ORDER BY id_usuario LIMIT 1")
    suspend fun obterUsuarioAtual(): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE email = :email LIMIT 1")
    suspend fun obterPorEmail(email: String): UsuarioEntity?
}
