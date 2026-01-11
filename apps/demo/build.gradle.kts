plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
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
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.29.0")
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
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
        providers.exec {
            commandLine = listOf("yarn", "docusaurus", "clean-api-docs", "all")
            workingDir = File("$rootDir/docs")
        }
        providers.exec {
            commandLine = listOf("yarn", "docusaurus", "gen-api-docs", "all")
            workingDir = File("$rootDir/docs")
        }
    }
}

tasks.named("updateDocusaurusOpenApiDocs") {
    // This ensures that 'replaceDocsLinks' is run immediately after
    // 'updateDocusaurusOpenApiDocs' completes successfully.
    finalizedBy("replaceDocsLinks")
}
open class ReplaceStringTask : DefaultTask() {

    // Define properties for task inputs
    // The target directory path is relative to the project directory
    @Input
    var targetDirPath: String = "../../docs/docs/api"

    @Input
    var findString: String = "https://singularity.stereov.io/docs/"

    @Input
    var replaceString: String = "../../docs/"

    init {
        group = "documentation"
        description = "Replaces specified strings in files within a target directory."
    }

    @TaskAction
    fun replaceStrings() {
        val targetDir = project.file(targetDirPath)

        if (!targetDir.isDirectory) {
            logger.warn("Target directory not found: ${targetDir.absolutePath}. Skipping task.")
            return
        }

        // The findString needs to be escaped for regex if it contains special characters like '/'
        // Groovy/Java/Kotlin's replaceAll uses regex for the search pattern.
        val escapedFindString = findString.replace("/", "\\/")
            .replace(".", "\\.") // Escaping '.' just in case
            .replace(":", "\\:") // Escaping ':' just in case

        project.fileTree(targetDir).files.forEach { file ->
            if (file.isFile) {
                logger.info("Processing file: ${file.name}")

                val originalText = file.readText(Charsets.UTF_8)
                val newText = originalText.replace(Regex(escapedFindString), replaceString)

                if (originalText != newText) {
                    file.writeText(newText, Charsets.UTF_8)
                    logger.lifecycle("  -> Replaced occurrences in ${file.name}")
                } else {
                    logger.info("  -> No changes needed in ${file.name}")
                }
            }
        }
    }
}

// Register the custom task
tasks.register<ReplaceStringTask>("replaceDocsLinks")