
import org.jreleaser.model.Active
import org.jreleaser.model.Signing
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("java-library")
    id("org.jreleaser") version "1.21.0"
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(25)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val kotlinVersion = "2.3.0"
val kotlinxVersion = "1.10.2"
val springBootVersion = "4.0.1"
val bucket4jVersion = "8.16.0"

dependencies {
    // Spring Boot Starters
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Security and JWT
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")
    api("org.springframework.boot:spring-boot-starter-oauth2-client")

    // File Storage
    implementation(platform("software.amazon.awssdk:bom:2.41.5"))
    implementation("software.amazon.awssdk:s3")

    // 2FA
    api("com.warrenstrange:googleauth:1.5.0")

    // Reactive and Coroutines
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-reactor-netty")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxVersion")
    api("io.projectreactor.netty:reactor-netty:1.3.1")

    // Development
    api("org.springframework.boot:spring-boot-devtools:$springBootVersion")

    // Logging
    api("io.github.oshai:kotlin-logging-jvm:7.0.14")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    api("com.lmax:disruptor:4.0.0")

    // MongoDB
    api("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // Redis
    api("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    api("io.lettuce:lettuce-core:7.2.1.RELEASE")

    // R2DBC and H2
    api("org.springframework.boot:spring-boot-starter-data-r2dbc")
    api("io.r2dbc:r2dbc-h2")

    // Rate Limiting
    api("com.bucket4j:bucket4j_jdk17-core:$bucket4jVersion")
    api("com.bucket4j:bucket4j_jdk17-redis-common:$bucket4jVersion")
    api("com.bucket4j:bucket4j_jdk17-lettuce:$bucket4jVersion")

    // Geolocation
    api("com.maxmind.geoip2:geoip2:5.0.2")

    // Compression
    api("org.apache.commons:commons-compress:1.28.0")

    // Image Scaling
    api("com.sksamuel.scrimage:scrimage-core:4.3.5")
    api("com.sksamuel.scrimage:scrimage-formats-extra:4.3.5")
    api("com.sksamuel.scrimage:scrimage-webp:4.3.5")

    // Serialization and Validation
    api("org.springframework.boot:spring-boot-starter-validation")
    api("tools.jackson.module:jackson-module-kotlin")

    // Mail
    api("org.springframework.boot:spring-boot-starter-mail")

    // Secrets
    implementation("org.springframework.vault:spring-vault-core:4.0.0")

    // Slugs
    implementation("com.github.slugify:slugify:3.0.7")

    // Documentation
    api("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.1")

    // Result
    api("com.michael-bull.kotlin-result:kotlin-result:2.1.0")
    api("com.michael-bull.kotlin-result:kotlin-result-coroutines:2.1.0")
}

configurations.all {
    exclude(group = "org.springframework", module = "spring-webmvc")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

val mavenCentralUsername: String? = properties["mavencentral.username"] as String?
    ?: System.getenv("MAVENCENTRAL_USERNAME")
val mavenCentralPassword: String? = properties["mavencentral.password"] as String?
    ?: System.getenv("MAVENCENTRAL_PASSWORD")
val gpgPassphrase: String? = properties["gpg.passphrase"] as String?
    ?: System.getenv("GPG_PASSPHRASE")
val gpgUseFileCondition: String = properties["gpg.use-file"] as String?
    ?: "false"
val gpgUseFile: Boolean = gpgUseFileCondition.toBoolean()
val gpgSecretKey: String? = properties["gpg.private-key"] as String?
    ?: System.getenv("GPG_PRIVATE_KEY")
val gpgPublicKey: String? = properties["gpg.public-key"] as String?
    ?: System.getenv("GPG_PUBLIC_KEY")
val gitHubToken: String? = properties["github.token"] as String?
    ?: System.getenv("GITHUB_TOKEN")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "singularity-core"
                description = "Spring Boot Web Starter - Core"
                url = "https://github.com/antistereov/singularity-core"
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
                        url = "https://github.com/antistereov"
                    }
                }

                scm {
                    url = "https://github.com/antistereov/singularity-core"
                    connection = "scm:git:git://github.com/antistereov/singularity-core.git"
                    developerConnection = "scm:git:ssh://github.com/antistereov/singularity-core.git"
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
            name.set("singularity")
            overwrite.set(true)
            skipTag.set(false)
            sign.set(true)
            token.set(gitHubToken)
            immutableRelease.set(false)

            uploadAssets.set(Active.ALWAYS)

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
                    categories.set(listOf("merge"))
                    contributors.set(listOf("[bot]"))
                }

                append {
                    target.set(File("$rootDir/libs/core/CHANGELOG.md"))
                    enabled.set(true)
                }
            }
        }
    }

    project {
        authors.set(listOf("André Antimonov"))
        license.set("GPL")
        links {
            homepage = "https://github.com/antistereov/singularity-core"
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
                register("sonatype") {
                    applyMavenCentralRules = true

                    username = mavenCentralUsername
                    password = mavenCentralPassword

                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")

                    maxRetries = 100
                    retryDelay = 60
                }
            }
        }
    }
}
