plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
}

repositories {
    mavenCentral()
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(25)
}

val kotlinxVersion = "1.10.2"
val testContainersVersion = "2.0.3"

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
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-mongodb:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-vault:${testContainersVersion}")

    testImplementation("org.testcontainers:testcontainers-minio:${testContainersVersion}")
    testImplementation("io.minio:minio:8.6.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    jvmArgs("-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=build/oom.hprof")
    maxHeapSize = "2560m"
    maxParallelForks = 1
    failFast = true
}
