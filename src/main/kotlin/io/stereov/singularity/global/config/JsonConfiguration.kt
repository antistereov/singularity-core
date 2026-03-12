package io.stereov.singularity.global.config

import com.github.michaelbull.result.getOrElse
import io.stereov.singularity.principal.core.exception.RoleException
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.data.web.config.SpringDataJackson3Configuration
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.module.SimpleModule
import java.io.IOException

@AutoConfiguration
class JsonConfiguration {

    @Bean
    fun jacksonModule(): SpringDataJackson3Configuration.PageModule {
        return SpringDataJackson3Configuration.PageModule(null)
    }

    @Bean
    fun jacksonCustomizer() = JsonMapperBuilderCustomizer { builder ->
        val module = SimpleModule()
        module.addSerializer(ObjectId::class.java, ObjectIdToStringSerializer())
        module.addDeserializer(ObjectId::class.java, StringToObjectIdDeserializer())
        builder.addModule(module)
    }

    class ObjectIdToStringSerializer : ValueSerializer<ObjectId>() {
        override fun serialize(
            value: ObjectId,
            gen: JsonGenerator,
            ctxt: SerializationContext
        ) {
            gen.writeString(value.toHexString())
        }
    }

    class StringToObjectIdDeserializer : ValueDeserializer<ObjectId>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): ObjectId {
            val value = p.valueAsString
            return try {
                ObjectId(value)
            } catch (_: IllegalArgumentException) {
                throw InvalidFormatException(
                    p,
                    "Invalid ObjectId: $value",
                    value,
                    ObjectId::class.java
                )
            }
        }
    }

    class RoleDeserializer : ValueDeserializer<Role>() {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): Role {
            val roleString = p.readValueAs(String::class.java)

            return Role.fromString(roleString)
                .getOrElse { e: RoleException.Invalid ->
                    throw IOException("Invalid role string '$roleString': ${e.message}", e)
                }
        }
    }
}
