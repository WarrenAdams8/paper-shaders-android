plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  `maven-publish`
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

android {
  namespace = "com.paperdesign.shaders"
  compileSdk = 37
  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = false
    aidl = false
    shaders = false
  }
  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  api(composeBom)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)

  androidTestImplementation(composeBom)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  // Compose ui-test pulls espresso 3.5, which cannot inject input on API 36+.
  androidTestImplementation(libs.androidx.test.espresso.core)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = project.group.toString()
      artifactId = "paper-shaders"
      version = project.version.toString()
      afterEvaluate { from(components["release"]) }
      pom {
        name.set("Paper Shaders for Android")
        description.set("AGSL ports of the Paper Design shaders for Jetpack Compose and Android Views.")
        url.set("https://github.com/WarrenAdams8/paper-shaders-android")
        licenses {
          license {
            name.set("Apache-2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        }
      }
    }
  }
}
