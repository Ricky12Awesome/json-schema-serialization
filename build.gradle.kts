import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
  kotlin("multiplatform") version "1.5.10"
  kotlin("plugin.serialization") version "1.5.10"
  id("org.jetbrains.dokka") version "1.4.32"
  `maven-publish`
}

repositories {
  mavenCentral()
  jcenter()
}

kotlin {
  jvm {
    compilations.all {
      kotlinOptions.jvmTarget = "1.8"
    }

    testRuns.all {
      executionTask {
        useJUnitPlatform()
      }
    }
  }

  js(BOTH) {
    nodejs {
      testTask {
        with(compilation) {
          kotlinOptions {
            moduleKind = "commonjs"
          }
        }
      }
    }
  }

  mingwX64()
  linuxX64()
  macosX64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("reflect"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmMain by getting
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit5"))
        runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.0")
      }
    }

    val jsMain by getting
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }

    val nativeMain by creating {
      dependsOn(commonMain)
    }

    val nativeTest by creating {
      dependsOn(commonTest)
    }

    val mingwX64Main by getting {
      dependsOn(nativeMain)
    }

    val mingwX64Test by getting {
      dependsOn(nativeTest)
    }

    val linuxX64Main by getting {
      dependsOn(nativeMain)
    }

    val linuxX64Test by getting {
      dependsOn(nativeTest)
    }

    val macosX64Main by getting {
      dependsOn(nativeMain)
    }

    val macosX64Test by getting {
      dependsOn(nativeTest)
    }

    all {
      languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
      languageSettings.useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
      languageSettings.enableLanguageFeature("InlineClasses")
    }
  }
}

publishing {
  val key = System.getenv("BINTRAY_API_KEY")
  val user = "ricky12awesome"

  repositories {
    mavenLocal()

    if (key != null) {
      maven {
        name = "bintray"
        url = uri("https://api.bintray.com/maven/$user/github/json-schema-serialization/;publish=0;override=1")

        credentials {
          username = user
          password = key
        }
      }
    }
  }
}

