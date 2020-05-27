import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  kotlin("jvm") version "1.3.72" apply false
  kotlin("multiplatform") version "1.3.72"
}

allprojects {
  group = "choliver.nespot"
  version = "0.0.0"

  repositories {
    mavenCentral()
  }

  // We don't need checkParameterIsNotNull (etc.) as we don't interact with Java code
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = listOf("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
    }
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events(FAILED)
    }
  }
}

kotlin {
  jvm()
  js().browser()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
      }
    }

    jvm().compilations["main"].defaultSourceSet {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.openjfx:javafx-base:14:mac")
        implementation("org.openjfx:javafx-graphics:14:mac")
        implementation("com.github.ajalt:clikt:2.6.0")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
        implementation("net.java.jinput:jinput:2.0.9")
        runtimeOnly("net.java.jinput:jinput:2.0.9:natives-all")
      }
    }

    jvm().compilations["test"].defaultSourceSet {
      dependencies {
        implementation("org.junit.jupiter:junit-jupiter:5.5.2")
        implementation("org.hamcrest:hamcrest-library:2.2")
        implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
        // byte-buddy 1.9.10 (pulled in by Mockito) behaves badly with Java 13 - see https://github.com/mockk/mockk/issues/397
        implementation("net.bytebuddy:byte-buddy:1.10.6")
      }
    }

    js().compilations["main"].defaultSourceSet {
      dependencies {
        implementation(kotlin("stdlib-js"))
      }
      resources.srcDir(File(rootDir, "roms"))
    }
  }
}
