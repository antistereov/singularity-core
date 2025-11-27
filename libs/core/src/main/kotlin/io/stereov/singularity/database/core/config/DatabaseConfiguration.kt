package io.stereov.singularity.database.core.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration(after = [
    ApplicationConfiguration::class,
    MongoConfiguration::class
])
class DatabaseConfiguration
