# Implementation Plan — MHA-001: Android Gradle Project Scaffold

**Output file:** `android/.agents/tasks/plan.md`
**Target directory:** `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\`
**Written by:** Planning agent (workflow wf_bc43aa3db36e7da3)

---

## Context and Decisions

**Gradle version:** AGP 8.4.2 requires a minimum of Gradle 8.6. Use **Gradle 8.7** — the stable release that shipped alongside AGP 8.4 and is the pairing recommended in Android Studio Hedgehog/Iguana toolchain documentation. This gives a clean match without pulling in a newer Gradle that hasn't been validated against AGP 8.4.2.

**Gradle wrapper distribution type:** `bin` (not `all`). The `all` variant downloads sources and is only needed for IDE navigation inside Gradle itself — not required for CI or coder builds. This keeps the first-run download smaller.

**`composeOptions` placement:** `composeOptions { kotlinCompilerExtensionVersion = … }` goes inside the `android {}` block in `app/build.gradle.kts`, not at the top level. This is the correct DSL location for the Kotlin Compose compiler extension version when NOT using the new Compose Compiler Gradle Plugin (which arrived in Kotlin 2.x). Because `kotlin = "1.9.24"` is pinned here, the old `composeOptions` block inside `android {}` is the correct approach.

**`buildFeatures`:** Both `compose = true` and `buildConfig = true` are enabled in `app/build.gradle.kts` under `android { buildFeatures { … } }`.

**`BACKEND_BASE_URL` strategy:** Read from `local.properties` key `backend.base.url` at Gradle sync time, fall back to the `BACKEND_BASE_URL` environment variable, and fall back to an empty string. This logic lives in the `app/build.gradle.kts` `defaultConfig` block using a `Properties` load pattern — no secrets are hardcoded.

**Theme colours:** Defined as `Color` constants in `Color.kt`. The `warning` and `warningContainer` tokens are custom extensions added to `MediHistoryColorScheme` (a wrapper data class) rather than shoved into Material 3's `ColorScheme` directly, because Material 3's `ColorScheme` has no `warning` slot. The wrapper is accessed via `MaterialTheme.mediHistoryColors` using a `CompositionLocal`. This is the standard Material 3 extension pattern.

**`Shape.kt`:** Defines a `MediHistoryShapes` instance using `RoundedCornerShape` tokens (extra-small 4dp, small 8dp, medium 12dp, large 16dp, extra-large 28dp) matching Material 3's default shape scale — no custom values needed at scaffold stage; it's a named set for consistency.

**`gradlew` / `gradlew.bat`:** These are standard Gradle wrapper scripts. The plan provides exact content. The Unix script must have LF line endings; on Windows the coder must ensure the file is written with LF endings (not CRLF) or the script will fail on Linux CI. Use the standard Gradle 8.7 wrapper script text verbatim.

**`local.properties` absence:** The file must NOT be present under `android/`. The root `.gitignore` already covers `local.properties` at any depth. Verify with a `git status` check after scaffolding.

**Test files:** `ExampleUnitTest` and `ExampleInstrumentedTest` use synthetic content — no real package, no real data, just a minimal `@Test` to let `testDebugUnitTest` pass without a device.

**`proguard-rules.pro`:** Minimal placeholder file. No rules are needed at scaffold stage; Compose and Hilt consumer rules come from the libraries' AAR files.

---

## Implementation Order

The files must be created in dependency order: wrapper scripts first (Gradle needs them to run), then the catalogue (build files reference it), then build files (they reference the catalogue), then source files (they reference the build output: applicationId, theme).

---

## Plan

- [ ] 1. Create `android/gradle/wrapper/gradle-wrapper.properties`

  Defines the Gradle distribution to download. Use Gradle 8.7 `bin` distribution.

  **File:** `android/gradle/wrapper/gradle-wrapper.properties`

  **Exact content:**
  ```properties
  distributionBase=GRADLE_USER_HOME
  distributionPath=wrapper/dists
  distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
  networkTimeout=10000
  validateDistributionUrl=true
  zipStoreBase=GRADLE_USER_HOME
  zipStorePath=wrapper/dists
  ```

  **Verify:** File exists at the correct path. Content check is implicit — Gradle will refuse to start with a malformed URL.

---

- [ ] 2. Create `android/gradlew` (Unix wrapper script)

  Standard Gradle wrapper shell script for Linux/macOS (used by CI). Must be written with **LF** line endings and marked executable in git (`git update-index --chmod=+x android/gradlew`).

  **File:** `android/gradlew`

  **Exact content** — use the canonical Gradle 8.7 wrapper script. The full script text is reproduced below. Write it verbatim; do not alter whitespace or line endings.

  ```sh
  #!/bin/sh
  #
  # Copyright © 2015-2021 the original authors.
  #
  # Licensed under the Apache License, Version 2.0 (the "License");
  # you may not use this file except in compliance with the License.
  # You may obtain a copy of the License at
  #
  #      https://www.apache.org/licenses/LICENSE-2.0
  #
  # Unless required by applicable law or agreed to in writing, software
  # distributed under the License is distributed on an "AS IS" BASIS,
  # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  # See the License for the specific language governing permissions and
  # limitations under the License.
  #
  # SPDX-License-Identifier: Apache-2.0
  #

  ##############################################################################
  #
  #   Gradle start up script for POSIX generated by "Gradle Init".
  #
  #   Important for running:
  #
  #   (1) You need a POSIX-compliant shell to run this script. If your /bin/sh is
  #       noncompliant, but you have some other POSIX-compliant shell such as ksh or
  #       bash, then to run this script, type that shell name before the whole
  #       command line, like:
  #
  #           ksh Gradle
  #
  #       Busybox and similar reduced shells will NOT work, because this script
  #       requires all these POSIX shell features:
  #         * functions;
  #         * expansions «$var», «${var}», «${var:-default}», «${var+SET}»,
  #           «${var#prefix}», «${var%suffix}», and «$( cmd )»;
  #         * compound commands having a testable exit status, especially «case»;
  #         * various built-in commands including «command», «set», and «ulimit».
  #
  #   Important for environment:
  #
  #   (2) You can specify JVM arguments via GRADLE_OPTS, e.g.
  #           export GRADLE_OPTS=-Xmx512m
  #
  #   (3) You can specify JAVA_HOME to get control over the JVM used.
  #
  #   (4) Gradle daemon is controlled by GRADLE_DAEMON_... settings.
  #
  ##############################################################################

  # Attempt to set APP_HOME

  # Resolve links: $0 may be a link
  app_path=$0

  # Need this for daisy-chained symlinks.
  while
      APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
      [ -h "$app_path" ]
  do
      ls=$( ls -ld "$app_path" )
      link=${ls#*' -> '}
      case $link in             #(
        /*)   app_path=$link ;; #( absolute
        *)    app_path=$APP_HOME$link ;; #( relative
      esac
  done

  # This is reliable if readlink is available.
  APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

  APP_NAME="Gradle"
  APP_BASE_NAME=${0##*/}

  # Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
  DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

  # Use the maximum available, or set MAX_FD != -1 to use that value.
  MAX_FD=maximum

  warn () {
      echo "$*"
  } >&2

  die () {
      echo
      echo "$*"
      echo
      exit 1
  } >&2

  # OS specific support (must be 'true' or 'false').
  cygwin=false
  msys=false
  darwin=false
  nonstop=false
  case "$( uname )" in                #(
    CYGWIN* )         cygwin=true  ;; #(
    Darwin* )         darwin=true  ;; #(
    MSYS* | MINGW* )  msys=true    ;; #(
    NONSTOP* )        nonstop=true ;;
  esac

  CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

  # Determine the Java command to use to start the JVM.
  if [ -n "$JAVA_HOME" ] ; then
      if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
          # IBM's JDK on AIX uses strange locations for the executables
          JAVACMD=$JAVA_HOME/jre/sh/java
      else
          JAVACMD=$JAVA_HOME/bin/java
      fi
      if [ ! -x "$JAVACMD" ] ; then
          die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

  Please set the JAVA_HOME variable in your environment to match the
  location of your Java installation."
      fi
  else
      JAVACMD=java
      which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

  Please set the JAVA_HOME environment variable in your environment to match the
  location of your Java installation."
  fi

  # Increase the maximum file descriptors if we can.
  if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
      case $MAX_FD in #(
        max*)
          # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
          # shellcheck disable=SC2039,SC3045
          MAX_FD=$( ulimit -H -n ) ||
              warn "Could not query maximum file descriptor limit"
          ;;
      esac
      case $MAX_FD in  #(
        '' | soft) :;; #(
        *)
          # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
          # shellcheck disable=SC2039,SC3045
          ulimit -n "$MAX_FD" ||
              warn "Could not set maximum file descriptor limit to $MAX_FD"
          ;;
      esac
  fi

  # Collect all arguments for the java command, following the shell quoting and substitution rules
  eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$@"

  exec "$JAVACMD" "$@"
  ```

  After writing: `git update-index --chmod=+x android/gradlew`

  **Verify:** File exists. Executable bit confirmed with `git ls-files --stage android/gradlew` (should show mode `100755`).

---

- [ ] 3. Create `android/gradlew.bat` (Windows wrapper script)

  Standard Gradle wrapper batch script for Windows. Used when running `.\gradlew.bat` locally and in CI on Windows agents.

  **File:** `android/gradlew.bat`

  **Exact content:**
  ```bat
  @rem
  @rem Copyright 2015 the original author or authors.
  @rem
  @rem Licensed under the Apache License, Version 2.0 (the "License");
  @rem you may not use this file except in compliance with the License.
  @rem You may obtain a copy of the License at
  @rem
  @rem      https://www.apache.org/licenses/LICENSE-2.0
  @rem
  @rem Unless required by applicable law or agreed to in writing, software
  @rem distributed under the License is distributed on an "AS IS" BASIS,
  @rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  @rem See the License for the specific language governing permissions and
  @rem limitations under the License.
  @rem
  @rem SPDX-License-Identifier: Apache-2.0
  @rem

  @if "%DEBUG%"=="" @echo off
  @rem ##########################################################################
  @rem
  @rem  Gradle startup script for Windows
  @rem
  @rem ##########################################################################

  @rem Set local scope for the variables with windows NT shell
  if "%OS%"=="Windows_NT" setlocal

  set DIRNAME=%~dp0
  if "%DIRNAME%"=="" set DIRNAME=.
  @rem This is normally unused
  set APP_BASE_NAME=%~n0
  set APP_HOME=%DIRNAME%

  @rem Resolve any "." and ".." in APP_HOME to make it shorter.
  for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

  @rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
  set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

  @rem Find java.exe
  if defined JAVA_HOME goto findJavaFromJavaHome

  set JAVA_EXE=java.exe
  %JAVA_EXE% -version >NUL 2>&1
  if %ERRORLEVEL% equ 0 goto execute

  echo. 1>&2
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
  echo. 1>&2
  echo Please set the JAVA_HOME variable in your environment to match the 1>&2
  echo location of your Java installation. 1>&2

  goto fail

  :findJavaFromJavaHome
  set JAVA_HOME=%JAVA_HOME:"=%
  set JAVA_EXE=%JAVA_HOME%/bin/java.exe

  if exist "%JAVA_EXE%" goto execute

  echo. 1>&2
  echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
  echo. 1>&2
  echo Please set the JAVA_HOME variable in your environment to match the 1>&2
  echo location of your Java installation. 1>&2

  goto fail

  :execute
  @rem Setup the command line

  set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

  @rem Execute Gradle
  %JAVA_EXE% %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

  :end
  @rem End local scope for the variables with windows NT shell
  if %ERRORLEVEL% equ 0 goto mainEnd

  :fail
  rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
  rem the _cmd_ return code!
  set EXIT_CODE=%ERRORLEVEL%
  if %EXIT_CODE% equ 0 set EXIT_CODE=1
  if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
  exit /b %EXIT_CODE%

  :mainEnd
  if "%OS%"=="Windows_NT" endlocal

  :omega
  ```

  **Verify:** File exists.

---

- [ ] 4. Download `gradle-wrapper.jar`

  The Gradle wrapper JAR must be present at `android/gradle/wrapper/gradle-wrapper.jar`. It is a binary bootstrap launcher committed to the repo. The standard way to obtain it is to run `gradle wrapper` from a system Gradle installation, or copy it from an existing Android project.

  **Steps:**
  1. If a local Gradle 8.7 installation is available: `cd android && gradle wrapper --gradle-version 8.7 --distribution-type bin`
     This regenerates `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar`. After running, re-check that `gradle-wrapper.properties` still has the exact content from step 1 (the task may overwrite it with a slightly different format — verify and restore if needed).
  2. If no local Gradle is available: download the JAR from the Gradle 8.7 release directly.
     The canonical source is the Gradle GitHub releases page. The JAR for 8.7 can be obtained from:
     `https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar`
     Copy the downloaded file to `android/gradle/wrapper/gradle-wrapper.jar`.
  3. Commit the JAR: `git add android/gradle/wrapper/gradle-wrapper.jar`

  > **Note:** The wrapper JAR is a binary file (~59 KB). It is intentional to commit it — this is the standard Android practice and allows CI to bootstrap Gradle without a pre-installed Gradle.

  **Verify:** `android/gradle/wrapper/gradle-wrapper.jar` exists and is non-empty (>50 KB).

---

- [ ] 5. Create `android/gradle/libs.versions.toml`

  The version catalogue consumed by all `build.gradle.kts` files. Every dependency version in the project comes from here. No version is declared anywhere else.

  **File:** `android/gradle/libs.versions.toml`

  **Exact content:**
  ```toml
  [versions]
  agp                = "8.4.2"
  kotlin             = "1.9.24"
  compose-bom        = "2024.06.00"
  compose-compiler   = "1.5.14"
  core-ktx           = "1.13.1"
  lifecycle          = "2.8.2"
  activity-compose   = "1.9.0"
  material3          = "1.2.1"
  hilt               = "2.51.1"
  room               = "2.6.1"
  navigation         = "2.7.7"
  coroutines         = "1.8.1"
  camerax            = "1.3.3"
  work               = "2.9.0"
  junit              = "4.13.2"
  junit-ext          = "1.1.5"
  espresso           = "3.5.1"

  [libraries]
  # AndroidX Core
  androidx-core-ktx               = { group = "androidx.core",            name = "core-ktx",                         version.ref = "core-ktx" }

  # Lifecycle
  androidx-lifecycle-runtime-ktx  = { group = "androidx.lifecycle",       name = "lifecycle-runtime-ktx",            version.ref = "lifecycle" }
  androidx-lifecycle-viewmodel    = { group = "androidx.lifecycle",        name = "lifecycle-viewmodel-ktx",          version.ref = "lifecycle" }

  # Activity
  androidx-activity-compose       = { group = "androidx.activity",        name = "activity-compose",                 version.ref = "activity-compose" }

  # Compose BOM + components
  androidx-compose-bom            = { group = "androidx.compose",         name = "compose-bom",                      version.ref = "compose-bom" }
  androidx-compose-ui             = { group = "androidx.compose.ui",      name = "ui" }
  androidx-compose-ui-graphics    = { group = "androidx.compose.ui",      name = "ui-graphics" }
  androidx-compose-ui-tooling     = { group = "androidx.compose.ui",      name = "ui-tooling" }
  androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
  androidx-compose-ui-test-manifest   = { group = "androidx.compose.ui", name = "ui-test-manifest" }
  androidx-compose-ui-test-junit4     = { group = "androidx.compose.ui", name = "ui-test-junit4" }
  androidx-compose-material3      = { group = "androidx.compose.material3", name = "material3",                      version.ref = "material3" }

  # Navigation
  androidx-navigation-compose     = { group = "androidx.navigation",      name = "navigation-compose",               version.ref = "navigation" }

  # Hilt
  hilt-android                    = { group = "com.google.dagger",        name = "hilt-android",                     version.ref = "hilt" }
  hilt-android-compiler           = { group = "com.google.dagger",        name = "hilt-android-compiler",            version.ref = "hilt" }
  androidx-hilt-navigation-compose = { group = "androidx.hilt",           name = "hilt-navigation-compose",          version = "1.2.0" }

  # Room
  androidx-room-runtime           = { group = "androidx.room",            name = "room-runtime",                     version.ref = "room" }
  androidx-room-ktx               = { group = "androidx.room",            name = "room-ktx",                         version.ref = "room" }
  androidx-room-compiler          = { group = "androidx.room",            name = "room-compiler",                    version.ref = "room" }

  # Coroutines
  kotlinx-coroutines-android      = { group = "org.jetbrains.kotlinx",   name = "kotlinx-coroutines-android",        version.ref = "coroutines" }

  # CameraX
  androidx-camera-core            = { group = "androidx.camera",          name = "camera-core",                      version.ref = "camerax" }
  androidx-camera-camera2         = { group = "androidx.camera",          name = "camera-camera2",                   version.ref = "camerax" }
  androidx-camera-lifecycle       = { group = "androidx.camera",          name = "camera-lifecycle",                 version.ref = "camerax" }
  androidx-camera-view            = { group = "androidx.camera",          name = "camera-view",                      version.ref = "camerax" }

  # WorkManager
  androidx-work-runtime-ktx       = { group = "androidx.work",            name = "work-runtime-ktx",                 version.ref = "work" }

  # Testing — unit
  junit                           = { group = "junit",                    name = "junit",                            version.ref = "junit" }

  # Testing — instrumented
  androidx-junit                  = { group = "androidx.test.ext",        name = "junit",                            version.ref = "junit-ext" }
  androidx-espresso-core          = { group = "androidx.test.espresso",   name = "espresso-core",                    version.ref = "espresso" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "agp" }
  kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
  hilt                = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
  ksp                 = { id = "com.google.devtools.ksp", version = "1.9.24-1.0.20" }
  ```

  > **Note on `androidx-hilt-navigation-compose`:** Pinned at `1.2.0` directly because it does not share the `hilt` version — it tracks the AndroidX release train. This is still a pinned exact version, compliant with the requirement.
  > **Note on `ksp`:** Pinned at `1.9.24-1.0.20`, which is the KSP release that targets Kotlin 1.9.24. Room and Hilt use KSP. The version format is `<kotlin-version>-<ksp-patch>`.

  **Verify:** File parses without error — Gradle will reject malformed TOML on sync.

---

- [ ] 6. Create `android/settings.gradle.kts`

  Configures the project name, plugin repositories, and includes the `:app` module.

  **File:** `android/settings.gradle.kts`

  **Exact content:**
  ```kotlin
  pluginManagement {
      repositories {
          google {
              content {
                  includeGroupByRegex("com\\.android.*")
                  includeGroupByRegex("com\\.google.*")
                  includeGroupByRegex("androidx.*")
              }
          }
          mavenCentral()
          gradlePluginPortal()
      }
  }

  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          google()
          mavenCentral()
      }
  }

  rootProject.name = "MediHistoryAI"
  include(":app")
  ```

  **Verify:** `.\gradlew.bat help` from `android/` succeeds (settings file is parsed first; a syntax error here will prevent any Gradle command from running).

---

- [ ] 7. Create `android/build.gradle.kts` (root build file)

  Root-level build file. Applies no plugins directly; declares them for all subprojects to avoid version conflicts.

  **File:** `android/build.gradle.kts`

  **Exact content:**
  ```kotlin
  // Top-level build file where you can add configuration options common to all sub-projects/modules.
  plugins {
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.kotlin.android) apply false
      alias(libs.plugins.hilt) apply false
      alias(libs.plugins.ksp) apply false
  }
  ```

  **Verify:** `.\gradlew.bat help` from `android/` succeeds with no errors about plugin resolution.

---

- [ ] 8. Create `android/app/build.gradle.kts` (app module build file)

  The main build configuration for the `:app` module. Reads `BACKEND_BASE_URL` from `local.properties` or environment, then injects it as a `BuildConfig` field. No secrets are hardcoded.

  **File:** `android/app/build.gradle.kts`

  **Exact content:**
  ```kotlin
  import java.util.Properties

  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.hilt)
      alias(libs.plugins.ksp)
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

  android {
      namespace = "com.medihistory.ai"
      compileSdk = 34

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

      // Navigation
      implementation(libs.androidx.navigation.compose)

      // Hilt
      implementation(libs.hilt.android)
      ksp(libs.hilt.android.compiler)
      implementation(libs.androidx.hilt.navigation.compose)

      // Room
      implementation(libs.androidx.room.runtime)
      implementation(libs.androidx.room.ktx)
      ksp(libs.androidx.room.compiler)

      // Coroutines
      implementation(libs.kotlinx.coroutines.android)

      // CameraX
      implementation(libs.androidx.camera.core)
      implementation(libs.androidx.camera.camera2)
      implementation(libs.androidx.camera.lifecycle)
      implementation(libs.androidx.camera.view)

      // WorkManager
      implementation(libs.androidx.work.runtime.ktx)

      // Unit tests
      testImplementation(libs.junit)

      // Instrumented tests
      androidTestImplementation(libs.androidx.junit)
      androidTestImplementation(libs.androidx.espresso.core)
      androidTestImplementation(platform(libs.androidx.compose.bom))
      androidTestImplementation(libs.androidx.compose.ui.test.junit4)

      // Debug — Compose tooling
      debugImplementation(libs.androidx.compose.ui.tooling)
      debugImplementation(libs.androidx.compose.ui.test.manifest)
  }
  ```

  **Verify:** `.\gradlew.bat assembleDebug` from `android/` succeeds (download of dependencies will occur on first run).

---

- [ ] 9. Create `android/app/proguard-rules.pro`

  Minimal placeholder. Consumer rules for Compose and Hilt are included by the libraries' own AARs; no custom rules are needed at scaffold stage.

  **File:** `android/app/proguard-rules.pro`

  **Exact content:**
  ```
  # MediHistory AI — ProGuard/R8 rules
  #
  # Consumer rules for Compose and Hilt are included by the library AARs.
  # Add project-specific keep rules here as features are implemented.
  #
  # See https://www.guardsquare.com/proguard for reference.

  # Keep data classes used with Room (added per-entity as needed).
  ```

  **Verify:** File exists and is valid ProGuard syntax (empty rules file is always valid).

---

- [ ] 10. Create `android/app/src/main/AndroidManifest.xml`

  Minimal manifest. Declares the application class (`MediHistoryApplication` — created in step 12), the `MainActivity`, and the `android:theme`.

  **File:** `android/app/src/main/AndroidManifest.xml`

  **Exact content:**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android">

      <application
          android:name=".MediHistoryApplication"
          android:allowBackup="false"
          android:dataExtractionRules="@xml/data_extraction_rules"
          android:fullBackupContent="@xml/backup_rules"
          android:icon="@mipmap/ic_launcher"
          android:label="@string/app_name"
          android:roundIcon="@mipmap/ic_launcher_round"
          android:supportsRtl="true"
          android:theme="@style/Theme.MediHistoryAI">

          <activity
              android:name=".MainActivity"
              android:exported="true"
              android:label="@string/app_name"
              android:theme="@style/Theme.MediHistoryAI">
              <intent-filter>
                  <action android:name="android.intent.action.MAIN" />
                  <category android:name="android.intent.category.LAUNCHER" />
              </intent-filter>
          </activity>

      </application>

  </manifest>
  ```

  > **Note:** `android:allowBackup="false"` is intentional — auto-backup of medical records is opt-in. Cloud backup will be wired through WorkManager in a later ticket (MHA-040), not through Android's auto-backup mechanism.

  **Verify:** Manifest is well-formed XML. The build will fail at manifest merge if it is malformed.

---

- [ ] 11. Create supporting resource files referenced by the manifest

  The manifest references three resource files. These must exist for the build to succeed.

  **11a. `android/app/src/main/res/values/strings.xml`**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="app_name">MediHistory AI</string>
  </resources>
  ```

  **11b. `android/app/src/main/res/xml/data_extraction_rules.xml`**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!--
      MediHistory AI — Data Extraction Rules
      Medical records are highly sensitive. No data is included in cloud backups
      via Android's auto-backup mechanism. Cloud backup is opt-in and implemented
      via WorkManager with explicit encryption (MHA-040).
  -->
  <data-extraction-rules>
      <cloud-backup>
          <exclude domain="root" />
          <exclude domain="file" />
          <exclude domain="database" />
          <exclude domain="sharedpref" />
          <exclude domain="external" />
      </cloud-backup>
      <device-transfer>
          <exclude domain="root" />
          <exclude domain="file" />
          <exclude domain="database" />
          <exclude domain="sharedpref" />
          <exclude domain="external" />
      </device-transfer>
  </data-extraction-rules>
  ```

  **11c. `android/app/src/main/res/xml/backup_rules.xml`**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!--
      MediHistory AI — Backup Rules (legacy, API < 31)
      All data excluded from auto-backup for privacy. Opt-in cloud backup
      is implemented via WorkManager (MHA-040).
  -->
  <full-backup-content>
      <exclude domain="root" path="." />
      <exclude domain="file" path="." />
      <exclude domain="database" path="." />
      <exclude domain="sharedpref" path="." />
      <exclude domain="external" path="." />
  </full-backup-content>
  ```

  **11d. `android/app/src/main/res/values/themes.xml`**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <!-- Base theme — the Compose theme (Theme.kt) manages all Compose styling.
           This style exists only to satisfy the manifest android:theme reference. -->
      <style name="Theme.MediHistoryAI" parent="android:Theme.Material.Light.NoActionBar" />
  </resources>
  ```

  > **Note:** Launcher icons (`@mipmap/ic_launcher`, `@mipmap/ic_launcher_round`) are referenced in the manifest. Android's build system generates placeholder mipmap icons automatically from the default `ic_launcher.xml` adaptive icon if no actual PNG is provided. For the scaffold this is sufficient — the build will generate default robot icons. If the build fails with a resource-not-found error for the launcher icon, add a minimal `android/app/src/main/res/mipmap-mdpi/ic_launcher.png` (any 48×48 PNG). This is unlikely with AGP 8.4 which includes vector drawable support.

  **Verify:** `.\gradlew.bat assembleDebug` from `android/` — all resource files must compile without AAPT2 errors.

---

- [ ] 12. Create `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt`

  Defines Material 3 colour constants and the custom `MediHistoryColorScheme` data class for the `warning`/`warningContainer` tokens that have no slot in Material 3's built-in `ColorScheme`.

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai.ui.theme

  import androidx.compose.runtime.staticCompositionLocalOf
  import androidx.compose.ui.graphics.Color

  // ---------------------------------------------------------------------------
  // MediHistory AI — Design token colours
  // Source: docs/design/design-system.md
  // ---------------------------------------------------------------------------

  // Primary palette
  val MhPrimary             = Color(0xFF006874)
  val MhOnPrimary           = Color(0xFFFFFFFF)
  val MhPrimaryContainer    = Color(0xFF97F0FF)
  val MhOnPrimaryContainer  = Color(0xFF001F24)

  // Secondary palette
  val MhSecondary           = Color(0xFF4A6267)
  val MhSecondaryContainer  = Color(0xFFCCE8ED)

  // Error palette
  val MhError               = Color(0xFFBA1A1A)
  val MhErrorContainer      = Color(0xFFFFDAD6)

  // Warning palette (custom extension — no Material 3 slot)
  val MhWarning             = Color(0xFF7D5700)
  val MhWarningContainer    = Color(0xFFFFDEA8)

  // Surface / neutral palette
  val MhSurface             = Color(0xFFF5FAFB)
  val MhOnSurface           = Color(0xFF171D1E)
  val MhSurfaceVariant      = Color(0xFFDBE4E6)
  val MhOutline             = Color(0xFF6F797A)

  // ---------------------------------------------------------------------------
  // Custom colour scheme extension for warning tokens.
  // Access via MaterialTheme.mediHistoryColors (wired in Theme.kt).
  // ---------------------------------------------------------------------------

  /**
   * Extended colour tokens that have no slot in Material 3's built-in ColorScheme.
   * All composables that display warning UI must use these tokens rather than
   * hardcoding colours.
   */
  data class MediHistoryColorScheme(
      val warning: Color,
      val onWarning: Color = Color(0xFFFFFFFF),
      val warningContainer: Color,
      val onWarningContainer: Color = Color(0xFF271900),
  )

  val LocalMediHistoryColorScheme = staticCompositionLocalOf {
      MediHistoryColorScheme(
          warning = MhWarning,
          warningContainer = MhWarningContainer,
      )
  }
  ```

  **Verify:** `.\gradlew.bat compileDebugKotlin` — no compilation errors.

---

- [ ] 13. Create `android/app/src/main/java/com/medihistory/ai/ui/theme/Type.kt`

  Defines the Material 3 `Typography` object using the system default font. Custom font files are not included at scaffold stage — they will be added with the design system ticket (MHA-010).

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Type.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai.ui.theme

  import androidx.compose.material3.Typography
  import androidx.compose.ui.text.TextStyle
  import androidx.compose.ui.text.font.FontFamily
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.sp

  /**
   * MediHistory AI typography scale.
   * Uses the system default font at scaffold stage.
   * Custom font (e.g. Roboto Flex) will be added in the design system ticket (MHA-010).
   */
  val MediHistoryTypography = Typography(
      bodyLarge = TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          lineHeight = 24.sp,
          letterSpacing = 0.5.sp,
      ),
      titleLarge = TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 22.sp,
          lineHeight = 28.sp,
          letterSpacing = 0.sp,
      ),
      labelSmall = TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp,
          lineHeight = 16.sp,
          letterSpacing = 0.5.sp,
      ),
  )
  ```

  **Verify:** `.\gradlew.bat compileDebugKotlin` — no compilation errors.

---

- [ ] 14. Create `android/app/src/main/java/com/medihistory/ai/ui/theme/Shape.kt`

  Defines the Material 3 `Shapes` object using the standard shape scale. Named for consistency; values match Material 3 defaults.

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Shape.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai.ui.theme

  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material3.Shapes
  import androidx.compose.ui.unit.dp

  /**
   * MediHistory AI shape scale.
   * Follows Material 3 defaults; named here so all composables reference
   * MaterialTheme.shapes rather than hardcoding corner radii.
   */
  val MediHistoryShapes = Shapes(
      extraSmall = RoundedCornerShape(4.dp),
      small      = RoundedCornerShape(8.dp),
      medium     = RoundedCornerShape(12.dp),
      large      = RoundedCornerShape(16.dp),
      extraLarge = RoundedCornerShape(28.dp),
  )
  ```

  **Verify:** `.\gradlew.bat compileDebugKotlin` — no compilation errors.

---

- [ ] 15. Create `android/app/src/main/java/com/medihistory/ai/ui/theme/Theme.kt`

  Wires together the colour scheme, typography, shapes, and the custom `MediHistoryColorScheme` extension into the top-level `MediHistoryTheme` composable. Provides both light and dark schemes. The `LocalMediHistoryColorScheme` `CompositionLocal` is provided here so all descendant composables can access `MaterialTheme.mediHistoryColors`.

  The `MaterialTheme` extension property `mediHistoryColors` is defined in this file as a convenience accessor.

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Theme.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai.ui.theme

  import android.os.Build
  import androidx.compose.foundation.isSystemInDarkTheme
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.darkColorScheme
  import androidx.compose.material3.dynamicDarkColorScheme
  import androidx.compose.material3.dynamicLightColorScheme
  import androidx.compose.material3.lightColorScheme
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.CompositionLocalProvider
  import androidx.compose.runtime.ReadOnlyComposable
  import androidx.compose.ui.platform.LocalContext

  // ---------------------------------------------------------------------------
  // Light colour scheme
  // ---------------------------------------------------------------------------
  private val LightColorScheme = lightColorScheme(
      primary             = MhPrimary,
      onPrimary           = MhOnPrimary,
      primaryContainer    = MhPrimaryContainer,
      onPrimaryContainer  = MhOnPrimaryContainer,
      secondary           = MhSecondary,
      secondaryContainer  = MhSecondaryContainer,
      error               = MhError,
      errorContainer      = MhErrorContainer,
      surface             = MhSurface,
      onSurface           = MhOnSurface,
      surfaceVariant      = MhSurfaceVariant,
      outline             = MhOutline,
  )

  // ---------------------------------------------------------------------------
  // Dark colour scheme — mirrors light scheme; full dark palette to be added
  // in the design system ticket (MHA-010).
  // ---------------------------------------------------------------------------
  private val DarkColorScheme = darkColorScheme(
      primary             = MhPrimaryContainer,
      onPrimary           = MhOnPrimaryContainer,
      primaryContainer    = MhPrimary,
      onPrimaryContainer  = MhPrimaryContainer,
      secondary           = MhSecondaryContainer,
      secondaryContainer  = MhSecondary,
      error               = MhErrorContainer,
      errorContainer      = MhError,
  )

  // ---------------------------------------------------------------------------
  // Warning token extension — custom colours not in Material 3 ColorScheme.
  // ---------------------------------------------------------------------------
  private val LightWarningScheme = MediHistoryColorScheme(
      warning          = MhWarning,
      warningContainer = MhWarningContainer,
  )

  private val DarkWarningScheme = MediHistoryColorScheme(
      warning          = MhWarningContainer,
      warningContainer = MhWarning,
  )

  /**
   * Convenience accessor for the custom MediHistory colour extension.
   * Usage: MaterialTheme.mediHistoryColors.warning
   */
  val MaterialTheme.mediHistoryColors: MediHistoryColorScheme
      @Composable
      @ReadOnlyComposable
      get() = LocalMediHistoryColorScheme.current

  // ---------------------------------------------------------------------------
  // Top-level theme composable
  // ---------------------------------------------------------------------------

  /**
   * MediHistory AI application theme.
   *
   * Wraps all screens. Provides Material 3 theming plus the custom warning
   * colour extension via [LocalMediHistoryColorScheme].
   *
   * @param darkTheme     Follow system dark-mode setting by default.
   * @param dynamicColor  Use Android 12+ dynamic colour if available (default on).
   *                      Disable in preview/test.
   * @param content       The composable content tree.
   */
  @Composable
  fun MediHistoryTheme(
      darkTheme: Boolean = isSystemInDarkTheme(),
      dynamicColor: Boolean = true,
      content: @Composable () -> Unit,
  ) {
      val colorScheme = when {
          dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
              val context = LocalContext.current
              if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
          }
          darkTheme -> DarkColorScheme
          else      -> LightColorScheme
      }

      val warningScheme = if (darkTheme) DarkWarningScheme else LightWarningScheme

      CompositionLocalProvider(LocalMediHistoryColorScheme provides warningScheme) {
          MaterialTheme(
              colorScheme = colorScheme,
              typography  = MediHistoryTypography,
              shapes      = MediHistoryShapes,
              content     = content,
          )
      }
  }
  ```

  **Verify:** `.\gradlew.bat compileDebugKotlin` — no compilation errors.

---

- [ ] 16. Create `android/app/src/main/java/com/medihistory/ai/MainActivity.kt`

  The single activity. Applies the `@AndroidEntryPoint` Hilt annotation. Sets the Compose content to a placeholder `MediHistoryApp` composable wrapped in `MediHistoryTheme`. The nav graph and real screens are wired in subsequent tickets.

  **File:** `android/app/src/main/java/com/medihistory/ai/MainActivity.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai

  import android.os.Bundle
  import androidx.activity.ComponentActivity
  import androidx.activity.compose.setContent
  import androidx.activity.enableEdgeToEdge
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.padding
  import androidx.compose.material3.Scaffold
  import androidx.compose.material3.Text
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import com.medihistory.ai.ui.theme.MediHistoryTheme
  import dagger.hilt.android.AndroidEntryPoint

  /**
   * The single activity for MediHistory AI.
   *
   * Navigation graph and screen wiring are added in MHA-010 (design system)
   * and feature tickets. This activity is intentionally minimal at scaffold stage.
   */
  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          enableEdgeToEdge()
          setContent {
              MediHistoryTheme {
                  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                      Box(
                          contentAlignment = Alignment.Center,
                          modifier = Modifier
                              .fillMaxSize()
                              .padding(innerPadding),
                      ) {
                          Text(text = "MediHistory AI")
                      }
                  }
              }
          }
      }
  }
  ```

  **Verify:** `.\gradlew.bat compileDebugKotlin` — no compilation errors.

---

- [ ] 17. Create `android/app/src/main/java/com/medihistory/ai/MediHistoryApplication.kt`

  Hilt `@HiltAndroidApp` application class. Referenced by `AndroidManifest.xml` as `android:name=".MediHistoryApplication"`. Required for Hilt dependency injection to work throughout the app.

  **File:** `android/app/src/main/java/com/medihistory/ai/MediHistoryApplication.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai

  import android.app.Application
  import dagger.hilt.android.HiltAndroidApp

  /**
   * Application class for MediHistory AI.
   *
   * The [HiltAndroidApp] annotation triggers Hilt's code generation for
   * the application-level dependency injection component.
   */
  @HiltAndroidApp
  class MediHistoryApplication : Application()
  ```

  **Verify:** `.\gradlew.bat assembleDebug` — build must succeed. A missing `@HiltAndroidApp` class causes a Hilt annotation processing error at compile time.

---

- [ ] 18. Create `android/app/src/test/java/com/medihistory/ai/ExampleUnitTest.kt`

  Minimal JVM unit test that runs without a device. Uses synthetic assertions only. No real patient data.

  **File:** `android/app/src/test/java/com/medihistory/ai/ExampleUnitTest.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai

  import org.junit.Assert.assertEquals
  import org.junit.Test

  /**
   * Scaffold unit test — verifies the JVM test configuration is working.
   * Real tests for business logic are added per-feature ticket.
   *
   * Uses synthetic values only; no real patient or medicine data.
   */
  class ExampleUnitTest {

      @Test
      fun `scaffold test configuration is working`() {
          // Verify that the unit test runtime is correctly set up.
          val syntheticPatientId = "synthetic-patient-001"
          val expected = "synthetic-patient-001"
          assertEquals(expected, syntheticPatientId)
      }
  }
  ```

  **Verify:** `.\gradlew.bat testDebugUnitTest` — test must pass with 0 failures.

---

- [ ] 19. Create `android/app/src/androidTest/java/com/medihistory/ai/ExampleInstrumentedTest.kt`

  Minimal instrumented test skeleton. Verifies the package name matches `applicationId`. Not run by the unit-test task; run on a device/emulator via `connectedDebugAndroidTest`.

  **File:** `android/app/src/androidTest/java/com/medihistory/ai/ExampleInstrumentedTest.kt`

  **Exact content:**
  ```kotlin
  package com.medihistory.ai

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import org.junit.Assert.assertEquals
  import org.junit.Test
  import org.junit.runner.RunWith

  /**
   * Scaffold instrumented test — verifies the test runner is correctly configured.
   * Real UI/integration tests are added per-feature ticket.
   *
   * Requires a connected device or emulator.
   * Run with: ./gradlew connectedDebugAndroidTest
   */
  @RunWith(AndroidJUnit4::class)
  class ExampleInstrumentedTest {

      @Test
      fun `application package name is correct`() {
          val appContext = InstrumentationRegistry.getInstrumentation().targetContext
          assertEquals("com.medihistory.ai", appContext.packageName)
      }
  }
  ```

  **Verify:** Compiles as part of `.\gradlew.bat assembleDebugAndroidTest` (does not require a device to build the test APK).

---

- [ ] 20. Remove `android/.gitkeep`

  The stub `.gitkeep` file that held the `android/` directory before files were added must be deleted. Once real files exist, it serves no purpose and will show as an untracked deletion.

  **Command:** `Remove-Item "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\.gitkeep"`

  Then stage the deletion: `git rm android/.gitkeep`

  **Verify:** `android/.gitkeep` does not appear in `git status`.

---

- [ ] 21. Run verification sequence

  Execute all four verification steps in order from `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\`:

  **Step 21a — assembleDebug**
  ```powershell
  cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android
  .\gradlew.bat assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` with exit code 0. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

  If this fails on first run due to Gradle wrapper JAR download or AGP download, wait for the download to complete and re-run. Network errors on first run are not a code defect.

  **Step 21b — lintDebug**
  ```powershell
  .\gradlew.bat lintDebug
  ```
  Expected: `BUILD SUCCESSFUL`. Lint report at `app/build/reports/lint-results-debug.html`.

  If lint reports issues, fix each one before proceeding. Common scaffold-stage lint warnings:
  - `UnusedResources` — suppress is acceptable at scaffold; real resources are added per-feature.
  - `HardcodedText` — the `"MediHistory AI"` string in `MainActivity.kt` should be replaced with `stringResource(R.string.app_name)` if lint flags it.

  **Step 21c — testDebugUnitTest**
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```
  Expected: `BUILD SUCCESSFUL`. `ExampleUnitTest` passes. Test report at `app/build/reports/tests/testDebugUnitTest/index.html`.

  **Step 21d — Verify `local.properties` is absent**
  ```powershell
  Test-Path "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\local.properties"
  ```
  Expected: `False`. If `True`, confirm the file is listed in `.gitignore` (it is) and do NOT commit it.

  **Step 21e — Security grep (no hardcoded secrets)**
  ```powershell
  Select-String -Path "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\main\*" `
    -Pattern "api_key|secret|password" -Recurse
  ```
  Expected: zero matches (no output). The `backendBaseUrl` variable in `app/build.gradle.kts` is not in `src/main/` so is not scanned here; it is injected into `BuildConfig` at build time from an external source.

---

## File Creation Checklist (in order)

| # | File | Notes |
|---|------|-------|
| 1 | `android/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.7 bin |
| 2 | `android/gradlew` | LF endings; mark +x in git |
| 3 | `android/gradlew.bat` | Windows batch script |
| 4 | `android/gradle/wrapper/gradle-wrapper.jar` | Binary; commit it |
| 5 | `android/gradle/libs.versions.toml` | All pinned versions |
| 6 | `android/settings.gradle.kts` | Plugin repos + include(:app) |
| 7 | `android/build.gradle.kts` | Root; plugins apply false |
| 8 | `android/app/build.gradle.kts` | Full app config; BACKEND_BASE_URL logic |
| 9 | `android/app/proguard-rules.pro` | Placeholder |
| 10 | `android/app/src/main/AndroidManifest.xml` | allowBackup=false |
| 11a | `android/app/src/main/res/values/strings.xml` | app_name |
| 11b | `android/app/src/main/res/xml/data_extraction_rules.xml` | Exclude all domains |
| 11c | `android/app/src/main/res/xml/backup_rules.xml` | Exclude all domains |
| 11d | `android/app/src/main/res/values/themes.xml` | NoActionBar theme |
| 12 | `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt` | All design tokens |
| 13 | `android/app/src/main/java/com/medihistory/ai/ui/theme/Type.kt` | Typography |
| 14 | `android/app/src/main/java/com/medihistory/ai/ui/theme/Shape.kt` | Shape scale |
| 15 | `android/app/src/main/java/com/medihistory/ai/ui/theme/Theme.kt` | MediHistoryTheme |
| 16 | `android/app/src/main/java/com/medihistory/ai/MainActivity.kt` | @AndroidEntryPoint |
| 17 | `android/app/src/main/java/com/medihistory/ai/MediHistoryApplication.kt` | @HiltAndroidApp |
| 18 | `android/app/src/test/java/com/medihistory/ai/ExampleUnitTest.kt` | Synthetic; JVM only |
| 19 | `android/app/src/androidTest/java/com/medihistory/ai/ExampleInstrumentedTest.kt` | Instrumented skeleton |
| 20 | Delete `android/.gitkeep` | `git rm android/.gitkeep` |
| 21 | Run all verifications | Steps 21a–21e |

---

## Common Failure Modes and How to Handle Them

**`Unresolved reference: libs`** in build files
→ The `libs.versions.toml` is not in the `gradle/` subdirectory, or the file has a TOML parse error. Verify the file is at `android/gradle/libs.versions.toml` (not `android/libs.versions.toml`).

**`Could not find com.google.devtools.ksp:...`**
→ The KSP plugin `1.9.24-1.0.20` is the correct version for Kotlin 1.9.24. If the Maven repository cannot resolve it, verify `google()` and `mavenCentral()` are in `pluginManagement.repositories` in `settings.gradle.kts`.

**`Hilt annotation processing error: @HiltAndroidApp not found`**
→ `MediHistoryApplication` is not declared in `AndroidManifest.xml` as `android:name`, or the class is in the wrong package. The manifest uses `.MediHistoryApplication` (relative to `namespace`).

**Lint error: `HardcodedText`**
→ Replace the `Text(text = "MediHistory AI")` literal in `MainActivity.kt` with `Text(text = stringResource(R.string.app_name))` and add `import androidx.compose.ui.res.stringResource`.

**Lint error: `MissingPermission` or similar at scaffold**
→ If lint flags missing permissions for CameraX or WorkManager (added as dependencies but not yet used), add `tools:ignore="UnusedResources"` annotations or suppress via `lint.xml`. Do not remove the dependencies — they are required for future tickets.

**`gradle-wrapper.jar` missing**
→ The JAR must be committed. If running `gradle wrapper` is not an option, download from the Gradle GitHub mirror (see step 4). The build cannot proceed without it.

**`local.properties` created by Android Studio**
→ This is expected behaviour. The file is git-ignored; just confirm it is not staged (`git status` should not show it). Never add it to the index.
