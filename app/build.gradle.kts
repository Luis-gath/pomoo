plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.pomodoro"
    compileSdk = 34

    defaultConfig {
        // Google Play rechaza cualquier paquete que empiece por com.example, y este es
        // ademas el identificador para el que esta emitido google-services.json.
        // El namespace sigue siendo com.example.pomodoro: solo afecta a donde se generan
        // R y BuildConfig, y cambiarlo obligaria a renombrar el paquete en todo el codigo.
        applicationId = "com.luis.pomodoro"
        minSdk = 26 // Android 8.0 (Oreo) per user request
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Por defecto la build de debug usa su propio applicationId, para poder tener
            // dev y producción instaladas a la vez en el mismo dispositivo.
            //
            // Con -PsameAppId se compila sin sufijo, de modo que el APK actualice
            // una instalación existente en lugar de instalarse al lado:
            //     ./gradlew assembleDebug -PsameAppId
            if (!project.hasProperty("sameAppId")) {
                applicationIdSuffix = ".debug"
                versionNameSuffix = "-dev"
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Estos detectores de Compose, en la versión de lint que trae AGP 8.7.3, revientan
        // con NullPointerException al analizar código Kotlin 2.0. Son fallos de la
        // herramienta, no hallazgos reales: sin desactivarlos `lintDebug` (y por tanto el
        // CI) falla siempre. Conviene reactivarlos al actualizar AGP.
        disable += "AutoboxingStateCreation"
        disable += "MutableCollectionMutableState"
    }
}

ksp {
    // Exporta el esquema de Room a app/schemas para poder escribir migraciones reales.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.coil.compose)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.google.code.scanner)

    // Firebase Authentication
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Credential Manager: la via actual para Google Sign-In.
    // GoogleSignInClient (play-services-auth) esta obsoleto.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Glance Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
