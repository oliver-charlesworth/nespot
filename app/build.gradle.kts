plugins {
  kotlin("jvm")
  application
}

dependencies {
  implementation(project(":"))
}

application {
  mainClassName = "choliver.nespot.runner.CliKt"
}

(tasks.run) {
  workingDir = rootDir
}
