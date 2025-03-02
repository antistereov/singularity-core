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

val accessToken = properties["maven.accessToken"] as String

repositories {
    mavenCentral()
}

val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val testContainersVersion = "1.19.0"

dependencies {
    // Web Starter
    api(project(":web-spring-boot-autoconfigure"))

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools:$springBootVersion")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:mongodb:$testContainersVersion")
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
