package io.stereov.singularity.stereovio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories(basePackages = ["io.stereov.singularity.stereovio"])
class StereovWebApplication

fun main() {
    runApplication<StereovWebApplication>()
}
