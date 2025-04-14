package io.stereov.web.user.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.file.model.FileMetaData
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.exception.model.InvalidUserDocumentException
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * # UserDocument
 *
 * This class represents a user document in the database.
 * It contains fields for user information, security details, and device information.
 * It also provides methods for managing two-factor authentication, devices, and roles.
 *
 * @property _id The unique identifier for the user document.
 * @property email The email address of the user.
 * @property name The name of the user.
 * @property password The hashed password of the user.
 * @property roles The roles assigned to the user.
 * @property security The security details of the user.
 * @property devices The list of devices associated with the user.
 * @property lastActive The last active timestamp of the user.
 * @property app The application information associated with the user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Document(collection = "users")
data class UserDocument(
    @Id val _id: String? = null,
    @Indexed(unique = true) var email: String,
    var name: String? = null,
    var password: String,
    val roles: MutableList<Role> = mutableListOf(Role.USER),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: MutableList<DeviceInfo> = mutableListOf(),
    var lastActive: Instant = Instant.now(),
    var app: ApplicationInfo? = null,
    var avatar: FileMetaData? = null,
) {

    @get:Transient
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Return the [_id] and throw a [InvalidUserDocumentException] if the [_id] is null.
     *
     * @throws InvalidUserDocumentException If [_id] is null
     */
    @get:Transient
    val id: String
        get() = this._id ?: throw InvalidUserDocumentException("No ID found in UserDocument")

    /**
     * Returns the path where user-specific information is stored.
     *
     * @throws InvalidUserDocumentException If [_id] is null.
     */
    @get:Transient
    val fileStoragePath: String
        get() = "users/$id"

    /**
     * Get the application info of the user.
     *
     * This method returns the application info associated with the user cast to the given class [T].
     *
     * @throws InvalidUserDocumentException If the application info is not of the expected type.
     */
    inline fun <reified T: ApplicationInfo> getApplicationInfo(): T {
        return app as? T ?: throw InvalidUserDocumentException("No application info found in UserDocument")
    }

    /**
     * Convert this [UserDocument] to a [UserDto].
     *
     * This method is used to create a data transfer object (DTO) for the user.
     *
     * @return A [UserDto] containing the user information.
     */
    fun toDto(): UserDto {
        logger.debug { "Creating UserDto" }

        return UserDto(
            id,
            name,
            email,
            roles,
            security.mail.verified,
            devices.map { it.toResponseDto() },
            lastActive.toString(),
            security.twoFactor.enabled,
            app?.toDto(),
            avatar
        )
    }

    /**
     * Set up two-factor authentication for the user.
     *
     * This method enables two-factor authentication and sets the secret and recovery code.
     *
     * @param secret The secret key for the user.
     * @param recoveryCodes The recovery codes for the user.
     *
     * @return The updated [UserDocument].
     */
    fun setupTwoFactorAuth(secret: String, recoveryCodes: List<String>): UserDocument {
        logger.debug { "Setting up two factor authentication" }

        this.security.twoFactor.enabled = true
        this.security.twoFactor.secret = secret
        this.security.twoFactor.recoveryCodes = recoveryCodes.toMutableList()

        return this
    }

    /**
     * Disable two-factor authentication for the user.
     *
     * This method disables two-factor authentication and clears the secret and recovery code.
     *
     * @return The updated [UserDocument].
     */
    fun disableTwoFactorAuth(): UserDocument {
        logger.debug { "Disabling two factor authentication" }

        this.security.twoFactor.enabled = false
        this.security.twoFactor.secret = null
        this.security.twoFactor.recoveryCodes = mutableListOf()

        return this
    }

    /**
     * Add or update a device for the user.
     *
     * This method adds a new device or updates an existing device in the user's device list.
     *
     * @param deviceInfo The device information to add or update.
     *
     * @return The updated [UserDocument].
     */
    fun addOrUpdateDevice(deviceInfo: DeviceInfo): UserDocument {
        logger.debug { "Adding or updating device ${deviceInfo.id}" }

        removeDevice(deviceInfo.id)
        this.devices.add(deviceInfo)

        return this
    }

    /**
     * Remove a device from the user's device list.
     *
     * This method removes a device from the user's device list based on the device ID.
     *
     * @param deviceId The ID of the device to remove.
     *
     * @return The updated [UserDocument].
     */
    fun removeDevice(deviceId: String): UserDocument {
        logger.debug { "Removing device $deviceId" }

        this.devices.removeAll { device -> device.id == deviceId }

        return this
    }

    /**
     * Clear all devices from the user's device list.
     *
     * This method removes all devices from the user's device list.
     */
    fun clearDevices() {
        logger.debug { "Clearing devices" }

        this.devices.clear()
    }

    /**
     * Add a role to the user.
     *
     * This method adds a new role to the user's role list.
     *
     * @param role The role to add.
     *
     * @return The updated list of roles.
     */
    fun addRole(role: Role): List<Role> {
        this.roles.add(role)
        return this.roles
    }

    /**
     * Remove a role from the user.
     *
     * This method removes a role from the user's role list.
     *
     * @param role The role to remove.
     *
     * @return True if the role was removed, false otherwise.
     */
    fun removeRole(role: Role): Boolean {
        return this.roles.remove(role)
    }

    /**
     * Update the last active timestamp of the user.
     *
     * This method sets the last active timestamp to the current time.
     *
     * @return The updated [UserDocument].
     */
    fun updateLastActive(): UserDocument {
        logger.debug { "Updating last active" }

        this.lastActive = Instant.now()
        return this
    }
}
