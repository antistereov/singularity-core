package io.stereov.singularity.database.core.config

import com.github.michaelbull.result.fold
import io.stereov.singularity.principal.core.model.Role
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@AutoConfiguration
class MongoConfiguration {

    @Bean
    fun customConversions(): MongoCustomConversions {

        return MongoCustomConversions(listOf(
            StringToRoleConverter()
        ))
    }

    @ReadingConverter
    class StringToRoleConverter : Converter<String, Role> {
        override fun convert(source: String): Role {

            return Role.fromString(source)
                .fold(
                    { role -> role },
                    { error -> throw error }
                )
        }
    }
}
