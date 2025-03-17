package io.stereov.web.user.model

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
    val roles: List<Role> = listOf(Role.USER),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: List<DeviceInfo> = listOf(),
    val lastActive: Instant = Instant.now(),
    val app: ApplicationInfo? = null,
) {

    /**
     * Return the [id] and throw a [UserException] if the [id] is null.
     *
     * @throws UserException If [id] is null
     */
    @get:Transient
    val idX: String
        get() = this.id ?: throw UserException("No ID found in UserDocument")

    fun toDto(): UserDto {
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

    fun disableTwoFactorAuth() {
        this.security.twoFactor.enabled = false
        this.security.twoFactor.secret = null
        this.security.twoFactor.recoveryCode = null
    }
}
