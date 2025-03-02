package io.stereov.web

import io.stereov.web.config.AutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ImportAutoConfiguration(AutoConfiguration::class)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
