plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.gonodono.adbsms"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.gonodono.adbsms"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "0.0.8"
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
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}