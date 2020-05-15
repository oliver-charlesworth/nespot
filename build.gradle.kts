import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application
  id("nebula.kotlin") version "1.3.72"
  id("org.openjfx.javafxplugin") version "0.0.8"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.github.ajalt:clikt:2.6.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")

  implementation("net.java.jinput:jinput:2.0.9")
  runtimeOnly("net.java.jinput:jinput:2.0.9:natives-all")

  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
  testImplementation("org.hamcrest:hamcrest-library:2.2")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
  // byte-buddy 1.9.10 (pulled in by Mockito) behaves badly with Java 13 - see https://github.com/mockk/mockk/issues/397
  testImplementation("net.bytebuddy:byte-buddy:1.10.6")
}

javafx {
  version = "14"
  modules = listOf("javafx.graphics")
}

// We don't need checkParameterIsNotNull (etc.) as we don't interact with Java code
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.freeCompilerArgs += listOf("-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions")
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events(FAILED)
  }
}

application {
  mainClassName = "choliver.nespot.runner.CliKt"
}
