package io.stereov.singularity.auth.core.cache

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.cache.service.CacheService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for managing access tokens in a cache. This includes operations such as
 * storing, validating, and invalidating tokens associated with specific users
 * and sessions.
 *
 * This service relies on an underlying [CacheService] to perform cache operations
 * and uses configurations from [JwtProperties] for token expiration settings.
 */
@Service
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class AccessTokenCache(
    private val cacheService: CacheService,
    jwtProperties: JwtProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val activeTokenKey = "active_tokes"
    private val expiresIn = jwtProperties.expiresIn

    /**
     * Associates a specific token ID with a user and session in the cache, allowing it to be treated as valid
     * for subsequent operations.
     *
     * @param userId The unique identifier of the user.
     * @param sessionId The unique identifier of the session the token is associated with.
     * @param tokenId The token ID to be stored and allowed for the user and session.
     * @return A [Result] which, if successful, contains the stored token ID as a string.
     *   If the operation fails, a [CacheException] is returned.
     */
    suspend fun allowTokenId(userId: ObjectId, sessionId: UUID, tokenId: String): Result<String, CacheException> {
        logger.trace { "Adding token ID for user $userId" }

        val key = "$activeTokenKey:${userId.toHexString()}:${sessionId}:$tokenId"
        return  cacheService.put(key, tokenId, expiresIn)
    }

    /**
     * Verifies if a specific token ID is valid for a given user and session.
     *
     * @param userId The unique identifier of the user.
     * @param sessionId The unique identifier of the session the token is associated with.
     * @param tokenId The token ID to be validated.
     * @return A [Result] containing a Boolean that indicates whether the token ID is valid.
     *   If the operation fails, a [CacheException] is returned.
     */
    suspend fun isTokenIdValid(userId: ObjectId, sessionId: UUID, tokenId: String): Result<Boolean, CacheException> {
        logger.trace { "Checking validity of token for user $userId" }

        val key = "$activeTokenKey:${userId.toHexString()}:${sessionId}:$tokenId"
        return  cacheService.exists(key)
    }

    /**
     * Invalidates a specific token for the given user and session by removing it from the cache.
     *
     * @param principalId The unique identifier of the user.
     * @param sessionId The unique identifier of the session associated with the token.
     * @param tokenId The unique identifier of the token to be invalidated.
     * @return A [Result] containing a Boolean that indicates whether the token was successfully invalidated
     * (`true` if the token was removed, `false` if it did not exist). If the operation fails, a [CacheException] is returned.
     */
    suspend fun invalidateToken(principalId: ObjectId, sessionId: UUID, tokenId: String): Result<Boolean, CacheException> {
        logger.trace { "Removing token for user $principalId" }

        val key = "$activeTokenKey:${principalId.toHexString()}:${sessionId}:$tokenId"
        return cacheService.delete(key).map { it == 1L }
    }

    /**
     * Invalidates all session tokens associated with a specific user and session.
     *
     * @param userId The unique identifier of the user.
     * @param sessionId The unique identifier of the session for which tokens need to be invalidated.
     * @return A [Result] that, if successful, indicates that all tokens were invalidated. If the operation fails, a [CacheException] is returned.
     */
    suspend fun invalidateSessionTokens(userId: ObjectId, sessionId: UUID): Result<Unit, CacheException> {
        logger.debug { "Invalidating all tokens for user $userId and session $sessionId" }

        return cacheService.deleteAll("$activeTokenKey:$userId:${sessionId}:*")
    }

    /**
     * Invalidates all active tokens associated with a specific user by removing them from the cache.
     *
     * @param principalId The unique identifier of the user whose tokens need to be invalidated.
     * @return A [Result] that, if successful, indicates that all tokens were invalidated.
     * If the operation fails, a [CacheException] is returned.
     */
    suspend fun invalidateAllTokens(principalId: ObjectId): Result<Unit, CacheException> {
        logger.trace { "Invalidating all tokens for user $principalId" }

        return cacheService.deleteAll("$activeTokenKey:$principalId:*")
    }
}
