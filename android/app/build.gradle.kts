import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
    jacoco
}

val skipGoogleServices = providers.gradleProperty("skipGoogleServices").orNull.toBoolean()
if (file("google-services.json").isFile && !skipGoogleServices) {
    pluginManager.apply("com.google.gms.google-services")
} else {
    logger.lifecycle(
        "google-services.json not found; skipping Google Services plugin. " +
            "Firebase-dependent runtime features require this file."
    )
}

// ---------------------------------------------------------------------------
// Read BACKEND_BASE_URL from local.properties or CI environment variable.
// local.properties is git-ignored and must never be committed.
// ---------------------------------------------------------------------------
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val backendBaseUrl: String =
    localProperties.getProperty("backend.base.url")
        ?: System.getenv("BACKEND_BASE_URL")
        ?: ""

val enableCloudBackup: Boolean =
    (localProperties.getProperty("enable.cloud.backup")
        ?: System.getenv("ENABLE_CLOUD_BACKUP")
        ?: "false").toBoolean()

val enableInteractionCheck: Boolean =
    (localProperties.getProperty("enable.interaction.check")
        ?: System.getenv("ENABLE_INTERACTION_CHECK")
        ?: "false").toBoolean()

android {
    namespace = "com.medihistory.ai"
    compileSdk = 34

    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    defaultConfig {
        applicationId = "com.medihistory.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // BuildConfig field — value comes from local.properties or env var, never hardcoded.
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
        buildConfigField("boolean", "ENABLE_CLOUD_BACKUP", "$enableCloudBackup")
        buildConfigField("boolean", "ENABLE_INTERACTION_CHECK", "$enableInteractionCheck")

        // Room schema export — keeps annotationProcessorOptions path for compatibility.
        // The ksp { } block below is the active path when using KSP.
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

// KSP arguments — Room schema export directory.
// This is the active path since the project uses KSP (not kapt).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)

    // Activity / Compose entry point
    implementation(libs.androidx.activity.compose)

    // Compose BOM — version managed by the BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Hilt WorkManager integration
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    // Security — EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    // Lifecycle — collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Accompanist — runtime permissions in Compose
    implementation(libs.accompanist.permissions)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase — BOM manages all Firebase library versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)

    // Debug — Compose tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
