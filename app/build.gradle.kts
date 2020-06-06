plugins {
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":"))
}

application {
  mainClassName = "choliver.nespot.ui.CliKt"
}

(tasks.run) {
  workingDir = rootDir
}
