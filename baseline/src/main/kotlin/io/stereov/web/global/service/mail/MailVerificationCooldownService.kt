package io.stereov.web.global.service.mail

import io.stereov.web.properties.MailProperties
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable-verification"], havingValue = "true", matchIfMissing = false)
class MailVerificationCooldownService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val mailProperties: MailProperties,
) {

    suspend fun getRemainingEmailVerificationCooldown(accountId: String): Long {
        val key = "email-cooldown:$accountId"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return if (remainingTtl.seconds > 0) remainingTtl.seconds else 0
    }

    suspend fun startEmailVerificationCooldown(accountId: String): Boolean {
        val key = "email-cooldown:$accountId"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(mailProperties.verificationSendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }
}
