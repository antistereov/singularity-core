package io.stereov.web.user.service.device

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.user.model.DeviceInfo
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import org.springframework.stereotype.Service

@Service
class UserDeviceService(
    private val userService: UserService,
    private val authenticationService: AuthenticationService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun getDevices(): List<DeviceInfo> {
        logger.debug { "Getting devices" }

        return authenticationService.getCurrentUser().devices
    }

    suspend fun removeDevice(deviceId: String): UserDocument {
        logger.debug { "Removing device $deviceId" }

        val user = authenticationService.getCurrentUser()

        user.removeDevice(deviceId)

        return userService.save(user)
    }

    suspend fun clearDevices(): UserDocument {
        logger.debug { "Clearing devices" }

        val user = authenticationService.getCurrentUser()

        user.clearDevices()

        return userService.save(user)
    }
}
