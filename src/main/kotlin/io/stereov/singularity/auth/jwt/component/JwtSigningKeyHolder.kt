package io.stereov.singularity.auth.jwt.component

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.OctetSequenceKey
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.spec.SecretKeySpec

@Component
class JwtSigningKeyHolder {

    private val currentJwk = AtomicReference<OctetSequenceKey?>()

    fun set(secret: ByteArray) {
        val secretKey = SecretKeySpec(secret, "HmacSHA256")
        val jwk = OctetSequenceKey.Builder(secretKey)
            .algorithm(JWSAlgorithm.HS256)
            .build()

        currentJwk.set(jwk)
    }

    fun get(): OctetSequenceKey {
        return currentJwk.get()
            ?: throw IllegalStateException("JWT signing key is not initialized")
    }
}