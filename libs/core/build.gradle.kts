
import org.jreleaser.model.Active
import org.jreleaser.model.Signing
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
    id("java-library")
    id("org.jreleaser") version "1.17.0"
}

kotlin {
    jvmToolchain(21)
}

group = "io.stereov.singularity"
version = "1.6.5-SNAPSHOT"

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

val kotlinVersion = "2.0.21"
val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.4"
val bucket4jVersion = "8.14.0"

val mavenCentralUsername: String? = properties["mavencentral.username"] as String?
    ?: System.getenv("MAVENCENTRAL_USERNAME")
val mavenCentralPassword: String? = properties["mavencentral.password"] as String?
    ?: System.getenv("MAVENCENTRAL_PASSWORD")
val gpgPassphrase: String? = properties["gpg.passphrase"] as String?
    ?: System.getenv("GPG_PASSPHRASE")
val gpgUseFileCondition: String? = properties["gpg.use-file"] as String?
    ?: "false"
val gpgUseFile: Boolean = gpgUseFileCondition.toBoolean()
val gpgSecretKey: String? = properties["gpg.private-key"] as String?
    ?: System.getenv("GPG_PRIVATE_KEY")
val gpgPublicKey: String? = properties["gpg.public-key"] as String?
    ?: System.getenv("GPG_PUBLIC_KEY")
val gitHubToken: String? = properties["github.token"] as String?
    ?: System.getenv("GITHUB_TOKEN")

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

    // File Storage
    implementation(platform("software.amazon.awssdk:bom:2.27.21"))
    implementation("software.amazon.awssdk:s3")

    // 2FA
    api("com.warrenstrange:googleauth:1.5.0")

    // Reactive and Coroutines
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-reactor-netty")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxVersion")
    api("io.projectreactor.netty:reactor-netty:1.2.1")

    // Development
    api("org.springframework.boot:spring-boot-devtools:$springBootVersion")

    // Logging
    api("io.github.oshai:kotlin-logging-jvm:7.0.6")
    api("org.springframework.boot:spring-boot-starter-log4j2")
    api("com.lmax:disruptor:3.4.4")

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
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Mail
    api("org.springframework.boot:spring-boot-starter-mail")

    // Secrets
    implementation("com.bitwarden:sdk-secrets:1.0.1")
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging")
    exclude(group = "org.springframework", module = "spring-webmvc")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.register("changelog") {
    group = "release"
    description = "Runs clean, publish, and jReleaserChangelog tasks in sequence."

    dependsOn("clean", "publish", "jreleaserChangelog")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "baseline"
                description = "Spring Boot Web Baseline"
                url = "https://github.com/antistereov/singularity"
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
                    url = "https://github.com/antistereov/singularity"
                    connection = "scm:git:git://github.com/antistereov/singularity.git"
                    developerConnection = "scm:git:ssh://github.com/antistereov/singularity.git"
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
                    target.set(File("$rootDir/CHANGELOG.md"))
                    enabled.set(true)
                }
            }
        }
    }

    project {
        authors.set(listOf("André Antimonov"))
        license.set("GPL")
        links {
            homepage = "https://github.com/antistereov/singularity"
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
                    applyMavenCentralRules.set(true)

                    username.set(mavenCentralUsername)
                    password.set(mavenCentralPassword)

                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")

                    maxRetries.set(100)
                    retryDelay.set(60)
                }
            }
        }
    }
}

tasks.register("checkVersion") {
    doLast {
        val projectVersion = project.version.toString()
        println(projectVersion)
    }
}
