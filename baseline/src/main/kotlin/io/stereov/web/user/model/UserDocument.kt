package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.exception.UserException
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class UserDocument(
    @Id val id: String? = null,
    @Indexed(unique = true) val email: String,
    val name: String? = null,
    val password: String,
    val roles: MutableList<Role> = mutableListOf(Role.USER),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: MutableList<DeviceInfo> = mutableListOf(),
    var lastActive: Instant = Instant.now(),
    val app: ApplicationInfo? = null,
) {

    @get:Transient
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Return the [id] and throw a [UserException] if the [id] is null.
     *
     * @throws UserException If [id] is null
     */
    @get:Transient
    val idX: String
        get() = this.id ?: throw UserException("No ID found in UserDocument")

    fun toDto(): UserDto {
        logger.debug { "Creating UserDto" }

        return UserDto(
            idX,
            name,
            email,
            roles,
            security.mail.verified,
            devices.map { it.toResponseDto() },
            lastActive.toString(),
            security.twoFactor.enabled,
            app?.toDto()
        )
    }

    fun setupTwoFactorAuth(secret: String, recoveryCode: String): UserDocument {
        logger.debug { "Setting up two factor authentication" }

        this.security.twoFactor.enabled = true
        this.security.twoFactor.secret = secret
        this.security.twoFactor.recoveryCode = recoveryCode

        return this
    }

    fun disableTwoFactorAuth() {
        logger.debug { "Disabling two factor authentication" }

        this.security.twoFactor.enabled = false
        this.security.twoFactor.secret = null
        this.security.twoFactor.recoveryCode = null
    }

    fun addOrUpdateDevice(deviceInfo: DeviceInfo): UserDocument {
        logger.debug { "Adding or updating device ${deviceInfo.id}" }

        removeDevice(deviceInfo.id)
        this.devices.add(deviceInfo)

        return this
    }

    fun removeDevice(deviceId: String): UserDocument {
        logger.debug { "Removing device $deviceId" }

        this.devices.removeAll { device -> device.id == deviceId }

        return this
    }

    fun clearDevices() {
        logger.debug { "Clearing devices" }

        this.devices.clear()
    }

    fun addRole(role: Role): List<Role> {
        this.roles.add(role)
        return this.roles
    }

    fun removeRole(role: Role): Boolean {
        return this.roles.remove(role)
    }

    fun updateLastActive(): UserDocument {
        logger.debug { "Updating last active" }

        this.lastActive = Instant.now()
        return this
    }
}
