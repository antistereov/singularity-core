package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.model.InvalidCredentialsException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorSetupInitRequest
import io.stereov.singularity.auth.twofactor.model.TwoFactorInitSetupToken
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class TwoFactorInitSetupTokenService(
    private val authorizationService: AuthorizationService,
    private val hashService: HashService,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.InitSetup

    suspend fun create(req: TwoFactorSetupInitRequest, issuedAt: Instant = Instant.now()): TwoFactorInitSetupToken {
        logger.debug { "Creating init setup token" }

        val user = authorizationService.getCurrentUser()
        val sessionId = authorizationService.getCurrentsessionId()

        if (!hashService.checkBcrypt(req.password, user.password)) throw InvalidCredentialsException("Wrong password")

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(user.id.toHexString())
            .claim(Constants.JWT_session_CLAIM, sessionId)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return TwoFactorInitSetupToken(user.id, sessionId, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): TwoFactorInitSetupToken {
        logger.debug { "Extracting two factor setup init token" }

        val token = tokenValueExtractor.extractValue(exchange, tokenType)

        val jwt = jwtService.decodeJwt(token,true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != authorizationService.getCurrentUserId()) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        val sessionId = jwt.claims[Constants.JWT_session_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain session id")

        if (sessionId != authorizationService.getCurrentsessionId()) {
            throw InvalidTokenException("Step up token is not valid for current session")
        }

        return TwoFactorInitSetupToken(userId, sessionId, jwt)
    }
}