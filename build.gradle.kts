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
    file.inputStream().use(::load)
  }
}

val bintrayApiKey: String? = properties.getProperty("bintray.apiKey")
val bintrayUser: String? = properties.getProperty("bintray.user")
val bintrayRepo: String? = properties.getProperty("bintray.repo")
val bintrayIssueTracker: String? = properties.getProperty("bintray.issueTracker")
val bintrayGithubRepo: String? = properties.getProperty("bintray.githubRepo")
val bintrayWebsite: String? = properties.getProperty("bintray.website")
val bintrayVCS: String? = properties.getProperty("bintray.vcs")
val all = arrayOf(bintrayUser, bintrayRepo, bintrayIssueTracker, bintrayGithubRepo, bintrayWebsite, bintrayVCS)

if (all.all { it != null }) {
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
      publicDownloadNumbers = true
      issueTrackerUrl = bintrayIssueTracker
      githubRepo = bintrayGithubRepo
      websiteUrl = bintrayWebsite
      vcsUrl = bintrayVCS

      setLicenses("MIT")

      configure(version, closureOf<VersionConfig> {
        name = "${project.version}"
        vcsTag = "v$name"
      })
    })
  }
}

