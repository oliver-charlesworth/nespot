plugins {
  kotlin("jvm")
}

dependencies {
  api("org.junit.jupiter:junit-jupiter:5.5.2")
  api("org.hamcrest:hamcrest-library:2.2")
  api("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
  // byte-buddy 1.9.10 (pulled in by Mockito) behaves badly with Java 13 - see https://github.com/mockk/mockk/issues/397
  api("net.bytebuddy:byte-buddy:1.10.6")
}
