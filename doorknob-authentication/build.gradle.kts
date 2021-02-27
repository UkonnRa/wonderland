plugins {
  id("com.github.node-gradle.node") version "3.0.1"
}

dependencies {
  implementation("org.springframework.security.experimental:spring-security-oauth2-authorization-server:0.1.0")
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
}

val tailwindCss = tasks.register<com.github.gradle.node.npm.task.NpxTask>("tailwindcss") {
  dependsOn(tasks.npmInstall)

  if (project.hasProperty("production")) {
    environment.put("NODE_ENV", "production")
  }

  val generatedFile = "build/resources/main/static/css/tailwind-generated.css"

  // Location of the tailwind config file
  val tailwindConfig = "css/tailwind.css"

  command.set("tailwindcss-cli@latest")
  args.set(listOf("build", tailwindConfig, "-o", generatedFile))

  inputs.file(tailwindConfig)
  outputs.file(generatedFile)
}

tasks.processResources {
  dependsOn(tailwindCss)
}
