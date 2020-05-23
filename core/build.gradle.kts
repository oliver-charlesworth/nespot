plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  js().browser()

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
      }
    }

    jvm().compilations["main"].defaultSourceSet {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
      }
    }

    jvm().compilations["test"].defaultSourceSet {
      dependencies {
        implementation(project(":common-test"))
      }
    }

    js().compilations["main"].defaultSourceSet  {
      dependencies {
        implementation(kotlin("stdlib-js"))
      }
    }
  }
}
