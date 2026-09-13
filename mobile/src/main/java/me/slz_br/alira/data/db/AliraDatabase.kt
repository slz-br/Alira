package me.slz_br.alira.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.slz_br.alira.data.db.dao.CuidadorDao
import me.slz_br.alira.data.db.dao.RelatorioDao
import me.slz_br.alira.data.db.dao.UsuarioDao
import me.slz_br.alira.data.db.entities.CuidadorEntity
import me.slz_br.alira.data.db.entities.RelatorioEntity
import me.slz_br.alira.data.db.entities.UsuarioEntity

/**
 * Banco local (SQLite via Room) do app MOBILE do ALIRA.
 *
 * É um arquivo .db diferente do que existe no :wear — mobile e wear são
 * dois .apk instalados em dois aparelhos diferentes, cada um com seu
 * próprio armazenamento. Este banco só guarda o subconjunto de tabelas que
 * nascem no celular: Usuario (cadastro completo, RF08), Cuidador e
 * Relatorio. Monitoramento/Alerta continuam nascendo no relógio; o celular
 * só fica sabendo deles através das mensagens recebidas em
 * data/sync/MobileDataLayerService.kt, sem precisar de tabela própria por
 * enquanto.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        CuidadorEntity::class,
        RelatorioEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AliraDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun cuidadorDao(): CuidadorDao
    abstract fun relatorioDao(): RelatorioDao

    companion object {
        @Volatile
        private var instancia: AliraDatabase? = null

        fun getInstance(context: Context): AliraDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AliraDatabase::class.java,
                    "alira.db",
                ).build().also { instancia = it }
            }
    }
}
