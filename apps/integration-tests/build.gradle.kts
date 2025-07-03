val accessToken = properties["maven.accessToken"] as String?

val kotlinxVersion = "1.10.1"
val springBootVersion = "3.4.1"
val testContainersVersion = "1.19.0"

dependencies {
    // Web Starter
    // Since this is configured as a module on the same repository, it uses a direct import.
    // You should use:
    // implementation("io.stereov.web:baseline:<version>")
    api(project(":libs:core"))
    api(project(":libs:content"))

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

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}

tasks.test {
    useJUnitPlatform()
}
