package io.stereov.web.user.model

import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.exception.UserException
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class UserDocument(
    @Id val id: String? = null,
    val name: String,
    @Indexed(unique = true) val email: String,
    val password: String,
    val roles: List<Role> = listOf(Role.USER),
    val emailVerified: Boolean = false,
    val verificationUuid: String,
    val devices: List<DeviceInfo> = listOf(),
    val lastActive: Instant = Instant.now(),
) {

    fun toDto(): UserDto {
        this.id ?: throw UserException("No ID provided in document")

        return UserDto(id, name, email, roles, emailVerified, devices, lastActive)
    }
}
