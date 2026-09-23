plugins {
    id("com.android.application") version "9.4.1"
}

android {
    namespace = "dev.gonodono.adbsms"

    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "dev.gonodono.adbsms"
        minSdk = 24
        targetSdk = 37

        val code =
            findProperty("app.versionCode")?.toString()?.toIntOrNull()
                ?: error("Missing or invalid app.versionCode")
        versionCode = code
        versionName = "0.0.$code"
    }
    buildTypes {
        release {
            optimization { enable = true }
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}