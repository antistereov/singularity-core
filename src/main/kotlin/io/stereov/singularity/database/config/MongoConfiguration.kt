package io.stereov.singularity.database.config

import io.stereov.singularity.global.language.util.LanguageToStringConverter
import io.stereov.singularity.global.language.util.StringToLanguageConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@AutoConfiguration
class MongoConfiguration {

    @Bean
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                StringToLanguageConverter(),
                LanguageToStringConverter()
            )
        )
    }
}
