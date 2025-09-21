package io.stereov.singularity.secrets.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.admin.core.dto.RotationStatusResponse
import io.stereov.singularity.database.core.service.SensitiveCrudService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SecretRotationService(
    private val context: ApplicationContext,
) {

    private val logger = KotlinLogging.logger {}

    private val rotationOngoing = AtomicBoolean(false)

    private val keyRotationScope = CoroutineScope(Dispatchers.Default)

    @PostConstruct
    fun init() {
        this.getSecretServices().forEach { runBlocking { it.getCurrentSecret() } }
    }

    private fun getSecretServices(): List<SecretService> {
        return this.context.getBeansOfType(SecretService::class.java).values.toList()
    }

    @Scheduled(cron = "\${singularity.secrets.key-rotation-cron:0 0 4 1 1,4,7,10 *}")
    suspend fun rotateKeys() {
        this.logger.info { "Rotating keys" }

        if (this.rotationOngoing.get()) {
            this.logger.warn { "Rotation is currently ongoing. Stopping new attempt." }
            return
        }

        this.rotationOngoing.set(true)

        this.keyRotationScope.launch {
            logger.info { "Rotating secrets" }
            context.getBeansOfType(SecretService::class.java).forEach { (name, service) ->
                logger.info { "Rotating keys for secrets defined in $name}" }
                service.rotateSecret()
            }

            logger.info { "Rotating encryption secrets" }
            context.getBeansOfType(SensitiveCrudService::class.java).forEach { (name, service) ->
                logger.info { "Rotating keys for documents defined in $name" }
                service.rotateSecret()
            }

            rotationOngoing.set(false)
            logger.info { "Rotation finished successfully" }
        }
    }

    suspend fun getRotationStatus(): RotationStatusResponse {
        this.logger.debug { "Checking rotation status" }

        return RotationStatusResponse(
            this.rotationOngoing.get(),
            this.getLastRotation()
        )
    }

    suspend fun getLastRotation(): Instant? {
        this.logger.debug { "Getting last rotation" }

        return context.getBeansOfType(SecretService::class.java)
            .map { (_, service) -> service.getLastUpdate() }
            .sortedBy { it?.toEpochMilli() }
            .firstOrNull()
    }
}