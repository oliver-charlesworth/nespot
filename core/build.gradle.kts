plugins {
  kotlin("jvm")
}

dependencies {
  implementation(kotlin("stdlib"))

  testImplementation(project(":common-test"))
}
