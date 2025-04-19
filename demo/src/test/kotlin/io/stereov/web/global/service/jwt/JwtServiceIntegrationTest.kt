package io.stereov.web.global.service.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.model.TokenExpiredException
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.test.BaseIntegrationTest
import io.stereov.web.test.config.MockKeyManager
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.time.Instant
import java.util.*

class JwtServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var keyManager: KeyManager

    @Autowired
    private lateinit var mockKeyManager: MockKeyManager

    @Autowired
    private lateinit var jwtService: JwtService

    private var keyId: UUID? = null
    private var secret: String? = null

    @PostConstruct
    fun initialize() {
        this.keyId = mockKeyManager.jwtId
        this.secret = keyManager.getSecretById(keyId!!).value
    }

    private fun createJwt(key: String, keyId: UUID, claims: JWTClaimsSet? = null): String {
        val actualClaims = claims
            ?: JWTClaimsSet.Builder()
                .subject("test-user")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .claim("role", "user")
                .build()

        val signer = MACSigner(key.toByteArray())

        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256)
                .keyID(keyId.toString())
                .build(),
            actualClaims
        )
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    @Test fun `valid JWT should decode successfully`() = runTest {
        val token = createJwt(secret!!, keyId!!)

        val jwt = jwtService.decodeJwt(token)

        assertEquals("test-user", jwt.subject)
        assertEquals("user", jwt.claims["role"])
    }
    @Test fun `JWT with invalid signature should fail`() = runTest {
        val tamperedToken = createJwt(keyManager.generateKey(algorithm = "HmacSHA256"), keyId!!)

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
        signedJWT.sign(MACSigner(secret!!.toByteArray()))
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

        val jwsHeader = JwsHeader
            .with { "HS256" }
            .keyId(keyId.toString())
            .build()

        val encoded = jwtService.encodeJwt(JwtEncoderParameters.from(jwsHeader, claims))

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

        val token = createJwt(secret!!, keyId!!, claims)

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

        val token = createJwt(secret!!, keyId!!, claims)

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
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(keyId.toString()).build(),
            claims
        )
        signedJWT.sign(MACSigner(secret!!.toByteArray()))
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
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(keyId.toString()).build(),
            claims
        )
        signedJWT.sign(MACSigner(secret!!.toByteArray()))
        val token = signedJWT.serialize()

        assertThrows<InvalidTokenException> { jwtService.decodeJwt(token) }
    }

    @Test fun `JWT with manipulated payload should fail`() = runTest {
        val token = createJwt(secret!!, keyId!!)

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
