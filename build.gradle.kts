import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
  base
  kotlin("jvm") version "1.3.72" apply false
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
