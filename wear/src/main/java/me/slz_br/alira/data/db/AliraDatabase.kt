package me.slz_br.alira.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.slz_br.alira.data.db.dao.AcessibilidadeDao
import me.slz_br.alira.data.db.dao.AlertaDao
import me.slz_br.alira.data.db.dao.CuidadorDao
import me.slz_br.alira.data.db.dao.MonitoramentoDao
import me.slz_br.alira.data.db.dao.RelatorioDao
import me.slz_br.alira.data.db.dao.UsuarioDao
import me.slz_br.alira.data.db.entities.AcessibilidadeEntity
import me.slz_br.alira.data.db.entities.AlertaEntity
import me.slz_br.alira.data.db.entities.CuidadorEntity
import me.slz_br.alira.data.db.entities.MonitoramentoEntity
import me.slz_br.alira.data.db.entities.RelatorioEntity
import me.slz_br.alira.data.db.entities.UsuarioEntity

/**
 * Banco local (SQLite via Room) do app ALIRA, espelhando o diagrama de
 * classes do capítulo 5.4 do TCC: Usuario, Acessibilidade, Monitoramento,
 * Alerta, Relatorio e Cuidador.
 *
 * A classe "Smartwatch" do diagrama NÃO virou tabela: o relógio é o
 * próprio app rodando, não um dado a se guardar sobre si mesmo — guardar
 * uma linha "smartwatch" só faria sentido se o sistema precisasse
 * rastrear vários dispositivos pareados por usuário, o que não é o caso
 * hoje. Se isso mudar (ex.: RF09 — pareamento com o celular, múltiplos
 * relógios por conta), a entidade volta a fazer sentido.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        AcessibilidadeEntity::class,
        MonitoramentoEntity::class,
        AlertaEntity::class,
        RelatorioEntity::class,
        CuidadorEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AliraDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun acessibilidadeDao(): AcessibilidadeDao
    abstract fun monitoramentoDao(): MonitoramentoDao
    abstract fun alertaDao(): AlertaDao
    abstract fun relatorioDao(): RelatorioDao
    abstract fun cuidadorDao(): CuidadorDao

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
