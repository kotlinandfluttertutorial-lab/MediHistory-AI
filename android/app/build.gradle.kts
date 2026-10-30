import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ktlint)
    jacoco
}

ktlint {
    filter {
        include("**/src/**/*.kt")
    }
}

tasks.matching {
    it.name == "ktlintKotlinScriptCheck" || it.name == "ktlintKotlinScriptFormat"
}.configureEach {
    enabled = false
}

val skipGoogleServices = providers.gradleProperty("skipGoogleServices").orNull.toBoolean()
if (file("google-services.json").isFile && !skipGoogleServices) {
    pluginManager.apply("com.google.gms.google-services")
} else {
    logger.lifecycle(
        "google-services.json not found; skipping Google Services plugin. " +
            "Firebase-dependent runtime features require this file.",
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
    (
        localProperties.getProperty("enable.cloud.backup")
            ?: System.getenv("ENABLE_CLOUD_BACKUP")
            ?: "false"
    ).toBoolean()

val enableInteractionCheck: Boolean =
    (
        localProperties.getProperty("enable.interaction.check")
            ?: System.getenv("ENABLE_INTERACTION_CHECK")
            ?: "false"
    ).toBoolean()

val geminiApiKey: String =
    localProperties.getProperty("gemini.api.key")
        ?: System.getenv("GEMINI_API_KEY")
        ?: ""

val enableGeminiOcr: Boolean =
    (
        localProperties.getProperty("enable.gemini.ocr")
            ?: System.getenv("ENABLE_GEMINI_OCR")
            ?: "false"
    ).toBoolean()

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
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("boolean", "ENABLE_GEMINI_OCR", "$enableGeminiOcr")

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
                "proguard-rules.pro",
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
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
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

    // AppCompat — required to satisfy resource references pulled in by ML Kit text-recognition
    implementation(libs.androidx.appcompat)
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

    // ML Kit — on-device text recognition, bundled (offline-capable, no API key required)
    implementation(libs.mlkit.text.recognition)

    // Gemini — generative AI SDK for prescription OCR (Gemini 1.5 Flash)
    implementation(libs.generativeai)
    implementation(libs.google.genai.kotlin)

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

// ---------------------------------------------------------------------------
// JaCoCo — coverage report and enforcement gate
// ---------------------------------------------------------------------------
// Run with: ./gradlew :app:jacocoTestReport (report only)
//           ./gradlew :app:jacocoTestCoverageVerification (gate — fails if below threshold)
// Both tasks depend on :app:testDebugUnitTest so they run tests automatically.
// ---------------------------------------------------------------------------

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates JaCoCo code coverage reports for the debug unit tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Class files to include in the report (exclude generated / non-business code).
    val buildDir = project.layout.buildDirectory.asFile.get()
    val classesDir =
        fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class", "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*_Hilt*.*", "**/Hilt_*.*", // Hilt generated
                "**/*_MembersInjector*.*",
                "**/*_Factory*.*",
                "**/*Dao_Impl*.*", // Room generated
                "**/databinding/**",
                "**/ComposableSingletons*.*", // Compose generated
            )
        }
    classDirectories.setFrom(classesDir)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree("$buildDir/jacoco") { include("*.exec") })
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")
    group = "verification"
    description = "Enforces JaCoCo coverage thresholds (data layer ≥80%, DuplicateDetector/ValidationUtils 100% branch)."

    val buildDir = project.layout.buildDirectory.asFile.get()
    val classesDir =
        fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
                "**/*_Hilt*.*", "**/Hilt_*.*", "**/*_MembersInjector*.*", "**/*_Factory*.*",
                "**/*Dao_Impl*.*", "**/databinding/**", "**/ComposableSingletons*.*",
            )
        }
    classDirectories.setFrom(classesDir)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree("$buildDir/jacoco") { include("*.exec") })

    violationRules {
        // ≥80% line coverage for the data layer (repositories, DAOs, use-cases, OCR).
        rule {
            element = "PACKAGE"
            includes = listOf("com.medihistory.ai.data.*")
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        // 100% branch coverage for DuplicateDetector (medical-safety critical path).
        rule {
            element = "CLASS"
            includes = listOf("com.medihistory.ai.duplicate.DuplicateDetector")
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
        // 100% branch coverage for ValidationUtils (medical-safety critical path).
        rule {
            element = "CLASS"
            includes = listOf("com.medihistory.ai.data.ocr.ValidationUtils")
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}
