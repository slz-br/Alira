package me.slz_br.alira.presentation

import android.Manifest
import android.os.Build

private const val API_WEAR_OS_6 = 36

const val PERMISSAO_LER_FREQUENCIA_CARDIACA = "android.permission.health.READ_HEART_RATE"

val permissaoFrequenciaCardiaca: String
    get() = if (Build.VERSION.SDK_INT >= API_WEAR_OS_6) {
        PERMISSAO_LER_FREQUENCIA_CARDIACA
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.BODY_SENSORS
    }

val permissoesSinaisVitais: List<String>
    get() = listOf(
        Manifest.permission.ACTIVITY_RECOGNITION,
        permissaoFrequenciaCardiaca,
    )

const val PERMISSAO_LER_EM_SEGUNDO_PLANO = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

@Suppress("DEPRECATION")
const val PERMISSAO_SENSORES_EM_SEGUNDO_PLANO = "android.permission.BODY_SENSORS_BACKGROUND"

val permissaoSegundoPlano: String
    get() = if (Build.VERSION.SDK_INT >= API_WEAR_OS_6) {
        PERMISSAO_LER_EM_SEGUNDO_PLANO
    } else {
        PERMISSAO_SENSORES_EM_SEGUNDO_PLANO
    }
