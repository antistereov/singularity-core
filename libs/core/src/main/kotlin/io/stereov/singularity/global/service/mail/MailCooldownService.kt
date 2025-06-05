package io.stereov.singularity.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.properties.MailProperties
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * # Service for managing email verification cooldowns.
 *
 * This service provides methods to manage the cooldown period for email verification and password resets.
 * It uses Redis to store the cooldown information.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class MailCooldownService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val mailProperties: MailProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Gets the remaining cooldown time for email verification.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another verification email.
     *
     * @param userId The ID of the user to check the cooldown for.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingVerificationCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for email verification" }

        val key = "email-verification-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    /**
     * Starts the cooldown period for email verification.
     *
     * This method sets a key in Redis to indicate that the cooldown period has started.
     *
     * @param userId The ID of the user to start the cooldown for.
     *
     * @return True if the cooldown was successfully started, false if it was already in progress.
     */
    suspend fun startVerificationCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for email verification" }

        val key = "email-verification-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.verificationSendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    /**
     * Starts the cooldown period for password reset.
     *
     * This method sets a key in Redis to indicate that the cooldown period has started.
     *
     * @param userId The ID of the user to start the cooldown for.
     *
     * @return True if the cooldown was successfully started, false if it was already in progress.
     */
    suspend fun startPasswordResetCooldown(userId: ObjectId): Boolean {
        logger.debug { "Starting cooldown for password reset" }

        val key = "password-reset-cooldown:$userId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.passwordResetSendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }

    /**
     * Gets the remaining cooldown time for password reset.
     *
     * This method checks the Redis cache for the remaining time to wait before sending another password reset email.
     *
     * @param userId The ID of the user to check the cooldown for.
     *
     * @return The remaining cooldown time in seconds.
     */
    suspend fun getRemainingPasswordResetCooldown(userId: ObjectId): Long {
        logger.debug { "Getting remaining cooldown for password resets" }

        val key = "password-reset-cooldown:$userId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }
}
