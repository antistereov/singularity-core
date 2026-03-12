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
import tools.jackson.core.JsonToken
import tools.jackson.databind.*
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.module.SimpleModule
import java.io.IOException
import java.util.*

@AutoConfiguration
class JsonConfiguration {

    @Bean
    fun jacksonModule(): SpringDataJackson3Configuration.PageModule {
        return SpringDataJackson3Configuration.PageModule(null)
    }

    @Bean
    fun simpleModule(): SimpleModule {
        val module = SimpleModule()
        module.addSerializer(ObjectId::class.java, ObjectIdToStringSerializer())
        module.addDeserializer(ObjectId::class.java, StringToObjectIdDeserializer())
        module.addDeserializer(Role::class.java, RoleDeserializer())
        return module
    }

    @Bean
    fun jacksonCustomizer(
        simpleModule: SimpleModule
    ) = JsonMapperBuilderCustomizer { builder ->
        builder.addModule(simpleModule)
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
            return when (p.currentToken()) {
                JsonToken.VALUE_STRING -> parseString(p, p.valueAsString)
                JsonToken.START_OBJECT -> parseObject(p)
                else -> throw InvalidFormatException(
                    p,
                    "Expected ObjectId as string or object, got token ${p.currentToken()}",
                    p.readValueAsTree<JsonNode>().asString(),
                    ObjectId::class.java
                )
            }
        }

        private fun parseString(p: JsonParser, value: String): ObjectId {
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

        private fun parseObject(p: JsonParser): ObjectId {
            val node = p.readValueAsTree<JsonNode>()

            node.get("value")?.asString()?.let { value ->
                return parseString(p, value)
            }

            val timestampNode = node.get("timestamp")
            if (timestampNode != null && timestampNode.canConvertToInt()) {
                val timestamp = timestampNode.asInt()

                return try {
                    ObjectId(Date(timestamp * 1000L))
                } catch (_: IllegalArgumentException) {
                    throw InvalidFormatException(
                        p,
                        "Invalid ObjectId timestamp representation: ${node.toPrettyString()}",
                        node,
                        ObjectId::class.java
                    )
                }
            }

            throw InvalidFormatException(
                p,
                "Expected ObjectId as string, {\"value\": ...}, or {\"timestamp\": ...}. Got: ${node.toPrettyString()}",
                node,
                ObjectId::class.java
            )
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
