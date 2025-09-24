plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"

    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":libs:core"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("forkedSpringBootRun") {
    dependsOn("assemble")
}

openApi {
    apiDocsUrl.set("http://localhost:8000/api/openapi.yaml")
    outputDir.set(file("$rootDir/docs/static/openapi"))
    outputFileName.set("openapi.yaml")
}

tasks.register("updateDocusaurusOpenApiDocs") {
    group = "openApi"

    dependsOn("generateOpenApiDocs")

    doLast {
        exec {
            commandLine = listOf("yarn", "docusaurus", "clean-api-docs", "all")
            workingDir = File("$rootDir/docs")
        }
        exec {
            commandLine = listOf("yarn", "docusaurus", "gen-api-docs", "all")
            workingDir = File("$rootDir/docs")
        }
    }
}
