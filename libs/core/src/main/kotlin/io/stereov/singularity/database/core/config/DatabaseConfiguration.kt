package io.stereov.singularity.database.core.config

import io.stereov.singularity.database.core.exception.handler.DatabaseExceptionHandler
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [
    ApplicationConfiguration::class,
    MongoConfiguration::class
])
class DatabaseConfiguration {


    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun databaseExceptionHandler() = DatabaseExceptionHandler()
}
