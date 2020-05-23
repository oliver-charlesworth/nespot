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
      }
    }

    jvm().compilations["test"].defaultSourceSet {
      dependencies {
        implementation(project(":common-test"))
      }
    }

    js().compilations["main"].defaultSourceSet  {
      dependencies {
        implementation(kotlin("stdlib-js"))
      }
    }
  }
}

