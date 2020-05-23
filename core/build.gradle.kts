plugins {
  kotlin("jvm")
  id("org.openjfx.javafxplugin") version "0.0.8"
  application
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


application {
  mainClassName = "choliver.nespot.runner.CliKt"
}

(tasks.run) {
  workingDir = rootDir
}
