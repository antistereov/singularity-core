package io.stereov.singularity.core.user.service.device

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.user.model.DeviceInfo
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.service.UserService
import org.springframework.stereotype.Service

/**
 * # Service for managing user devices.
 *
 * This service provides methods to get, remove, and clear user devices.
 * It interacts with the [UserService] to manage user data
 * and the [AuthenticationService] to get the current user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserDeviceService(
    private val userService: UserService,
    private val authenticationService: AuthenticationService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Gets the list of devices associated with the current user.
     *
     * @return A list of [DeviceInfo] objects representing the user's devices.
     */
    suspend fun getDevices(): List<DeviceInfo> {
        logger.debug { "Getting devices" }

        return authenticationService.getCurrentUser().sensitive.devices
    }

    /**
     * Removes a device from the current user's account.
     *
     * @param deviceId The ID of the device to remove.
     *
     * @return The updated [UserDocument] of the user.
     */
    suspend fun removeDevice(deviceId: String): UserDocument {
        logger.debug { "Removing device $deviceId" }

        val user = authenticationService.getCurrentUser()

        user.removeDevice(deviceId)

        return userService.save(user)
    }

    /**
     * Clears all devices from the current user's account.
     *
     * @return The updated [UserDocument] of the user.
     */
    suspend fun clearDevices(): UserDocument {
        logger.debug { "Clearing devices" }

        val user = authenticationService.getCurrentUser()

        user.clearDevices()

        return userService.save(user)
    }
}
