plugins {
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":"))
  implementation(kotlin("stdlib"))
  implementation("org.openjfx:javafx-base:14:mac")
  implementation("org.openjfx:javafx-graphics:14:mac")
  implementation("com.github.ajalt:clikt:2.6.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
  implementation("net.java.jinput:jinput:2.0.9")
  runtimeOnly("net.java.jinput:jinput:2.0.9:natives-all")

  testImplementation(project(":common-test"))
}

application {
  mainClassName = "choliver.nespot.runner.CliKt"
}

(tasks.run) {
  workingDir = rootDir
}
