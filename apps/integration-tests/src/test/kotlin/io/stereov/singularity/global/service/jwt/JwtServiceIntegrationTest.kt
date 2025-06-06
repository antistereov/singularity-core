package io.stereov.singularity.global.service.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.secrets.model.Secret
import io.stereov.singularity.jwt.service.JwtSecretService
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import java.time.Instant
import java.util.*

class JwtServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var jwtSecretService: JwtSecretService

    @Autowired
    private lateinit var jwtService: JwtService

    private fun createJwt(secret: Secret, claims: JWTClaimsSet? = null): String {
        val actualClaims = claims
            ?: JWTClaimsSet.Builder()
                .subject("test-user")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("role", "user")
                .build()

        val signer = MACSigner(secret.value.toByteArray())

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(secret.id.toString())
                .build(),
            actualClaims
        )
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    @Test fun `valid JWT should decode successfully`() = runTest {
        val token = createJwt(jwtSecretService.getCurrentSecret())

        val jwt = jwtService.decodeJwt(token)

        assertEquals("test-user", jwt.subject)
        assertEquals("user", jwt.claims["role"])
    }
    @Test fun `JWT with secret that is not saved in key manager should fail`() = runTest {
        val tamperedToken = createJwt(Secret(UUID.randomUUID(), "value", jwtSecretService.generateKey(algorithm = "HmacSHA256"), Instant.now()))

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(tamperedToken) }
    }
    @Test fun `JWT with wrong secret should fail`() = runTest {
        val tamperedToken = createJwt(Secret(UUID.randomUUID(), "value", jwtSecretService.generateKey(algorithm = "HmacSHA256"), Instant.now()))

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(tamperedToken) }
    }
    @Test fun `JWT with missing key id should fail`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("test-user")
            .issueTime(Date())
            .expirationTime(Date(System.currentTimeMillis() + 100000))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(token) }
    }
    @Test fun `JWT encoder should create a valid token`() = runTest {
        val claims = JwtClaimsSet.builder()
            .subject("test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("role", "admin")
            .build()

        val encoded = jwtService.encodeJwt(claims)
        val jwt = jwtService.decodeJwt(encoded)

        assertEquals("test-user", jwt.subject)
        assertEquals("admin", jwt.claims["role"])
    }

    @Test fun `unsigned JWT should fail decoding`() = runTest {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"test-user","exp":${Instant.now().epochSecond + 3600}}""".toByteArray())

        val unsignedJwt = "$header.$payload."

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(unsignedJwt) }
    }

    @Test fun `unexpired JWT should be decoded`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("unexpired-user")
            .issueTime(Date.from(Instant.now().minusSeconds(7200)))
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("role", "user")
            .build()

        val token = createJwt(jwtSecretService.getCurrentSecret(), claims)

        assertEquals("unexpired-user", jwtService.decodeJwt(token).subject)
        assertEquals("user", jwtService.decodeJwt(token).claims["role"])
    }
    @Test fun `expired JWT should fail decoding`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("expired-user")
            .issueTime(Date.from(Instant.now().minusSeconds(7200)))
            .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
            .claim("role", "user")
            .build()

        val token = createJwt(jwtSecretService.getCurrentSecret(), claims)

        assertThrows<TokenExpiredException> { jwtService.decodeJwt(token, true) }
    }

    @Test fun `JWT with past nbf should be decoded`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("future-user")
            .claim("role", "user")
            .notBeforeTime(Date.from(Instant.now().minusSeconds(300))) // ⏱ 5 Minuten in der Zukunft
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(jwtSecretService.getCurrentSecret().id.toString()).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().value.toByteArray()))
        val token = signedJWT.serialize()

        assertEquals("future-user", jwtService.decodeJwt(token).subject)
        assertEquals("user", jwtService.decodeJwt(token).claims["role"])
    }
    @Test fun `JWT with future nbf should fail decoding`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("future-user")
            .notBeforeTime(Date.from(Instant.now().plusSeconds(300))) // ⏱ 5 Minuten in der Zukunft
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(jwtSecretService.getCurrentSecret().id.toString()).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(token) }
    }

    @Test fun `JWT with manipulated payload should fail`() = runTest {
        val token = createJwt(jwtSecretService.getCurrentSecret())

        val parts = token.split(".")
        val fakePayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"hacker"}""".toByteArray())

        val tampered = "${parts[0]}.$fakePayload.${parts[2]}"

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(tampered) }
    }
    @Test fun `JWT with invalid base64 should fail decoding`() = runTest {
            val invalidToken = "abc.def$%.ghi"

            assertThrows<InvalidTokenException> {
                jwtService.decodeJwt(invalidToken)
            }
        }
}
