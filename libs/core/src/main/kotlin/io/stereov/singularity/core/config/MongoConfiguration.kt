package io.stereov.singularity.core.config

import io.stereov.singularity.core.global.language.util.LanguageToStringConverter
import io.stereov.singularity.core.global.language.util.StringToLanguageConverter
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
