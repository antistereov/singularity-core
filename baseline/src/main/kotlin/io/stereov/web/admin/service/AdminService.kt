package io.stereov.web.admin.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.database.service.SensitiveCrudService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.properties.AppProperties
import io.stereov.web.user.model.Role
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service
class AdminService(
    private val keyManager: KeyManager,
    private val context: ApplicationContext,
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val hashService: HashService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val rotationOngoing = AtomicBoolean(false)

    private val keyRotationScope = CoroutineScope(Dispatchers.Default)

    @PostConstruct
    fun init() {
        runBlocking { initRootAccount() }
    }

    private suspend fun initRootAccount() {
        if (!this.userService.existsByEmail(appProperties.rootEmail) && appProperties.createRootUser) {
            this.logger.info { "Creating root account" }

            val rootUser = UserDocument(
                email = appProperties.rootEmail,
                password = hashService.hashBcrypt(appProperties.rootPassword),
                name = "Root"
            ).addRole(Role.ADMIN)

            this.userService.save(rootUser)
        } else {
            this.logger.info { "Root account exists, skipping creation" }
        }
    }


    @Scheduled(cron = "\${baseline.secrets.key-rotation-cron}")
    suspend fun rotateKeys() {
        this.logger.info { "Rotating keys" }

        if (this.rotationOngoing()) {
            this.logger.warn { "Rotation is currently ongoing. Stopping new attempt" }
            return
        }

        this.rotationOngoing.set(true)

        this.keyRotationScope.launch {
            logger.info { "Rotating JWT secret" }
            keyManager.updateJwtSecret()
            keyManager.updateEncryptionSecret()

            logger.info { "Rotating encryption secrets" }
            context.getBeansOfType(SensitiveCrudService::class.java).forEach { (name, service) ->
                logger.info { "Rotating keys for documents defined in $name" }
                service.rotateKey()
            }

            rotationOngoing.set(false)
            logger.info { "Rotation finished successfully" }
        }
    }

    suspend fun rotationOngoing(): Boolean {
        this.logger.debug { "Checking rotation status" }

        return this.rotationOngoing.get()
    }
}
