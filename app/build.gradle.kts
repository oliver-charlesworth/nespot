plugins {
  kotlin("jvm")
  id("org.openjfx.javafxplugin") version "0.0.8"
  application
}

dependencies {
  implementation(project(":core"))
  implementation(kotlin("stdlib"))
  implementation("com.github.ajalt:clikt:2.6.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
  implementation("net.java.jinput:jinput:2.0.9")
  runtimeOnly("net.java.jinput:jinput:2.0.9:natives-all")

  testImplementation(project(":common-test"))
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
