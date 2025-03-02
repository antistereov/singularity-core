import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
    id("java-library")
    id("org.jetbrains.kotlin.kapt") version "2.1.10"
}

kotlin {
    jvmToolchain(21)
}

group = "io.stereov.web"
version = "0.1.3"

val accessToken = properties["maven.accessToken"] as String?

repositories {
    mavenCentral()
}

val kotlinVersion = "2.0.21"
val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val log4jVersion = "2.24.3"
val testContainersVersion = "1.19.0"
val bucket4jVersion = "8.14.0"

dependencies {
    // Spring Boot Starters
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-autoconfigure")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // Security and JWT
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")

    // Reactive and Coroutines
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-reactor-netty")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxVersion")
    api("io.projectreactor.netty:reactor-netty:1.2.1")

    // Logging
    api("io.github.oshai:kotlin-logging-jvm:7.0.0")
    api("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    api("org.apache.logging.log4j:log4j-spring-boot:$log4jVersion")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    runtimeOnly("com.lmax:disruptor:3.4.4")

    // MongoDB
    api("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // Redis
    api("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    api("io.lettuce:lettuce-core:6.5.2.RELEASE")

    // Rate Limiting
    api("com.bucket4j:bucket4j_jdk17-core:$bucket4jVersion")
    api("com.bucket4j:bucket4j_jdk17-redis-common:$bucket4jVersion")
    api("com.bucket4j:bucket4j_jdk17-lettuce:$bucket4jVersion")

    // Serialization and Validation
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Mail
    api("org.springframework.boot:spring-boot-starter-mail")

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

publishing {
    publications {
        create<MavenPublication>("webSpringBoot") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "Gitea"
            url = uri("https://git.stereov.io/api/packages/baseline/maven")

            credentials(HttpHeaderCredentials::class.java) {
                name = "Authorization"
                value = "token $accessToken"
            }

            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}
