plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.kanou.reny"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.kanou.reny"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val keystoreFile = providers.environmentVariable("RENY_KEYSTORE_FILE")
    val keystorePassword = providers.environmentVariable("RENY_KEYSTORE_PASSWORD")
    val keyAlias = providers.environmentVariable("RENY_KEY_ALIAS")
    val keyPassword = providers.environmentVariable("RENY_KEY_PASSWORD")
    val hasReleaseSigning =
        keystoreFile.isPresent &&
            keystorePassword.isPresent &&
            keyAlias.isPresent &&
            keyPassword.isPresent

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFile.get())
                storePassword = keystorePassword.get()
                this.keyAlias = keyAlias.get()
                this.keyPassword = keyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

ktlint {
    android.set(true)
    version.set(libs.versions.ktlintVersion.get())
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt.yml")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.kotlinx.serialization.json)
}
