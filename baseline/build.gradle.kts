
import org.jreleaser.model.Active
import org.jreleaser.model.Signing

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
    id("java-library")
    id("org.jetbrains.kotlin.kapt") version "2.1.10"
    id("org.jreleaser") version "1.17.0"
}

kotlin {
    jvmToolchain(21)
}

group = "io.stereov.web"
version = "0.1.5"

repositories {
    mavenCentral()
}

val kotlinVersion = "2.0.21"
val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val log4jVersion = "2.24.3"
val testContainersVersion = "1.19.0"
val bucket4jVersion = "8.14.0"

val mavenCentralUsername: String = properties["mavencentral.username"] as String?
    ?: System.getenv("MAVENCENTRAL_USERNAME")
val mavenCentralPassword: String = properties["mavencentral.password"] as String?
    ?: System.getenv("MAVENCENTRAL_PASSWORD")
val gpgPassphrase: String = properties["gpg.passphrase"] as String?
    ?: System.getenv("GPG_PASSPHRASE")
val gpgUseFileCondition: String = properties["gpg.use-file"] as String?
    ?: "false"
val gpgUseFile: Boolean = gpgUseFileCondition.toBoolean()
val gpgSecretKey: String = properties["gpg.private-key"] as String?
    ?: System.getenv("GPG_PRIVATE_KEY")
val gpgPublicKey: String = properties["gpg.public-key"] as String?
    ?: System.getenv("GPG_PUBLIC_KEY")
val gitHubToken: String = properties["github.token"] as String?
    ?: System.getenv("GITHUB_TOKEN")

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

    // 2FA
    api("com.warrenstrange:googleauth:1.5.0")

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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "baseline"
                description = "Spring Boot Web Baseline"
                url = "https://github.com/antistereov/web-kotlin-spring-baseline"
                inceptionYear = "2025"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.html"
                    }
                }
                developers {
                    developer {
                        id = "antistereov"
                        name = "André Antimonov"
                        email = "andre.antimonov@stereov.io"
                        url = "https://stereov.io"
                    }
                }

                scm {
                    url = "https://github.com/antistereov/web-kotlin-spring-baseline"
                    connection = "scm:git:git://github.com/antistereov/web-kotlin-spring-baseline.git"
                    developerConnection = "scm:git:ssh://github.com/antistereov/web-kotlin-spring-baseline.git"
                }
            }
        }
    }

    repositories {
        maven {
            url = File("$projectDir/build/staging-deploy").toURI()
        }
    }
}

jreleaser {
    gitRootSearch.set(true)

    release {
        github {
            repoOwner.set("antistereov")
            name.set("web-kotlin-spring-baseline")
            tagName.set("$version")
            releaseName.set("Release $version")
            overwrite.set(true)
            skipTag.set(false)
            sign.set(true)
            token.set(gitHubToken)

            changelog {
                enabled.set(true)
                preset.set("conventional-commits")
                formatted.set(Active.ALWAYS)
                links.set(true)

                contributors {
                    enabled.set(true)
                }

                hide {
                    uncategorized.set(true)
                    categories.set(listOf("merge", "build", "refactor", "revert", "style", "test", "chore", "build", "ci", "docs"))
                    contributors.set(listOf("[bot]"))
                }

                append {
                    enabled.set(true)
                }
            }
        }
    }

    project {
        authors.set(listOf("André Antimonov"))
        license.set("GPL")
        links {
            homepage = "https://github.com/antistereov/web-kotlin-spring-baseline"
        }
        inceptionYear = "2025"
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)

        if (gpgUseFile) mode.set(Signing.Mode.FILE)

        publicKey.set(gpgPublicKey)
        secretKey.set(gpgSecretKey)
        passphrase.set(gpgPassphrase)
    }

    deploy {
        maven {
            mavenCentral {
                this.create("sonatype") {
                    username.set(mavenCentralUsername)
                    password.set(mavenCentralPassword)
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
