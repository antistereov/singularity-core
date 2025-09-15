package io.stereov.singularity.admin.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.admin.core.dto.RotationStatusResponse
import io.stereov.singularity.auth.twofactor.properties.TwoFactorMailCodeProperties
import io.stereov.singularity.database.core.service.SensitiveCrudService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.service.SecretService
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
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
class AdminService(
    private val context: ApplicationContext,
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val hashService: HashService,
    private val twoFactorMailCodeProperties: TwoFactorMailCodeProperties
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val rotationOngoing = AtomicBoolean(false)

    private val keyRotationScope = CoroutineScope(Dispatchers.Default)

    @PostConstruct
    fun init() {
        this.getSecretServices().forEach { runBlocking { it.getCurrentSecret() } }
        runBlocking { initRootAccount() }
    }

    private suspend fun initRootAccount() {
        if (!this.userService.existsByEmail(appProperties.rootEmail) && appProperties.createRootUser) {
            this.logger.info { "Creating root account" }

            val rootUser = UserDocument.ofPassword(
                email = appProperties.rootEmail,
                password = hashService.hashBcrypt(appProperties.rootPassword),
                name = "Root",
                mailEnabled = appProperties.enableMail,
                mailTwoFactorCodeExpiresIn = twoFactorMailCodeProperties.expiresIn
            ).addRole(Role.ADMIN)

            this.userService.save(rootUser)
        } else {
            this.logger.info { "Root account exists, skipping creation" }
        }
    }

    private fun getSecretServices(): List<SecretService> {
        return this.context.getBeansOfType(SecretService::class.java).values.toList()
    }


    @Scheduled(cron = "\${singularity.secrets.key-rotation-cron:0 0 4 1 1,4,7,10 *}")
    suspend fun rotateKeys() {
        this.logger.info { "Rotating keys" }

        if (this.rotationOngoing.get()) {
            this.logger.warn { "Rotation is currently ongoing. Stopping new attempt" }
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
