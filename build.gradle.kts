import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  base
  kotlin("jvm") version "2.0.10" apply false
  kotlin("multiplatform") version "2.0.10"
}

allprojects {
  group = "choliver.nespot"
  version = "0.0.0"

  repositories {
    mavenCentral()
  }

  // We don't need checkParameterIsNotNull (etc.) as we don't interact with Java code
  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
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
        implementation("org.openjfx:javafx-base:19.0.2.1:mac")
        implementation("org.openjfx:javafx-graphics:19.0.2.1:mac")
        implementation("com.github.ajalt:clikt:2.6.0")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
        implementation("net.java.jinput:jinput:2.0.10")
        runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")
      }
    }

    jvm().compilations["test"].defaultSourceSet {
      dependencies {
        implementation("org.junit.jupiter:junit-jupiter:5.5.2")
        implementation("org.hamcrest:hamcrest-library:2.2")
        implementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
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
