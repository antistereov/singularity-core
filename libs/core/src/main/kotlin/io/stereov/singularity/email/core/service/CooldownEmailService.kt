package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.properties.EmailProperties

/**
 * A service providing email cooldown functionality to restrict repetitive email actions within a certain time period.
 *
 * The service leverages a caching mechanism to track and manage cooldown periods for individual email addresses.
 *
 * @property cacheService An instance of the caching service used to store and manage cooldown-related data.
 * @property slug A unique prefix or identifier used to create cache keys for this service.
 * @property logger The logging utility used for providing debugging and informational log messages.
 */
interface CooldownEmailService {

    val cacheService: CacheService
    val emailProperties: EmailProperties

    val slug: String
    val logger: KLogger
    private fun getCooldownCacheKey(email: String) = "$slug:$email"


    suspend fun isCooldownActive(email: String): Result<Boolean, CacheException.Operation> {
        logger.debug { "Getting remaining cooldown" }

        return cacheService.isCooldownActive(getCooldownCacheKey(email))
    }

    suspend fun startCooldown(email: String): Result<Boolean, CacheException.Operation> {
        logger.debug { "Starting cooldown" }

        return cacheService.startCooldown(getCooldownCacheKey(email), emailProperties.sendCooldown)
    }
}