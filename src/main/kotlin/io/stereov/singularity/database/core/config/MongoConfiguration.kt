package io.stereov.singularity.database.core.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@AutoConfiguration
class MongoConfiguration {

    @Bean
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf<MongoCustomConversions>()
        )
    }
}
