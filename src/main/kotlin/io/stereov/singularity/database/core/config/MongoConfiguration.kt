package io.stereov.singularity.database.core.config

import io.stereov.singularity.content.translate.util.LanguageToStringConverter
import io.stereov.singularity.content.translate.util.StringToLanguageConverter
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
