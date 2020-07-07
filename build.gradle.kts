plugins {
  kotlin("multiplatform") version "1.3.72"
  kotlin("plugin.serialization") version "1.3.72"
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