import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("nebula.kotlin") version "1.3.61"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.github.microutils:kotlin-logging:1.7.7")

  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
  testImplementation("org.hamcrest:hamcrest-library:2.2")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.freeCompilerArgs += listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events(PASSED, SKIPPED, FAILED)
  }
}
