import org.gradle.api.tasks.testing.logging.TestLogEvent.*

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

tasks.test {
  useJUnitPlatform()
  testLogging {
    events(PASSED, SKIPPED, FAILED)
  }
}
