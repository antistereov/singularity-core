plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
}

repositories {
    mavenCentral()
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":libs:core"))
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.25.0")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}
