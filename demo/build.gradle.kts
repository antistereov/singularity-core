import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
}

group = "io.stereov.web"
version = "0.0.1-SNAPSHOT"

val accessToken = properties["maven.accessToken"] as String?

repositories {
    mavenCentral()
}

val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val testContainersVersion = "1.19.0"
val log4jVersion = "2.24.3"

dependencies {
    // Web Starter
    // Since this is configured as a module on the same repository, it uses a direct import.
    // You should use:
    // implementation("io.stereov.web:baseline:<version>")
    api(project(":baseline"))
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
