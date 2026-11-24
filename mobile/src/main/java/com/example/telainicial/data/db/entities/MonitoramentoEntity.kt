package com.example.telainicial.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Classe "Monitoramento" do diagrama, agora também no celular.
 *
 * As leituras NASCEM no relógio (sensor de batimento, passos etc.). Quando a
 * pessoa toca em "Gerar relatório" no relógio, ele manda para cá as leituras
 * novas (ver data/sync/MobileDataLayerService.kt), e o celular guarda
 * nesta tabela para montar os gráficos da tela de Batimentos.
 *
 * Campos iguais aos do relógio, mais um: idNoRelogio, o número que a
 * leitura tem no banco do relógio. Ele, junto com a data/hora, forma um
 * índice ÚNICO: se a mesma leitura chegar duas vezes, a segunda é ignorada
 * (não duplica pontos no gráfico).
 *
 * Não tem coluna de usuário: o celular só tem um usuário (o dono).
 */
@Entity(
    tableName = "monitoramento",
    indices = [Index(value = ["id_no_relogio", "data_hora"], unique = true)],
)
data class MonitoramentoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_monitoramento")
    val idMonitoramento: Long = 0L,

    @ColumnInfo(name = "id_no_relogio")
    val idNoRelogio: Long,

    @ColumnInfo(name = "data_hora")
    val dataHora: Long,

    @ColumnInfo(name = "frequencia_cardiaca")
    val frequenciaCardiaca: Double? = null,

    @ColumnInfo(name = "passos")
    val passos: Long? = null,

    @ColumnInfo(name = "calorias")
    val calorias: Double? = null,

    @ColumnInfo(name = "distancia")
    val distancia: Double? = null,
)
