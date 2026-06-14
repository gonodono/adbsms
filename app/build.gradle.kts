plugins {
    id("com.android.application") version "9.2.1"
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
        versionCode = 11
        versionName = "0.0.11"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}