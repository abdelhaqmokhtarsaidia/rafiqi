plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.rafiqi.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.rafiqi.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("rafiqi") {
            storeFile = file("rafiqi.keystore")
            storePassword = "rafiqi2026"
            keyAlias = "rafiqi"
            keyPassword = "rafiqi2026"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("rafiqi")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += listOf("mp3") }
}
dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
