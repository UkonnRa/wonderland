
plugins {
  jacoco
  idea

  id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
  id("io.gitlab.arturbosch.detekt") version "1.16.0-RC2"

  id("com.github.ben-manes.versions") version "0.36.0"
  id("org.springframework.boot") version "2.5.0-M2"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  kotlin("jvm") version "1.4.31"
  kotlin("kapt") version "1.4.31"
  kotlin("plugin.spring") version "1.4.31"
}

object Versions {
  const val JACKSON = "2.12.1"

  const val FONT_AWESOME = "5.15.2"

  const val JAVA = "11"
}

allprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  apply(plugin = "io.gitlab.arturbosch.detekt")

  group = "com.ukonnra"
  version = "0.0.1"

  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
  }

  ktlint {
    version.set("0.40.0")
  }

  detekt {
    failFast = true
    config = files("$rootDir/detekt.yml")
    autoCorrect = true
    buildUponDefaultConfig = true
    reports {
      xml.enabled = true
      html.enabled = true
      txt.enabled = false
    }
  }

  tasks.detekt {
    jvmTarget = Versions.JAVA
  }
}

subprojects {
  apply(plugin = "idea")
  apply(plugin = "jacoco")

  apply(plugin = "com.github.ben-manes.versions")
  apply(plugin = "org.springframework.boot")
  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.kapt")
  apply(plugin = "org.jetbrains.kotlin.plugin.spring")

  configurations {
    developmentOnly
    runtimeClasspath {
      extendsFrom(configurations.developmentOnly.get())
    }
  }

  dependencies {
    implementation("org.springframework.experimental:spring-graalvm-native:0.8.5")

    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework:spring-context-indexer")
    implementation("org.springframework.boot:spring-boot-starter-security")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    implementation("org.webjars:font-awesome:${Versions.FONT_AWESOME}")

    kapt("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
      exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
  }

  tasks.compileKotlin {
    kotlinOptions {
      jvmTarget = Versions.JAVA
      freeCompilerArgs = listOf("-Xjsr305=strict")
    }
  }

  tasks.compileTestKotlin {
    kotlinOptions {
      jvmTarget = Versions.JAVA
      freeCompilerArgs = listOf("-Xjsr305=strict")
    }
  }

  tasks.test {
    useJUnitPlatform()
  }

  tasks.bootBuildImage {
    val imageTag = project.properties["imageTag"]!!
    imageName = "ukonnra/${project.name}:$imageTag"
    builder = "paketobuildpacks/builder:tiny"
    environment = mapOf(
      "BP_BOOT_NATIVE_IMAGE" to "1",
      "BP_BOOT_NATIVE_IMAGE_BUILD_ARGUMENTS" to """
      -H:+AddAllCharsets
      -H:+ReportExceptionStackTraces
      -H:+PrintAnalysisCallTree
      --enable-all-security-services
      --enable-https
      --enable-http
      -Dspring.spel.ignore=true
      -Dspring.native.remove-yaml-support=true
      """.trimIndent()
    )

    if (project.hasProperty("production")) {
      isPublish = true
      docker {
        publishRegistry {
          username = "ukonnra"
          email = "ukonnra@outlook.com"
          password = project.properties["dockerToken"]!!.toString()
        }
      }
    }
  }
}
