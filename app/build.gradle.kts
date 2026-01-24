import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id ("com.android.application")
}

android {
    namespace = "dev.gonodono.adbsms"

    compileSdk {
        version = release(36)
    }
    defaultConfig {
        applicationId = "dev.gonodono.adbsms"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "0.0.10"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
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

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}