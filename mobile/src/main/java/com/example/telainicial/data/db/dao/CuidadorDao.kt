package com.example.telainicial.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.telainicial.data.db.entities.CuidadorEntity

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

    // Versão "tira uma foto agora" da consulta acima (sem Flow): usada
    // quando chega uma emergência do relógio e o app precisa da lista de
    // contatos uma vez só, para mandar o SMS.
    @Query("SELECT * FROM cuidador WHERE usuario_id = :usuarioId ORDER BY nome")
    suspend fun obterCuidadores(usuarioId: Long): List<CuidadorEntity>
}
