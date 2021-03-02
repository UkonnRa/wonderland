import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  idea
  java
  jacoco

  id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
  id("io.gitlab.arturbosch.detekt") version "1.16.0-RC2"

  id("com.github.ben-manes.versions") version "0.36.0"
  kotlin("jvm") version "1.4.31"
  kotlin("kapt") version "1.4.31"

  id("org.springframework.boot") version "2.5.0-M2"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  kotlin("plugin.spring") version "1.4.31"

  id("com.google.protobuf") version "0.8.15"
}

object Versions {
  const val JAVA = "11"

  const val PROTOC = "3.15.3"

  const val JAVAX_ANNOTATION = "1.3.2"
}

allprojects {
  apply(plugin = "idea")
  apply(plugin = "java")
  apply(plugin = "jacoco")

  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  apply(plugin = "io.gitlab.arturbosch.detekt")

  apply(plugin = "com.github.ben-manes.versions")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.kotlin.kapt")

  group = "com.ukonnra.wonderland"
  version = "0.0.1"

  repositories {
    jcenter()
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
  apply(plugin = "org.springframework.boot")
  apply(plugin = "org.jetbrains.kotlin.plugin.spring")
  apply(plugin = "io.spring.dependency-management")

  kapt.includeCompileClasspath = false

  dependencies {
    implementation("io.projectreactor:reactor-core")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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

  // For Proto staring libs
  if (name.endsWith("proto")) {
    dependencyManagement {
      imports {
        mavenBom("io.grpc:grpc-bom:1.36.0")
      }
    }

    apply(plugin = "com.google.protobuf")

    dependencies {
      api("javax.annotation:javax.annotation-api:${Versions.JAVAX_ANNOTATION}")
      api("com.google.protobuf:protobuf-java-util:${Versions.PROTOC}")
      implementation("io.grpc:grpc-netty-shaded")
      implementation("io.grpc:grpc-protobuf")
      implementation("io.grpc:grpc-stub")
    }

    protobuf {
      protoc {
        artifact = "com.google.protobuf:protoc:${Versions.PROTOC}"
      }
      plugins {
        id("grpc") {
          artifact = "io.grpc:protoc-gen-grpc-java"
        }
      }
      generateProtoTasks {
        all().forEach {
          it.plugins {
            id("grpc")
          }
        }
      }
    }

    idea {
      module {
        generatedSourceDirs.addAll(
          listOf(
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/java"),
          )
        )
      }
    }
  } else {
    // All non-proto libs should add spring dependencies

    configurations {
      developmentOnly
      runtimeClasspath {
        extendsFrom(configurations.developmentOnly.get())
      }
    }

    dependencyManagement {
      imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR10")
      }
    }

    dependencies {
      implementation("org.springframework.experimental:spring-graalvm-native:0.8.5")

      implementation("org.springframework.boot:spring-boot-starter-logging")
      implementation("org.springframework:spring-context-indexer")
      implementation("org.springframework.boot:spring-boot-starter-security")
      developmentOnly("org.springframework.boot:spring-boot-devtools")

      kapt("org.springframework.boot:spring-boot-configuration-processor")
      annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

      implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

      testImplementation("org.springframework.security:spring-security-test")
      testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
  }

  // For runnable applications
  if (name != "infrastructure" && !name.endsWith("proto")) {
    tasks.bootBuildImage {
      val imageTag = project.properties["imageTag"] ?: throw GradleException("imageTag should not be null")
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
      -Dspring.spel.ignore=false
      -Dspring.native.remove-yaml-support=true
        """.trimIndent()
      )

      if (System.getProperty("spring.profiles.active")?.equals("production") == true) {
        isPublish = true
        docker {
          publishRegistry {
            username = "ukonnra"
            email = "ukonnra@outlook.com"
            password = project.properties["dockerToken"]?.toString() ?: throw GradleException("imageTag should not be null")
          }
        }
      }
    }
  } else {
    tasks.bootJar {
      enabled = false
    }

    tasks.jar {
      enabled = true
    }
  }
}

val codeCoverageReport = tasks.register<JacocoReport>("codeCoverageReport") {
  setDependsOn(subprojects.map { it.tasks.test })

  executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

  subprojects.forEach {
    sourceSets(it.sourceSets["main"])
  }

  reports {
    xml.isEnabled = true
    xml.destination = file("$buildDir/reports/jacoco/report.xml")
    html.isEnabled = true
    csv.isEnabled = false
  }
}

tasks.check {
  dependsOn(codeCoverageReport)
}
