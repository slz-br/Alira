plugins {
    alias(libs.plugins.android.application)

    // KSP: mesma peça usada em wear/build.gradle.kts — gera a implementação
    // de cada @Dao em tempo de compilação. Precisa de UMA linha com versão
    // aqui, já que este módulo não usa "apply false" em nenhum lugar.
    id("com.google.devtools.ksp") version "2.3.11"
}

android {
    namespace = "me.slz_br.alira"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "me.slz_br.alira"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Room: mesmo banco local (SQLite) do relógio, agora no celular. É uma
    // instalação separada porque :mobile e :wear viram dois .apk distintos
    // (ver comentário em AliraDatabase.kt deste módulo).
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // lifecycle-runtime-ktx: dá acesso a "lifecycleScope" em qualquer
    // AppCompatActivity — o equivalente, numa Activity tradicional, do
    // rememberCoroutineScope() usado nas telas Compose do :wear.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.play.services.wearable)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // room-testing: dá suporte extra pra testar Room (ex.: MigrationTestHelper,
    // se um dia a Tabela usuario/cuidador/relatorio mudar de formato e
    // precisar de migração). Room.inMemoryDatabaseBuilder() usado nos testes
    // de mobile/src/androidTest/ já vem do room-runtime (dependência "implementation"
    // acima), então essa aqui não é estritamente obrigatória hoje — é
    // adicionada agora pra não faltar quando o capítulo de migração existir.
    androidTestImplementation("androidx.room:room-testing:$room_version")
}

// Mesmo raciocínio do :wear: exporta o schema a cada build (útil pra
// escrever migrações no futuro, se uma tabela mudar de formato).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}