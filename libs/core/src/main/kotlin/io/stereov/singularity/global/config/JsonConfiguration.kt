package io.stereov.singularity.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean

@AutoConfiguration
class JsonConfiguration {

    @Bean
    fun jacksonCustomizer() = Jackson2ObjectMapperBuilderCustomizer {
        it.serializerByType(ObjectId::class.java, ObjectIdToStringSerializer())
        it.serializerByType(TwoFactorMethod::class.java, TwoFactorMethodSerializer())
        it.deserializerByType(TwoFactorMethod::class.java, TwoFactorMethodDeserializer())
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

    class TwoFactorMethodSerializer : StdSerializer<TwoFactorMethod>(TwoFactorMethod::class.java) {
        override fun serialize(
            value: TwoFactorMethod,
            gen: JsonGenerator,
            provider: SerializerProvider
        ) {
            gen.writeString(value.value)
        }
    }

    class TwoFactorMethodDeserializer : StdDeserializer<TwoFactorMethod>(TwoFactorMethod::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TwoFactorMethod {
            return TwoFactorMethod.ofString(p.text)
        }
    }
}
