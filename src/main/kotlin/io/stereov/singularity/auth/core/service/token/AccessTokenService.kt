package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.exception.AccessTokenException
import io.stereov.singularity.auth.core.exception.model.TokenMissingException
import io.stereov.singularity.auth.core.model.token.AccessToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.exception.model.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.model.TokenException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.global.util.catchAs
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*
import kotlin.String
import kotlin.getOrElse
import kotlin.runCatching

/**
 * Service for creating and extracting [AccessToken]s.
 *
 * @author antistereov
 */
@Service
class AccessTokenService(
    private val jwtService: JwtService,
    private val accessTokenCache: AccessTokenCache,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Access

    /**
     * Creates an [AccessToken] for a given [UserDocument] and session.
     *
     * @param user The [UserDocument] the [AccessToken] should be created for.
     * @param sessionId The [UUID] of the session this [AccessToken] should be linked to.
     * @param issuedAt Optional issue time. Defaults to the current time.
     */
    suspend fun create(
        user: UserDocument,
        sessionId: UUID,
        issuedAt: Instant = Instant.now()
    ): Result<AccessToken, TokenCreationException.Encoding> {
        logger.debug { "Creating access token for user ${user.id} and session $sessionId" }

        val tokenId = Random.generateString(20)

        accessTokenCache.addTokenId(user.id, sessionId, tokenId)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(user.id.toHexString())
            .claim(Constants.JWT_ROLES_CLAIM, user.roles)
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .claim(Constants.JWT_GROUPS_CLAIM, user.groups)
            .id(tokenId)
            .build()

        return jwtService.encodeJwt(claims, tokenType.cookieName).map { jwt ->
            AccessToken(user.id, sessionId, tokenId, user.roles, user.groups, jwt)
        }
    }

    /**
     * Extract an [AccessToken] from a [ServerWebExchange].
     *
     * @param exchange The [ServerWebExchange] that should contain an [AccessToken].
     * @throws [TokenMissingException] If no access token was found in the [ServerWebExchange].
     */
    suspend fun extract(exchange: ServerWebExchange): Result<AccessToken, AccessTokenException> {

        return tokenValueExtractor.extractValue(exchange, tokenType, true)
            .mapBoth(
                success = { tokenValue -> extract(tokenValue) },
                failure = { ex ->
                    when (ex) {
                        is TokenException.Missing -> Err(AccessTokenException.Missing(ex))
                    }
                }
            )
    }

    suspend fun extractOrOAuth2FlowException(exchange: ServerWebExchange): AccessToken {

        return runCatching { extractOrNull(exchange) }
            .getOrElse { exception ->
                when (exception) {
                    is TokenExpiredException -> throw OAuth2FlowException(
                        OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED,
                        "Failed to connect a new provider to the current user. AccessToken expired.",
                        exception
                    )

                    else -> throw OAuth2FlowException(
                        OAuth2ErrorCode.INVALID_ACCESS_TOKEN,
                        "Failed to connect a new provider to the current user: AccessToken is invalid",
                        exception
                    )
                }
            } ?: throw OAuth2FlowException(
            OAuth2ErrorCode.ACCESS_TOKEN_MISSING,
            "Failed to connect a new provider to the current user: AccessToken is invalid"
        )
    }

    suspend fun extract(tokenValue: String): Result<AccessToken, AccessTokenException> {
        logger.debug { "Extracting and validating access token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .mapError { exception ->
                when (exception) {
                    is TokenException.Expired -> AccessTokenException.Expired(exception)
                    is TokenException.Invalid -> AccessTokenException.Invalid(
                        exception.message ?: "Failed to decode access token", exception
                    )

                    is TokenException.Missing -> AccessTokenException.Missing(exception)
                }
            }
            .andThen { jwt ->
                val userId = jwt.subject.toResultOr { AccessTokenException.Invalid("AccessToken does not contain sub") }
                    .andThen { sub ->
                        catchAs({ ObjectId(sub) }) { ex ->
                            AccessTokenException.Invalid("Invalid ObjectId in sub: $sub", ex)
                        }
                    }

                val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                    .toResultOr { AccessTokenException.Invalid("AccessToken does not contain session id") }
                    .andThen { s ->
                        catchAs({ UUID.fromString(s) }) { ex ->
                            AccessTokenException.Invalid(
                                "Invalid session id: $s",
                                ex
                            )
                        }
                    }

                val tokenId = jwt.id
                    .toResultOr { AccessTokenException.Invalid("AccessToken does not contain token id") }

                val roles = (jwt.claims[Constants.JWT_ROLES_CLAIM] as? List<*>)
                    .toResultOr { AccessTokenException.Invalid("AccessToken does not contain roles") }
                    .andThen { list ->
                        val parsed = mutableSetOf<Role>()
                        for (raw in list) {
                            val s = raw as? String
                                ?: return@andThen Err(AccessTokenException.Invalid("Cannot decode role $raw"))
                            val r = Role.fromString(s)
                                ?: return@andThen Err(AccessTokenException.Invalid("Unknown role $s"))
                            parsed += r
                        }
                        Ok(parsed.toSet())
                    }

                val groups = (jwt.claims[Constants.JWT_GROUPS_CLAIM] as? List<*>)
                    .toResultOr { AccessTokenException.Invalid("AccessToken does not contain groups") }
                    .andThen { list ->
                        val parsed = mutableSetOf<String>()
                        for (raw in list) {
                            val g = raw as? String
                                ?: return@andThen Err(AccessTokenException.Invalid("Cannot decode group $raw"))
                            parsed += g
                        }
                        Ok(parsed.toSet())
                    }

                userId.andThen { uid ->
                    sessionId.andThen { sid ->
                        tokenId.andThen { tid ->
                            roles.andThen { rs ->
                                groups.andThen { gs ->
                                    if (!accessTokenCache.isTokenIdValid(uid, sid, tid)) {
                                        Err(AccessTokenException.Invalid("AccessToken is not valid"))
                                    } else {
                                        Ok(AccessToken(uid, sid, tid, rs, gs, jwt))
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
