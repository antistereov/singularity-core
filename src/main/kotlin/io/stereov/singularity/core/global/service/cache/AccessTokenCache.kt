package io.stereov.singularity.core.global.service.cache

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.core.properties.JwtProperties
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

/**
 * # AccessTokenCache
 *
 * This class is responsible for managing access tokens in Redis.
 * It provides methods to add, check, remove, and invalidate access tokens.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class AccessTokenCache(
    private val commands: RedisCoroutinesCommands<String, ByteArray>,
    jwtProperties: JwtProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val activeTokenKey = "active_tokes"
    private val expiresIn = jwtProperties.expiresIn

    /**
     * Adds a token ID to the cache for a specific user.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be added.
     */
    suspend fun addTokenId(userId: String, tokenId: String) {
        logger.debug { "Adding token ID for user $userId" }

        val key = "$activeTokenKey:$userId:$tokenId"
        commands.sadd(key, tokenId.toByteArray())
        commands.expire(key, expiresIn)
    }

    /**
     * Checks if a token ID is valid for a specific user.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be checked.
     * @return True if the token ID is valid, false otherwise.
     */
    suspend fun isTokenIdValid(userId: String, tokenId: String): Boolean {
        logger.debug { "Checking validity of token for user $userId" }

        val key = "$activeTokenKey:$userId:$tokenId"
        return commands.exists(key) == 1L
    }

    /**
     * Checks if a token ID is valid for a specific user.
     *
     * @param userId The ID of the user.
     * @param tokenId The token ID to be checked.
     * @return True if the token ID is valid, false otherwise.
     */
    suspend fun removeTokenId(userId: String, tokenId: String): Boolean {
        logger.debug { "Removing token for user $userId" }

        val key = "$activeTokenKey:$userId:$tokenId"
        return commands.del(key) == 1L
    }

    /**
     * Invalidates all tokens for a specific user.
     *
     * @param userId The ID of the user.
     */
    suspend fun invalidateAllTokens(userId: String) {
        logger.debug { "Invalidating all tokens for user $userId" }

        val keys = commands.keys("$activeTokenKey:$userId:*")

        keys.map { key ->
            commands.del(key)
        }
    }
}
