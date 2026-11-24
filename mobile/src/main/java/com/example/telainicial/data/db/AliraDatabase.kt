package com.example.telainicial.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.telainicial.data.db.dao.CuidadorDao
import com.example.telainicial.data.db.dao.MonitoramentoDao
import com.example.telainicial.data.db.dao.RelatorioDao
import com.example.telainicial.data.db.dao.UsuarioDao
import com.example.telainicial.data.db.entities.CuidadorEntity
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import com.example.telainicial.data.db.entities.RelatorioEntity
import com.example.telainicial.data.db.entities.UsuarioEntity

/**
 * Banco local (SQLite via Room) do app do CELULAR do ALIRA.
 *
 * É um arquivo .db diferente do que existe no relógio: são dois aparelhos,
 * cada um com o seu armazenamento.
 *
 * Versão 2: ganhou a tabela "monitoramento", com as leituras que chegam do
 * relógio (para os gráficos). A versão 1 tinha só usuario, cuidador e
 * relatorio.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        CuidadorEntity::class,
        RelatorioEntity::class,
        MonitoramentoEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AliraDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun cuidadorDao(): CuidadorDao
    abstract fun relatorioDao(): RelatorioDao
    abstract fun monitoramentoDao(): MonitoramentoDao

    companion object {
        @Volatile
        private var instancia: AliraDatabase? = null

        /**
         * Migração 1 -> 2: só CRIA a tabela nova. Quem já tinha o app
         * instalado não perde o usuário nem o cuidador salvos.
         * O SQL é exatamente o que o Room espera para MonitoramentoEntity.
         */
        val MIGRACAO_1_PARA_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `monitoramento` (" +
                        "`id_monitoramento` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`id_no_relogio` INTEGER NOT NULL, " +
                        "`data_hora` INTEGER NOT NULL, " +
                        "`frequencia_cardiaca` REAL, " +
                        "`passos` INTEGER, " +
                        "`calorias` REAL, " +
                        "`distancia` REAL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_monitoramento_id_no_relogio_data_hora` " +
                        "ON `monitoramento` (`id_no_relogio`, `data_hora`)"
                )
            }
        }

        fun getInstance(context: Context): AliraDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AliraDatabase::class.java,
                    "alira.db",
                )
                    .addMigrations(MIGRACAO_1_PARA_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
