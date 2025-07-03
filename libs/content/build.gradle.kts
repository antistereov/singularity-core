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
    id("org.jreleaser") version "1.19.0"
}

group = properties["group"] as String
version = properties["version"] as String

kotlin {
    jvmToolchain(21)
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

dependencies {
    api(project(":libs:core"))

    testImplementation(kotlin("test"))
}

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "singularity-content"
                description = "Spring Boot Web Starter - Content Management"
                url = "https://github.com/antistereov/singularity-content"
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
                    url = "https://github.com/antistereov/singularity-content"
                    connection = "scm:git:git://github.com/antistereov/singularity-content.git"
                    developerConnection = "scm:git:ssh://github.com/antistereov/singularity-content.git"
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
            name.set("singularity-content")
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
                    target.set(File("$rootDir/libs/content/CHANGELOG.md"))
                    enabled.set(true)
                }
            }
        }
    }

    project {
        authors.set(listOf("André Antimonov"))
        license.set("GPL")
        links {
            homepage = "https://github.com/antistereov/singularity-content"
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
