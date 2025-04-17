package io.stereov.web.global.service.secrets.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.service.UserService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KeyRotationService(
    private val userService: UserService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @Scheduled
    suspend fun rotateKeys() {
        this.logger.info { "Rotating encryption keys" }

        this.userService.rotateKeys()
    }


}
