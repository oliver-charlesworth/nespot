plugins {
  kotlin("jvm")
}

dependencies {
  implementation("org.junit.jupiter:junit-jupiter:5.5.2")
  implementation("org.hamcrest:hamcrest-library:2.2")
  implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
  // byte-buddy 1.9.10 (pulled in by Mockito) behaves badly with Java 13 - see https://github.com/mockk/mockk/issues/397
  implementation("net.bytebuddy:byte-buddy:1.10.6")
}
