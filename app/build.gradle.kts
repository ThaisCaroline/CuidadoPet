import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cuidadopet"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.cuidadopet"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Configuração de assinatura para o release.
    // As credenciais são lidas de local.properties para nunca serem commitadas no git.
    // Para gerar o keystore, execute no terminal:
    //   keytool -genkey -v -keystore cuidadopet.jks -keyalg RSA -keysize 2048 \
    //           -validity 10000 -alias cuidadopet
    // Depois adicione ao local.properties:
    //   KEYSTORE_PATH=../cuidadopet.jks
    //   KEYSTORE_PASSWORD=sua_senha
    //   KEY_ALIAS=cuidadopet
    //   KEY_PASSWORD=sua_senha
    signingConfigs {
        create("release") {
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) {
                val props = Properties()
                localFile.inputStream().use { stream -> props.load(stream) }
                val keystorePath = props.getProperty("KEYSTORE_PATH")
                if (!keystorePath.isNullOrBlank()) {
                    storeFile     = file(keystorePath)
                    storePassword = props.getProperty("KEYSTORE_PASSWORD")
                    keyAlias      = props.getProperty("KEY_ALIAS")
                    keyPassword   = props.getProperty("KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            // isMinifyEnabled remove código não utilizado (reduz tamanho do APK/AAB)
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose + Android core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Splash Screen — mostra a tela de abertura com a patinha enquanto o app inicia
    implementation(libs.androidx.core.splashscreen)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Vico (gráficos)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Ícones estendidos — Medication, Remove, Pets e outros não incluídos no pacote padrão
    implementation(libs.androidx.compose.material.icons.extended)

    // Coil — carregamento assíncrono de imagens para Compose (foto dos pets)
    implementation(libs.coil.compose)

    // SQLCipher + Security Crypto — banco de dados cifrado com AES-256
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)
    implementation(libs.security.crypto)

    // Gson — serialização JSON para backup/restauração de dados
    implementation(libs.gson)

    // Play In-App Updates — popup de atualização quando há nova versão na Play Store
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
