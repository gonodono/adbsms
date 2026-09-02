plugins {
    id("com.android.application") version "9.4.0"
}

android {
    namespace = "dev.gonodono.adbsms.min"

    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "dev.gonodono.adbsms.min"
        minSdk = 19
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
    packaging {
        resources {
            // Several *.kotlin_builtins are always included, for some reason,
            // even if we use Groovy instead. This excludes everything kotlin.
            excludes.add("**/kotlin/**")
        }
    }
}