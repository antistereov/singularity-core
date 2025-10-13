package io.stereov.singularity.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JsonConfiguration {

    @Bean
    fun jacksonCustomizer() = Jackson2ObjectMapperBuilderCustomizer {
        it.serializerByType(ObjectId::class.java, ObjectIdToStringSerializer())
    }

    class ObjectIdToStringSerializer : StdSerializer<ObjectId>(ObjectId::class.java) {
        override fun serialize(
            value: ObjectId,
            gen: JsonGenerator,
            provider: SerializerProvider
        ) {
            gen.writeString(value.toHexString())
        }
    }
}
