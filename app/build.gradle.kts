plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.license.report)
}

android {
    namespace = "dev.danbrada.wallpaperexporter"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.danbrada.wallpaperexporter"
        versionCode = 9
        versionName = "1.1.4"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    // No keystore is committed to this repo. Supply your own via project properties to sign a
    // release build (e.g. `--project-prop keystore=... --project-prop keystorepassword=... \
    // --project-prop keystorealias=... --project-prop keystorekeypassword=...`); without one,
    // `assembleRelease` produces an unsigned APK. Day-to-day testing should use `assembleDebug`.
    if (project.hasProperty("keystore")) {
        signingConfigs {
            create("release") {
                storeFile = file(project.property("keystore") as String)
                storePassword = project.property("keystorepassword") as String
                keyAlias = project.property("keystorealias") as String
                keyPassword = project.property("keystorekeypassword") as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (project.hasProperty("keystore")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.bundles.implementation.app)
}

configurations.all {
    resolutionStrategy {
        // androidx.core:core 1.14.0+ raises minSdk to 23 (this app supports minSdk 22). Pinning
        // androidx-core in libs.versions.toml only sets what core-ktx *requests*; appcompat/material
        // /lifecycle also pull in androidx.core:core transitively, and Gradle resolves conflicts to
        // the highest requested version. Force it so a newer transitive request can't win.
        force("androidx.core:core:${libs.versions.androidx.core.get()}")
    }
}

licenseReport {
    // Run via `gradlew licenseReleaseReport`
    generateCsvReport = false
    generateHtmlReport = true
    generateJsonReport = false
    copyHtmlReportToAssets = true
}
