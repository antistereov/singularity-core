plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
    id("java-library")
    id("org.jreleaser") version "1.19.0"
}

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

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = properties["group"] as String
    version = properties["version"] as String

    kotlin {
        jvmToolchain(21)
    }

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

    configurations.all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }
}
