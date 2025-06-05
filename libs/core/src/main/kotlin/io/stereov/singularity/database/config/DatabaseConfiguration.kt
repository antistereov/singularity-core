package io.stereov.singularity.database.config

import io.stereov.singularity.database.exception.handler.DatabaseExceptionHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class DatabaseConfiguration {


    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun databaseExceptionHandler() = DatabaseExceptionHandler()
}
