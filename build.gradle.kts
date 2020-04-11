import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("nebula.kotlin") version "1.3.61"
  id("org.openjfx.javafxplugin") version "0.0.8"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.github.microutils:kotlin-logging:1.7.7")

  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
  testImplementation("org.hamcrest:hamcrest-library:2.2")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

  runtimeOnly("org.slf4j:slf4j-simple:1.7.29")
}

javafx {
  version = "14"
  modules = listOf("javafx.graphics")
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
