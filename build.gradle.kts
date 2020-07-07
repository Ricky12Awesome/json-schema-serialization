import com.jfrog.bintray.gradle.BintrayExtension.PackageConfig
import com.jfrog.bintray.gradle.BintrayExtension.VersionConfig
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
  kotlin("multiplatform") version "1.3.72"
  kotlin("plugin.serialization") version "1.3.72"
  id("com.jfrog.bintray") version "1.8.5"
  `maven-publish`
}

repositories {
  mavenCentral()
}

kotlin {
  jvm {
    compilations.forEach {
      it.compileKotlinTask.kotlinOptions.jvmTarget = "1.8"
    }
  }

  js {
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.20.0")
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
      }
    }

    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit5"))
      }
    }

    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }

    forEach {
      it.kotlin.srcDir("./${it.name}/src/")
      it.resources.srcDir("./${it.name}/resources/")
    }
  }
}

val properties = Properties().apply {
  val file = file("local.properties")
  if (file.exists()) {
    file.inputStream().use {
      load(it)
    }
  }
}

val bintrayApiKey: String? = properties.getProperty("BINTRAY_API_KEY")
val bintrayUser: String? = properties.getProperty("BINTRAY_USER")
val bintrayRepo: String? = properties.getProperty("BINTRAY_REPO")

if (bintrayApiKey != null && bintrayUser != null || bintrayRepo != null) {
  bintray {
    key = bintrayApiKey
    user = bintrayUser
    publish = false

    setPublications(*publishing.publications
      .map { it.name }
      .filter { it != "kotlinMultiplatform" }
      .toTypedArray()
    )

    configure(pkg, closureOf<PackageConfig> {
      repo = bintrayRepo
      name = project.name
      vcsUrl = "https://github.com/Ricky12Awesome/json-schema-serialization.git"
      setLicenses("MIT")

      configure(version, closureOf<VersionConfig> {
        name = project.version.toString()
      })
    })
  }
}

