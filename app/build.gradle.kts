plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sting.overtimewagecalc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sting.overtimewagecalc"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
        vectorDrawables { useSupportLibrary = true }
    }

    // 显式声明 debug signing config(绕开 AGP 8+ build cache 用默认 keystore 的坑)
    // 不依赖 ~/.android/debug.keystore,直接读仓库里的固定 keystore
    signingConfigs {
        create("stingDebug") {
            val keystoreFile = rootProject.file(".github/keystore.b64")
            // 把 base64 解码后写到临时文件
            val decoded = file("$buildDir/decoded-debug.keystore")
            val keystoreBytes = java.util.Base64.getDecoder().decode(keystoreFile.readBytes())
            decoded.parentFile.mkdirs()
            decoded.writeBytes(keystoreBytes)
            storeFile = decoded
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stingDebug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}