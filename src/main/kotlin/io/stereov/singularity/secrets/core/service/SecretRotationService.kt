package io.stereov.singularity.secrets.core.service

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.secrets.core.dto.RotationStatusResponse
import io.stereov.singularity.secrets.core.exception.SecretRotationException
import io.stereov.singularity.secrets.core.model.RotationInformation
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service responsible for managing and executing key rotation processes for secrets and sensitive data.
 *
 * This service handles the periodic rotation of encryption keys and other secrets used by the application,
 * ensuring that cryptographic materials are updated securely and in compliance with scheduled configurations.
 */
@Service
class SecretRotationService(
    private val context: ApplicationContext,
) {

    private val logger = KotlinLogging.logger {}
    private val rotationOngoing = AtomicBoolean(false)
    private val keyRotationScope = CoroutineScope(Dispatchers.Default)
    private val rotationInfos = mutableMapOf<String, RotationInformation>()

    /**
     * Checks if a key rotation process is currently ongoing.
     *
     * This method provides the current state of the key rotation process by returning
     * the value of an internal atomic flag. It indicates whether a key rotation
     * operation is in progress or not.
     *
     * @return A boolean value: `true` if a rotation is in progress, `false` otherwise.
     */
    fun rotationOngoing() = rotationOngoing.get()

    @PostConstruct
    fun init() {
        this.getSecretServices().forEach { runBlocking { it.getCurrentSecret() } }
    }

    private fun getSecretServices(): List<SecretService> {
        return this.context.getBeansOfType(SecretService::class.java).values.toList()
    }

    /**
     * Initiates the rotation of keys across all registered secret and sensitive CRUD services.
     *
     * This method performs the following steps:
     * - Checks if a rotation process is already ongoing and returns an error if true.
     * - Clears previous rotation information and sets the rotation process as ongoing.
     * - Launches a coroutine to iterate through all services implementing the [SecretService]
     *   and [SensitiveCrudService] interfaces, invoking their respective `rotateSecret` methods.
     * - Captures the success or failure status for each key rotation operation, including the
     *   last successful rotation timestamp and any exceptions encountered during the process.
     * - Updates the rotation status upon completion of all operations.
     *
     * @return A [Result] instance containing a `Map` of service names to their respective
     * rotation information if the operation is successful, or a [SecretRotationException]
     * in case of an error.
     */
    @Scheduled(cron = "\${singularity.secrets.key-rotation-cron:0 0 4 1 1,4,7,10 *}")
    suspend fun rotateKeys(): Result<Map<String, RotationInformation>, SecretRotationException> {
        logger.info { "Rotating keys" }

        if (rotationOngoing.get()) {
            return Err(SecretRotationException.Ongoing("Rotation is currently ongoing"))
        }

        rotationOngoing.set(true)
        rotationInfos.clear()

        this.keyRotationScope.launch {
            logger.info { "Rotating secrets" }
            context.getBeansOfType(SecretService::class.java).forEach { (name, service) ->
                logger.info { "Rotating keys for secrets defined in $name}" }
                service.rotateSecret()
                    .onSuccess {
                        rotationInfos[name] = RotationInformation(lastRotation = service.getLastUpdate(), success = true)
                    }
                    .onFailure { ex ->
                        rotationInfos[name] = RotationInformation(lastRotation = service.getLastUpdate(), success = false, error = ex)
                        logger.error(ex) { "Failed to rotate secret for $name: ${ex.message}" }
                    }
            }

            logger.info { "Rotating encryption secrets" }
            context.getBeansOfType(SensitiveCrudService::class.java).forEach { (name, service) ->
                logger.info { "Rotating keys for documents defined in $name" }
                service.rotateSecret()
                    .onSuccess {
                        rotationInfos[name] = RotationInformation(lastRotation = service.getLastSuccessfulKeyRotation(), success = true)
                    }
                    .onFailure { ex ->
                        rotationInfos[name] = RotationInformation(lastRotation = service.getLastSuccessfulKeyRotation(), success = false, error = ex)
                        logger.error(ex) { "Failed to rotate secret for $name: ${ex.message}" }
                    }
            }

            rotationOngoing.set(false)
            logger.info { "Rotation finished successfully" }
        }

        return Ok(rotationInfos)
    }

    /**
     * Retrieves the current status of the key rotation process.
     *
     * This method provides information about whether a key rotation process is currently
     * ongoing and returns the state of the rotation for each service involved.
     *
     * @return A [RotationStatusResponse] containing the ongoing status of key rotation
     * and a map of service names to their respective rotation information.
     */
    suspend fun getRotationStatus(): RotationStatusResponse {
        this.logger.debug { "Checking rotation status" }

        return RotationStatusResponse(
            this.rotationOngoing.get(),
            rotationInfos
        )
    }
}