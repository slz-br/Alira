plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // KSP: só UMA linha é necessária aqui. O erro anterior veio de declarar
    // o plugin duas vezes (uma com "apply false" e outra sem) no mesmo
    // arquivo. "apply false" só faz sentido no build.gradle.kts da RAIZ do
    // projeto (deixar a versão disponível pra vários módulos sem aplicar
    // ali); dentro de um módulo como este, queremos aplicar de verdade.
    // Versão atualizada pra mais recente do KSP (github.com/google/ksp/releases).
    id("com.google.devtools.ksp") version "2.3.11"
}

android {
    namespace = "me.slz_br.alira"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "me.slz_br.alira"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Room: banco de dados local (SQLite) do ALIRA.
    // - room-runtime/room-ktx rodam DENTRO do app (por isso "implementation").
    // - room-ktx especificamente é o que dá suporte a Flow<...> nas queries
    //   (sem ela, um @Dao que retorne Flow não compila).
    // - room-compiler só roda DURANTE a compilação, gerando a implementação
    //   de cada @Dao sozinho — por isso usa ksp(...), e não implementation(...).
    val room_version = "2.8.4"

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.compose.ui.tooling)
    implementation(libs.play.services.wearable)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.androidx.fragment)

    implementation(libs.androidx.health.services.client)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
}

// Pede ao Room pra exportar, a cada build, um "retrato" do formato das
// tabelas nesse momento (pasta schemas/). Não é obrigatório pro app rodar,
// mas evita dor de cabeça no futuro: é a partir desses arquivos que se
// escreve uma Migration quando uma tabela precisar mudar de formato depois
// que o app já estiver em uso.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}