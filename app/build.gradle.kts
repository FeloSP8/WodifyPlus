plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
    id("com.chaquo.python")
}

android {
    namespace = "com.example.wodifyplus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wodifyplus"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "6.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        chaquopy {
            defaultConfig {
                version = "3.10"
                pip {
                    install("python-dotenv")
                    install("requests")
                    install("beautifulsoup4")
                    install("lxml")
                }
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            assets.srcDirs("src/main/python")
        }
    }
    
    signingConfigs {
        getByName("debug") {
            // Usar el keystore de debug por defecto de Android
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // DataStore (para configuración)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager (para notificaciones)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.1")
    
    // Gráficas - Vico
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.28")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.28")
    implementation("com.patrykandpatrick.vico:core:2.0.0-alpha.28")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}