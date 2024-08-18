plugins {
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":"))
}

application {
  mainClass = "choliver.nespot.ui.CliKt"
}

(tasks.run) {
  workingDir = rootDir
}
