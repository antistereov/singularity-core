package io.stereov.singularity.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.github.michaelbull.result.getOrElse
import io.stereov.singularity.principal.core.exception.RoleException
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import java.io.IOException

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

    class RoleDeserializer : StdDeserializer<Role>(Role::class.java) {

        @Throws(IOException::class)
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Role {
            val roleString = p.readValueAs(String::class.java)

            return Role.fromString(roleString)
                .getOrElse { e: RoleException.Invalid ->
                    throw IOException("Invalid role string '$roleString': ${e.message}", e)
                }
        }
    }
}
