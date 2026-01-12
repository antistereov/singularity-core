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
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
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

    class RoleDeserializer : ValueDeserializer<Role>() {
        override fun deserialize(
            p: tools.jackson.core.JsonParser,
            ctxt: tools.jackson.databind.DeserializationContext
        ): Role {
            val roleString = p.readValueAs(String::class.java)

            return Role.fromString(roleString)
                .getOrElse { e: RoleException.Invalid ->
                    throw IOException("Invalid role string '$roleString': ${e.message}", e)
                }
        }
    }
}
