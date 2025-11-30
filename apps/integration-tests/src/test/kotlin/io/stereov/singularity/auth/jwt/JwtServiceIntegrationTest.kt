package io.stereov.singularity.auth.jwt

import com.github.michaelbull.result.getOrThrow
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.service.JwtSecretService
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
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

    private val testTokenType = "test"

    private fun createJwt(secret: Secret, claims: JWTClaimsSet? = null, tokenType: String = testTokenType): String {
        val actualClaims = claims
            ?: JWTClaimsSet.Builder()
                .subject("test-user")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("role", "user")
                .claim(jwtService.tokenTypeClaim, tokenType)
                .build()

        val signer = MACSigner(secret.value.toByteArray())

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(secret.key)
                .build(),
            actualClaims
        )
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    @Test fun `valid JWT should decode successfully`() = runTest {
        val token = createJwt(jwtSecretService.getCurrentSecret().getOrThrow())

        val jwt = jwtService.decodeJwt(token, testTokenType).getOrThrow()

        Assertions.assertEquals("test-user", jwt.subject)
        Assertions.assertEquals("user", jwt.claims["role"])
    }
    @Test fun `JWT with secret that is not saved in key manager should fail`() = runTest {
        val tamperedToken = createJwt(
            Secret(
                UUID.randomUUID(),
                "value",
                jwtSecretService.generateKey(algorithm = "HmacSHA256").getOrThrow(),
                Instant.now()
            )
        )

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(tamperedToken, testTokenType).getOrThrow() }
    }
    @Test fun `JWT with wrong secret should fail`() = runTest {
        val tamperedToken = createJwt(
            Secret(
                UUID.randomUUID(),
                "value",
                jwtSecretService.generateKey(algorithm = "HmacSHA256").getOrThrow(),
                Instant.now()
            )
        )

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(tamperedToken, testTokenType).getOrThrow() }
    }
    @Test fun `JWT with missing key id should fail`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("test-user")
            .issueTime(Date())
            .expirationTime(Date(System.currentTimeMillis() + 100000))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(jwtSecretService.getCurrentSecret().getOrThrow().key)
                .build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().getOrThrow().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(token, testTokenType).getOrThrow() }
    }
    @Test fun `JWT with missing type should fail`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("test-user")
            .issueTime(Date())
            .expirationTime(Date(System.currentTimeMillis() + 100000))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().getOrThrow().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(token, "wrong-type").getOrThrow() }
    }
    @Test fun `JWT of wrong type should fail`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("test-user")
            .issueTime(Date())
            .expirationTime(Date(System.currentTimeMillis() + 100000))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().getOrThrow().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(token, "wrong-type").getOrThrow() }
    }
    @Test fun `JWT encoder should create a valid token`() = runTest {
        val claims = JwtClaimsSet.builder()
            .subject("test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("role", "admin")
            .claim(jwtService.tokenTypeClaim, testTokenType)
            .build()

        val encoded = jwtService.encodeJwt(claims, testTokenType).getOrThrow()
        val jwt = jwtService.decodeJwt(encoded.tokenValue, testTokenType).getOrThrow()

        Assertions.assertEquals("test-user", jwt.subject)
        Assertions.assertEquals("admin", jwt.claims["role"])
    }

    @Test fun `unsigned JWT should fail decoding`() = runTest {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"test-user","exp":${Instant.now().epochSecond + 3600}}""".toByteArray())

        val unsignedJwt = "$header.$payload."

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(unsignedJwt, testTokenType).getOrThrow() }
    }

    @Test fun `unexpired JWT should be decoded`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("unexpired-user")
            .issueTime(Date.from(Instant.now().minusSeconds(7200)))
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("role", "user")
            .claim(jwtService.tokenTypeClaim, testTokenType)
            .build()

        val token = createJwt(jwtSecretService.getCurrentSecret().getOrThrow(), claims)

        Assertions.assertEquals("unexpired-user", jwtService.decodeJwt(token, testTokenType).getOrThrow().subject)
        Assertions.assertEquals("user", jwtService.decodeJwt(token, testTokenType).getOrThrow().claims["role"])
    }
    @Test fun `expired JWT should fail decoding`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("expired-user")
            .issueTime(Date.from(Instant.now().minusSeconds(7200)))
            .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
            .claim(jwtService.tokenTypeClaim, testTokenType)
            .claim("role", "user")
            .build()

        val token = createJwt(jwtSecretService.getCurrentSecret().getOrThrow(), claims)

        assertThrows<TokenExtractionException.Expired> { jwtService.decodeJwt(token, testTokenType).getOrThrow() }
    }

    @Test fun `JWT with past nbf should be decoded`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("future-user")
            .claim("role", "user")
            .claim(jwtService.tokenTypeClaim, testTokenType)
            .notBeforeTime(Date.from(Instant.now().minusSeconds(300))) // ⏱ 5 Minuten in der Zukunft
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(jwtSecretService.getCurrentSecret().getOrThrow().key).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().getOrThrow().value.toByteArray()))
        val token = signedJWT.serialize()

        Assertions.assertEquals("future-user", jwtService.decodeJwt(token, testTokenType).getOrThrow().subject)
        Assertions.assertEquals("user", jwtService.decodeJwt(token, testTokenType).getOrThrow().claims["role"])
    }
    @Test fun `JWT with future nbf should fail decoding`() = runTest {
        val claims = JWTClaimsSet.Builder()
            .subject("future-user")
            .notBeforeTime(Date.from(Instant.now().plusSeconds(300))) // ⏱ 5 Minuten in der Zukunft
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .build()

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(jwtSecretService.getCurrentSecret().getOrThrow().id.toString()).build(),
            claims
        )
        signedJWT.sign(MACSigner(jwtSecretService.getCurrentSecret().getOrThrow().value.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(token, testTokenType).getOrThrow() }
    }

    @Test fun `JWT with manipulated payload should fail`() = runTest {
        val token = createJwt(jwtSecretService.getCurrentSecret().getOrThrow())

        val parts = token.split(".")
        val fakePayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"hacker"}""".toByteArray())

        val tampered = "${parts[0]}.$fakePayload.${parts[2]}"

        assertThrows<TokenExtractionException> { jwtService.decodeJwt(tampered, testTokenType).getOrThrow() }
    }
    @Test fun `JWT with invalid base64 should fail decoding`() = runTest {
        val invalidToken = "abc.def$%.ghi"

        assertThrows<TokenExtractionException> {
            jwtService.decodeJwt(invalidToken, testTokenType).getOrThrow()
        }
    }
}