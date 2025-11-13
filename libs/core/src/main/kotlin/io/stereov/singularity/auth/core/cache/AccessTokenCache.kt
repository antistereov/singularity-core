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
 * This class is responsible for managing access tokens in Redis.
 * It provides methods to add, check, remove, and invalidate access tokens.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
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
     * Adds a token ID to the cache for a specific user to the allowlist.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be added.
     */
    suspend fun allowTokenId(userId: ObjectId, sessionId: UUID, tokenId: String): Result<String, CacheException> {
        logger.trace { "Adding token ID for user $userId" }

        val key = "$activeTokenKey:${userId.toHexString()}:${sessionId}:$tokenId"
        return  cacheService.put(key, tokenId, expiresIn)
    }

    /**
     * Checks if a token ID is valid for a specific user.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be checked.
     * @return True if the token ID is valid, false otherwise.
     */
    suspend fun isTokenIdValid(userId: ObjectId, sessionId: UUID, tokenId: String): Result<Boolean, CacheException> {
        logger.trace { "Checking validity of token for user $userId" }

        val key = "$activeTokenKey:${userId.toHexString()}:${sessionId}:$tokenId"
        return  cacheService.exists(key)
    }

    /**
     * Checks if a token ID is valid for a specific user.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be checked.
     * @return True if the token ID is valid, false otherwise.
     */
    suspend fun invalidateToken(userId: ObjectId, sessionId: UUID, tokenId: String): Result<Boolean, CacheException> {
        logger.trace { "Removing token for user $userId" }

        val key = "$activeTokenKey:${userId.toHexString()}:${sessionId}:$tokenId"
        return cacheService.delete(key).map { it == 1L }
    }

    suspend fun invalidateSessionTokens(userId: ObjectId, sessionId: UUID): Result<Unit, CacheException> {
        logger.debug { "Invalidating all tokens for user $userId and session $sessionId" }

        return cacheService.deleteAll("$activeTokenKey:$userId:${sessionId}:*")
    }

    /**
     * Invalidates all tokens for a specific user.
     *
     * @param userId The ID of the user.
     */
    suspend fun invalidateAllTokens(userId: ObjectId): Result<Unit, CacheException> {
        logger.trace { "Invalidating all tokens for user $userId" }

        return cacheService.deleteAll("$activeTokenKey:$userId:*")
    }
}
