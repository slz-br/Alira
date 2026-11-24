plugins {
    alias(libs.plugins.android.application)

    // KSP: gera o código do banco (Room) em tempo de compilação, igual ao :wear.
    id("com.google.devtools.ksp") version "2.3.11"
}

android {
    // namespace = pacote do código e da classe R. Continua o do app do grupo
    // (com.example.telainicial), para não precisar mexer em todas as telas.
    namespace = "com.example.telainicial"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // applicationId = "nome" do app instalado. TEM que ser igual ao do
        // relógio (wear/build.gradle.kts): é assim que o Android sabe que os
        // dois apps são do mesmo sistema e deixa um mandar mensagem ao outro.
        applicationId = "me.slz_br.alira"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.charts)
    implementation(libs.mpandroidchart)
    // CameraX 1.4.2 (antes 1.3.4): a partir da 1.4 a parte nativa da câmera
    // é compatível com celulares de páginas de memória de 16 KB (Android 15+).
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // graphics-path vem "de carona" com outras bibliotecas do Android; aqui ela
    // é puxada para a versão mais nova (1.1.0), também por causa dos 16 KB.
    implementation("androidx.graphics:graphics-path:1.1.0")

    // ---- Integração com o relógio ----
    // Room: banco local (SQLite) do celular: usuário, cuidador e as
    // leituras que chegam do relógio.
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    androidTestImplementation("androidx.room:room-testing:$room_version")

    // lifecycleScope: rodar tarefas do banco a partir das telas.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // Wearable Data Layer: o canal de mensagens entre celular e relógio.
    implementation(libs.play.services.wearable)
}

// Exporta o formato do banco a cada build (ajuda a escrever migrações).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
