plugins {
}

object Versions {
  const val HYDRA = "1.9.0"
  const val JACKSON = "2.12.1"
}

dependencies {
  implementation(project(":infrastructure"))
  implementation(project(":doorknob-proto"))

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.session:spring-session-core")

  implementation("sh.ory.hydra:hydra-client:${Versions.HYDRA}")
  implementation("com.github.joschi.jackson:jackson-datatype-threetenbp:${Versions.JACKSON}")
}
