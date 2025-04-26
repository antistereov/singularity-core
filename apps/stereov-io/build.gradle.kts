plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
}

group = "io.stereov.singularity"
version = "0.0.1-SNAPSHOT"

val accessToken = properties["maven.accessToken"] as String?

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/bitwarden/sdk-sm")
        credentials {
            username = properties["gpr.user"] as String?
                ?: System.getenv("GPR_USER")
            password = properties["gpr.key"] as String?
                ?: System.getenv("GPR_KEY")
        }
    }
}

val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val testContainersVersion = "1.19.0"
val log4jVersion = "2.24.3"

dependencies {
    api(project(":libs:core"))

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
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}

tasks.test {
    useJUnitPlatform()
}
