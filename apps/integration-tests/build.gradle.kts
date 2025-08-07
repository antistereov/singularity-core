plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"

    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

repositories {
    mavenCentral()
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(21)
}

val kotlinxVersion = "1.10.1"
val testContainersVersion = "1.21.3"

dependencies {
    // Web Starter
    // Since this is configured as a module on the same repository, it uses a direct import.
    // You should use:
    // implementation("io.stereov.web:baseline:<version>")
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
    testImplementation("org.testcontainers:vault:${testContainersVersion}")

    testImplementation("org.testcontainers:minio:${testContainersVersion}")
    testImplementation("io.minio:minio:8.5.17")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("forkedSpringBootRun") {
    dependsOn("assemble")
}

openApi {
    apiDocsUrl.set("http://localhost:8000/api/openapi")
    outputDir.set(file("$rootDir/docs/static/openapi"))
    outputFileName.set("openapi.yaml")
}
