import com.jfrog.bintray.gradle.BintrayExtension.PackageConfig
import com.jfrog.bintray.gradle.BintrayExtension.VersionConfig
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
  kotlin("jvm") version "1.3.72"
  kotlin("plugin.serialization") version "1.3.72"
  id("com.jfrog.bintray") version "1.8.5"
  `maven-publish`
}

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")

  testImplementation(kotlin("test-junit5"))

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.0.0")
}

kotlin.sourceSets["main"].kotlin.srcDir("src/main/")
kotlin.sourceSets["test"].kotlin.srcDir("src/test/")

sourceSets["main"].resources.srcDir("resources/main/")
sourceSets["test"].resources.srcDir("resources/test/")

val sourcesJar by tasks.creating(Jar::class) {
  archiveClassifier.set("sources")

  from(sourceSets["main"].allSource)

  dependsOn(JavaPlugin.CLASSES_TASK_NAME)
}

val javadocJar by tasks.creating(Jar::class) {
  archiveClassifier.set("javadoc")

  from(tasks["javadoc"])

  dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
}

tasks {
  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
      jvmTarget = "1.8"
    }
  }

  compileTestKotlin {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }

  test {
    useJUnitPlatform()
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      artifact(sourcesJar)
      artifact(javadocJar)
    }
  }

  repositories {
    mavenLocal()
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

    setPublications("maven")

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

