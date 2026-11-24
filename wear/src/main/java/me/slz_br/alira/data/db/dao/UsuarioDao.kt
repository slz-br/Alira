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

    // Usuário que veio do cadastro do celular (o mais recente), ignorando o
    // usuário provisório "usuario@local" que garantirUsuarioLocal() cria
    // quando o relógio ainda não recebeu nenhum cadastro. É desse usuário
    // que a emergência precisa: deficiência (como avisar) e e-mail (para
    // o celular achar os contatos de emergência).
    @Query("SELECT * FROM usuario WHERE email != 'usuario@local' ORDER BY id_usuario DESC LIMIT 1")
    suspend fun obterUsuarioSincronizado(): UsuarioEntity?

    // "Usuário ativo" = quem está usando o relógio agora: o cadastro mais
    // recente que veio do celular; se ainda não veio nenhum, o provisório
    // "usuario@local". O truque do ORDER BY: (email = 'usuario@local') vale
    // 0 para os cadastros reais e 1 para o provisório, então os reais vêm
    // primeiro; entre eles, o de id maior (o mais novo).
    @Query(
        "SELECT * FROM usuario " +
            "ORDER BY (email = 'usuario@local') ASC, id_usuario DESC LIMIT 1"
    )
    fun observarUsuarioAtivo(): Flow<UsuarioEntity?>

    @Query(
        "SELECT * FROM usuario " +
            "ORDER BY (email = 'usuario@local') ASC, id_usuario DESC LIMIT 1"
    )
    suspend fun obterUsuarioAtivo(): UsuarioEntity?
}
