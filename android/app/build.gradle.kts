import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.grindhouse420.kxbz"
    minSdk = 24
    targetSdk = 36
    versionCode = 35
    versionName = "1.6.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Zwei Ausgaben aus einer Quelle. "light" ist zum Zuschauen am Fernseher gedacht, "full"
  // zusaetzlich zum Mitreden — der Chat-Code liegt nur im full-Zweig und faellt bei light
  // komplett weg. Unterschiedliche Anwendungs-IDs, damit beide Varianten nebeneinander auf
  // einem Geraet leben koennen: das Handy mit full wird so zur Tastatur fuer den Fernseher.
  flavorDimensions += "edition"
  productFlavors {
    create("light") {
      dimension = "edition"
      buildConfigField("Boolean", "HAS_CHAT_INPUT", "false")
      resValue("string", "app_name", "Mikes 420 Grindhouse")
    }
    create("full") {
      dimension = "edition"
      applicationIdSuffix = ".full"
      versionNameSuffix = "-full"
      buildConfigField("Boolean", "HAS_CHAT_INPUT", "true")
      resValue("string", "app_name", "Mikes 420 Grindhouse Full")
    }
  }

  // Liegt der Upload-Schluessel vor, wird damit signiert; sonst mit demselben Schluessel wie
  // bisher. Das ist der Punkt: die bislang verteilten Builds waren Debug-Builds — laufen ohne
  // JIT-Optimierung und tragen android:debuggable, was den App-Speicher auf jedem Geraet mit
  // adb auslesbar macht. Ein Release-Build behebt beides. Die Signatur bleibt dabei dieselbe,
  // sonst muesste jeder Tester die App vorher deinstallieren.
  val releaseKeystore = file("${rootDir}/grindhouse-release.jks")
  val uploadKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  val hasUploadKeystore = uploadKeystore.exists()

  signingConfigs {
    create("release") {
      if (releaseKeystore.exists()) {
        storeFile = releaseKeystore
        storePassword = System.getenv("STORE_PASSWORD") ?: "grindhouse420"
        keyAlias = "grindhouse"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "grindhouse420"
      } else if (hasUploadKeystore) {
        storeFile = uploadKeystore
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  // Die Unit-Tests laufen unter Java 21, der uebrige Build bleibt auf 17. Robolectric baut
  // seine Sandbox gegen das Android-SDK der Testklasse; ab SDK 36 verlangt das Java 21 und
  // brach sonst schon beim Klassen-Setup ab ("Failed to create a Robolectric sandbox").
  tasks.withType<Test>().configureEach {
    javaLauncher.set(
      javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
    )
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  // Der In-App-Sprachumschalter waehlt die Locale zur Laufzeit. Ohne das hier wuerde ein AAB
  // nur die Systemsprache ausliefern und die jeweils andere Sprache fehlte schlicht.
  bundle {
    language {
      enableSplit = false
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.socket.io.client)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
